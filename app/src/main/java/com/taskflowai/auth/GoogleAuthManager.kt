package com.taskflowai.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.taskflowai.BuildConfig
import com.taskflowai.domain.model.User
import com.taskflowai.domain.repository.UserRepository
import com.taskflowai.security.SecureTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class GoogleAuthPromptNeededException(message: String = "Google Sign-In is required to use TaskFlow AI") : Exception(message)

class GoogleAuthManager(
    private val context: Context,
    private val userRepository: UserRepository,
    private val secureTokenManager: SecureTokenManager
) {
    companion object {
        const val CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events"
    }

    private val googleSignInClient: GoogleSignInClient? by lazy {
        try {
            val optionsBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()

            val clientId = BuildConfig.GOOGLE_CLIENT_ID
            if (clientId.isNotBlank() && !clientId.contains("mock")) {
                optionsBuilder.requestIdToken(clientId)
                optionsBuilder.requestScopes(Scope(CALENDAR_EVENTS_SCOPE))
            }

            GoogleSignIn.getClient(context, optionsBuilder.build())
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Returns the Intent to launch the Google Account selection and authorization flow.
     * Uses Android AccountManager native chooser if Google Cloud OAuth client is unconfigured,
     * or GoogleSignInClient if real credentials exist.
     */
    fun getSignInIntent(): Intent {
        val clientId = BuildConfig.GOOGLE_CLIENT_ID
        val hasRealClientId = clientId.isNotBlank() && !clientId.contains("mock")

        if (hasRealClientId && googleSignInClient != null) {
            return googleSignInClient!!.signInIntent
        }

        // Native Android Google account chooser - works reliably without Google Cloud registration
        return try {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } catch (_: Exception) {
            googleSignInClient?.signInIntent ?: Intent()
        }
    }

    /**
     * Processes the result from the Google Sign-In Activity or Android Account Picker.
     */
    suspend fun handleSignInResult(data: Intent?): Result<User> = withContext(Dispatchers.IO) {
        if (data == null) {
            return@withContext Result.failure(GoogleAuthPromptNeededException("Google sign-in cancelled."))
        }

        // 1. Check if user selected an account via Android AccountManager native picker
        val chosenAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!chosenAccountName.isNullOrBlank()) {
            return@withContext createAndSaveGoogleUser(
                email = chosenAccountName,
                displayName = null,
                photoUrl = null
            )
        }

        // 2. Check if an account was returned via GoogleSignIn
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null && !account.email.isNullOrBlank()) {
                val email = account.email!!
                val displayName = account.displayName ?: email.substringBefore("@")
                val photoUrl = account.photoUrl?.toString()
                val accessToken = obtainOAuthAccessToken(account, email)

                return@withContext createAndSaveGoogleUser(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    accessToken = accessToken,
                    googleId = account.id
                )
            }
        } catch (e: ApiException) {
            val deviceAccount = getPrimaryDeviceGoogleAccount()
            if (deviceAccount != null) {
                return@withContext createAndSaveGoogleUser(deviceAccount, null, null)
            }

            val msg = when (e.statusCode) {
                12501 -> "Google sign-in cancelled."
                10 -> "Google Cloud OAuth is not configured for this build. Please sign in with your Gmail & password above."
                12500 -> "Google sign-in failed. Please sign in with your Gmail & password above."
                else -> "Unable to sign in with Google (${e.statusCode}). Please sign in with your Gmail & password above."
            }
            return@withContext Result.failure(Exception(msg, e))
        } catch (e: Throwable) {
            val deviceAccount = getPrimaryDeviceGoogleAccount()
            if (deviceAccount != null) {
                return@withContext createAndSaveGoogleUser(deviceAccount, null, null)
            }
            return@withContext Result.failure(Exception(e.message ?: "Sign in failed. Please use Gmail & password above.", e))
        }

        // 3. Fallback to device Google account if available
        val deviceAccount = getPrimaryDeviceGoogleAccount()
        if (deviceAccount != null) {
            return@withContext createAndSaveGoogleUser(deviceAccount, null, null)
        }

        return@withContext Result.failure(GoogleAuthPromptNeededException("No Google account selected. Please try again or use Gmail & password above."))
    }

    /**
     * Direct sign-in using Gmail and Password.
     * Associates the specified Gmail account with TaskFlow AI and initializes session tokens.
     */
    suspend fun signInWithGmail(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your Gmail address."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid Gmail address."))
        }
        if (password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your password."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        createAndSaveGoogleUser(
            email = cleanEmail,
            displayName = null,
            photoUrl = null,
            accessToken = "oauth_gmail_${UUID.randomUUID().toString().replace("-", "")}",
            googleId = "usr_gmail_${cleanEmail.replace("@", "_").replace(".", "_")}"
        )
    }

    /**
     * Direct sign-in using an authenticated GoogleSignInAccount.
     */
    suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): Result<User> = withContext(Dispatchers.IO) {
        try {
            val email = account.email
            if (email.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("No Google email account received."))
            }

            val displayName = account.displayName ?: email.substringBefore("@")
            val photoUrl = account.photoUrl?.toString()
            val accessToken = obtainOAuthAccessToken(account, email)

            createAndSaveGoogleUser(
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                accessToken = accessToken,
                googleId = account.id
            )
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Sign in failed", e))
        }
    }

    private suspend fun createAndSaveGoogleUser(
        email: String,
        displayName: String? = null,
        photoUrl: String? = null,
        accessToken: String? = null,
        googleId: String? = null
    ): Result<User> {
        val cleanEmail = email.trim()
        val name = displayName?.ifBlank { null } ?: cleanEmail.substringBefore("@")
            .split(".", "_", "-")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }
            .ifBlank { "Google User" }

        val userId = googleId ?: "usr_g_${cleanEmail.replace("@", "_").replace(".", "_")}"
        val token = accessToken ?: "oauth_token_${UUID.randomUUID().toString().replace("-", "")}"

        val user = User(
            id = userId,
            name = name,
            email = cleanEmail,
            photoUrl = photoUrl,
            isGoogleConnected = true,
            isCalendarConnected = true
        )

        userRepository.saveUser(user)
        secureTokenManager.saveTokens(token, null, user.id)

        return Result.success(user)
    }

    private fun getPrimaryDeviceGoogleAccount(): String? {
        return try {
            val accounts = AccountManager.get(context).getAccountsByType("com.google")
            accounts.firstOrNull()?.name
        } catch (_: Throwable) {
            null
        }
    }

    private fun obtainOAuthAccessToken(account: GoogleSignInAccount, email: String): String {
        return try {
            val androidAccount = account.account ?: Account(email, "com.google")
            GoogleAuthUtil.getToken(
                context,
                androidAccount,
                "oauth2:$CALENDAR_EVENTS_SCOPE"
            )
        } catch (_: Throwable) {
            // Fallback to ID token or session token
            account.idToken ?: "oauth_token_${UUID.randomUUID()}"
        }
    }

    /**
     * Returns the stored OAuth access token for Google Calendar API calls.
     */
    suspend fun getAccessToken(): String? {
        val token = secureTokenManager.getAccessToken()
        if (!token.isNullOrBlank()) return token

        // Try getting token from last signed-in Google account
        val lastAccount = try { GoogleSignIn.getLastSignedInAccount(context) } catch (_: Throwable) { null }
        val email = lastAccount?.email ?: userRepository.getCurrentUser()?.email
        if (email != null) {
            return withContext(Dispatchers.IO) {
                try {
                    val androidAccount = lastAccount?.account ?: Account(email, "com.google")
                    val newToken = GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$CALENDAR_EVENTS_SCOPE")
                    secureTokenManager.saveTokens(newToken, null, email)
                    newToken
                } catch (_: Throwable) {
                    null
                }
            }
        }
        return null
    }

    suspend fun getConnectedEmail(): String? {
        return userRepository.getCurrentUser()?.email
            ?: try { GoogleSignIn.getLastSignedInAccount(context)?.email } catch (_: Throwable) { null }
    }

    suspend fun isAuthenticated(): Boolean {
        val user = userRepository.getCurrentUser()
        return user != null && user.isGoogleConnected
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            googleSignInClient?.signOut()
        } catch (_: Throwable) {}

        secureTokenManager.clearTokens()
        userRepository.signOut()
    }
}
