package pl.foodhub.pos.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the bearer access token to every request except the unauthenticated auth
 * endpoints (login, PIN login, token refresh), which must go out bare.
 */
class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Login / PIN login / token refresh must go out unauthenticated.
        if (request.url.pathSegments.contains("auth")) {
            return chain.proceed(request)
        }

        val token = runBlocking { tokenProvider.accessToken() }
        val authorized =
            if (token != null) {
                request.newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                request
            }

        return chain.proceed(authorized)
    }
}
