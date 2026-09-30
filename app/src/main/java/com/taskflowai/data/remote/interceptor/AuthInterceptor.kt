package com.taskflowai.data.remote.interceptor

import com.taskflowai.security.SecureTokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val secureTokenManager: SecureTokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = secureTokenManager.getAuthToken()

        val requestBuilder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        requestBuilder.header("Accept", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}
