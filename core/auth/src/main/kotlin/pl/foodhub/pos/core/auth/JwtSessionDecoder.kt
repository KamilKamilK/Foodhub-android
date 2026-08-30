package pl.foodhub.pos.core.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Reads the place/POS context foodhub-api's `JWTCreatedListener` embeds in the JWT
 * payload when the authenticating request carried a `device` object (pos-login
 * always does). The signature is never checked here -- these claims only pick which
 * place/POS context the UI operates in, never an authorization decision; the backend
 * re-validates placeId/posId on every request that uses them.
 */
object JwtSessionDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(jwt: String): PosSession? {
        val payload = payloadOf(jwt) ?: return null
        val claims = runCatching { json.decodeFromString<Claims>(payload) }.getOrNull() ?: return null
        val place = claims.place ?: return null
        return PosSession(placeId = place.id, placeName = place.name, posId = claims.posId)
    }

    private fun payloadOf(jwt: String): String? {
        val segments = jwt.split(".")
        if (segments.size < 2) return null
        return runCatching {
            String(Base64.getUrlDecoder().decode(segments[1].withBase64Padding()), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun String.withBase64Padding(): String {
        val remainder = length % BASE64_BLOCK_SIZE
        return if (remainder == 0) this else this + "=".repeat(BASE64_BLOCK_SIZE - remainder)
    }

    private const val BASE64_BLOCK_SIZE = 4

    @Serializable
    private data class Claims(
        @SerialName("place") val place: PlaceClaim? = null,
        @SerialName("posId") val posId: String? = null,
    )

    @Serializable
    private data class PlaceClaim(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String = "",
    )
}
