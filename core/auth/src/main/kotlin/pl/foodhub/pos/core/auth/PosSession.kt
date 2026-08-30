package pl.foodhub.pos.core.auth

/**
 * The place/POS context resolved from the JWT issued by the terminal's PIN login
 * (`POST /v1/auth/pos-login`). Persisted independently of the token pair in
 * [TokenStore]: a refreshed JWT does not carry these claims again, because
 * foodhub-api's `JWTCreatedListener` only injects them when the authenticating
 * request carried a `device` payload, and `/v1/auth/refresh-token` does not send one.
 */
data class PosSession(
    val placeId: String,
    val placeName: String,
    val posId: String?,
)
