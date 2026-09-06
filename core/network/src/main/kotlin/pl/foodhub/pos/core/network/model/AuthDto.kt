package pl.foodhub.pos.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    @SerialName("macAddress") val macAddress: String,
    @SerialName("name") val name: String,
    @SerialName("model") val model: String,
    @SerialName("platform") val platform: String = "Android",
    @SerialName("version") val version: String,
)

/** Body for POST /v1/auth/pos-login (PosPinAuthenticator, foodhub-api). */
@Serializable
data class PosLoginRequestDto(
    @SerialName("pin") val pin: String,
    @SerialName("device") val device: DeviceDto,
    @SerialName("posId") val posId: String? = null,
)

/**
 * Body for POST /v1/auth/refresh-token. [device] is included so the backend can
 * re-resolve the place/POS context (RefreshTokenAuthenticator) and reissue a
 * [AuthTokensDto.mercureToken] scoped to it -- without it, the response carries
 * neither claim, since foodhub-api only resolves them from a device payload.
 */
@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("device") val device: DeviceDto? = null,
)

/** Response shared by /auth/login, /auth/pos-login and /auth/refresh-token. */
@Serializable
data class AuthTokensDto(
    @SerialName("token") val token: String,
    @SerialName("refreshToken") val refreshToken: String? = null,
    @SerialName("mercureToken") val mercureToken: String? = null,
)
