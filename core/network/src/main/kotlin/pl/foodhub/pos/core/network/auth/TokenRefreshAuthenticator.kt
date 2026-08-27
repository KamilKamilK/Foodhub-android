package pl.foodhub.pos.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On a 401 for an authenticated call, refreshes the token once and replays the
 * request. A second consecutive 401 (responseCount > 1) means the refresh token is
 * dead too -- give up and let core:auth tear the session down.
 */
class TokenRefreshAuthenticator(
    private val tokenProvider: AuthTokenProvider,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (responseCount(response) > 1) {
            runBlocking { tokenProvider.onAuthenticationLost() }
            return null
        }

        val refreshed = runBlocking { tokenProvider.refresh() }
        if (!refreshed) {
            runBlocking { tokenProvider.onAuthenticationLost() }
            return null
        }

        val token = runBlocking { tokenProvider.accessToken() } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
