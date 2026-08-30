package pl.foodhub.pos.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtSessionDecoderTest {
    private fun fakeJwt(payloadJson: String): String {
        val header = base64Url("""{"alg":"none"}""")
        val payload = base64Url(payloadJson)
        return "$header.$payload.signature"
    }

    private fun base64Url(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    @Test
    fun `decodes place and posId from the JWT payload`() {
        val jwt = fakeJwt("""{"place":{"id":"place-1","name":"Bistro"},"posId":"pos-9"}""")

        val session = JwtSessionDecoder.decode(jwt)

        assertEquals(PosSession(placeId = "place-1", placeName = "Bistro", posId = "pos-9"), session)
    }

    @Test
    fun `defaults the place name and posId when absent`() {
        val jwt = fakeJwt("""{"place":{"id":"place-1"}}""")

        val session = JwtSessionDecoder.decode(jwt)

        assertEquals(PosSession(placeId = "place-1", placeName = "", posId = null), session)
    }

    @Test
    fun `returns null when the token carries no place claim`() {
        val jwt = fakeJwt("""{"uuid":"user-1"}""")

        assertNull(JwtSessionDecoder.decode(jwt))
    }

    @Test
    fun `returns null for a malformed token`() {
        assertNull(JwtSessionDecoder.decode("not-a-jwt"))
    }
}
