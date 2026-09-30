package com.taskflowai

import android.accounts.AccountManager
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.taskflowai.auth.GoogleAuthManager
import com.taskflowai.domain.model.User
import com.taskflowai.domain.repository.UserRepository
import com.taskflowai.security.SecureTokenManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthTest {

    private lateinit var userRepository: UserRepository
    private lateinit var secureTokenManager: SecureTokenManager
    private lateinit var authManager: GoogleAuthManager

    @Before
    fun setUp() {
        userRepository = mockk(relaxed = true)
        secureTokenManager = mockk(relaxed = true)
        authManager = GoogleAuthManager(
            context = mockk(relaxed = true),
            userRepository = userRepository,
            secureTokenManager = secureTokenManager
        )
    }

    @Test
    fun `signInWithGoogleAccount connects user with their exact authenticated Google account`() = runBlocking {
        val googleAccount = mockk<GoogleSignInAccount>(relaxed = true)
        every { googleAccount.id } returns "google_uid_123"
        every { googleAccount.email } returns "user.actual@gmail.com"
        every { googleAccount.displayName } returns "Payal Sharma"
        every { googleAccount.photoUrl } returns null
        every { googleAccount.idToken } returns "mock_id_token"

        val userSlot = slot<User>()
        coEvery { userRepository.saveUser(capture(userSlot)) } just Runs

        val result = authManager.signInWithGoogleAccount(googleAccount)

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("user.actual@gmail.com", user?.email)
        assertEquals("Payal Sharma", user?.name)
        assertTrue(user!!.isGoogleConnected)
        assertTrue(user.isCalendarConnected)
        coVerify(exactly = 1) { userRepository.saveUser(any()) }
        coVerify(exactly = 1) { secureTokenManager.saveTokens(any(), any(), any()) }
    }

    @Test
    fun `handleSignInResult with AccountManager intent successfully logs in user`() = runBlocking {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) } returns "chosen.google@gmail.com"

        val userSlot = slot<User>()
        coEvery { userRepository.saveUser(capture(userSlot)) } just Runs

        val result = authManager.handleSignInResult(intent)

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("chosen.google@gmail.com", user?.email)
        assertEquals("Chosen Google", user?.name)
        assertTrue(user!!.isGoogleConnected)
        assertTrue(user.isCalendarConnected)
        coVerify(exactly = 1) { userRepository.saveUser(any()) }
        coVerify(exactly = 1) { secureTokenManager.saveTokens(any(), null, user.id) }
    }

    @Test
    fun `signInWithGmail successfully authenticates with valid email and password`() = runBlocking {
        val userSlot = slot<User>()
        coEvery { userRepository.saveUser(capture(userSlot)) } just Runs

        val result = authManager.signInWithGmail("rohan.sharma@gmail.com", "mypassword123")

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("rohan.sharma@gmail.com", user?.email)
        assertEquals("Rohan Sharma", user?.name)
        assertTrue(user!!.isGoogleConnected)
        assertTrue(user.isCalendarConnected)
        coVerify(exactly = 1) { userRepository.saveUser(any()) }
        coVerify(exactly = 1) { secureTokenManager.saveTokens(any(), null, user.id) }
    }

    @Test
    fun `signInWithGmail rejects blank or invalid email`() = runBlocking {
        val blankResult = authManager.signInWithGmail("", "password123")
        assertTrue(blankResult.isFailure)

        val invalidResult = authManager.signInWithGmail("invalidemail", "password123")
        assertTrue(invalidResult.isFailure)
    }

    @Test
    fun `signInWithGmail rejects short password`() = runBlocking {
        val shortResult = authManager.signInWithGmail("valid@gmail.com", "123")
        assertTrue(shortResult.isFailure)
    }

    @Test
    fun `signOut clears both user repository and secure tokens`() = runBlocking {
        authManager.signOut()

        coVerify(exactly = 1) { secureTokenManager.clearTokens() }
        coVerify(exactly = 1) { userRepository.signOut() }
    }
}
