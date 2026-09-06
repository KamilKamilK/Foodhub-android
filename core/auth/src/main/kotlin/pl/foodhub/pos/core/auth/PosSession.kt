package pl.foodhub.pos.core.auth

/**
 * The place/POS context resolved from the JWT issued by the terminal's PIN login
 * (`POST /v1/auth/pos-login`) or a device-aware token refresh. Persisted independently
 * of the token pair in [TokenStore], since foodhub-api's `JWTCreatedListener` only
 * embeds these claims when the authenticating request carried a `device` payload.
 */
data class PosSession(
    val placeId: String,
    val placeName: String,
    val posId: String?,
)
