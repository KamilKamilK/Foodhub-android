package pl.foodhub.pos.testing

import java.util.Base64

/**
 * An unsigned JWT carrying the same `place`/`posId` claims `core:auth`'s
 * `JwtSessionDecoder` reads out of a real `pos-login` token. The signature segment is
 * never checked client-side (the backend re-validates place/POS on every request that
 * uses them), so any placeholder value stands in for one here.
 */
fun fakeJwt(
    placeId: String,
    placeName: String = "Test Place",
    posId: String? = null,
): String {
    val header = """{"alg":"none","typ":"JWT"}"""
    val posIdJson = posId?.let { "\"$it\"" } ?: "null"
    val payload = """{"place":{"id":"$placeId","name":"$placeName"},"posId":$posIdJson}"""
    return "${encode(header)}.${encode(payload)}.signature"
}

private fun encode(json: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
