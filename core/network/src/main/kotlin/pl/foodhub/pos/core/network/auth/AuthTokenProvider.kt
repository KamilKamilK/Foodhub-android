package pl.foodhub.pos.core.network.auth

/**
 * Read-side contract the network layer needs for authenticated calls, implemented in
 * core:auth (which owns the encrypted token store and the refresh call). Declared here
 * so core:network never has to depend on core:auth -- the dependency runs one way,
 * core:auth -> core:network.
 */
interface AuthTokenProvider {
    /** The current access token, or null when the terminal has no session yet. */
    suspend fun accessToken(): String?

    /**
     * Exchange the refresh token for a fresh access token. Returns true when a new
     * token is now available. Called by [TokenRefreshAuthenticator] on a 401.
     */
    suspend fun refresh(): Boolean

    /** Drop the local session after an unrecoverable auth failure. */
    suspend fun onAuthenticationLost()
}
