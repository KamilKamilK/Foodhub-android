package pl.foodhub.pos.core.fiscal.novitus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovitusFrameCodecTest {
    @Test
    fun `buildFrame wraps content between ESC P and ESC backslash with a trailing CRC`() {
        val content = "0;0;0;0\$h".toByteArray(Charsets.US_ASCII)

        val frame = NovitusFrameCodec.buildFrame(content)

        assertEquals(27, frame[0].toInt())
        assertEquals('P'.code, frame[1].toInt())
        assertEquals(27, frame[frame.size - 2].toInt())
        assertEquals('\\'.code, frame[frame.size - 1].toInt())
        // content + 4-char hex CRC sit between the two framing markers.
        assertEquals(content.size + 4, frame.size - 4)
    }

    @Test
    fun `parseStatus recognizes an ok status frame and reads the last-transaction bit`() {
        val okCorrect = "1100001".toByteArray(Charsets.US_ASCII)
        val okIncorrect = "1100000".toByteArray(Charsets.US_ASCII)

        assertEquals(NovitusStatus.Ok(lastTransactionCorrect = true), NovitusFrameCodec.parseStatus(okCorrect))
        assertEquals(NovitusStatus.Ok(lastTransactionCorrect = false), NovitusFrameCodec.parseStatus(okIncorrect))
    }

    @Test
    fun `parseStatus recognizes an error-detail frame and extracts the numeric code`() {
        val response = "P1#E42".toByteArray(Charsets.US_ASCII)

        val status = NovitusFrameCodec.parseStatus(response)

        assertEquals(NovitusStatus.Error(42), status)
    }

    @Test
    fun `parseStatus recognizes a mechanism-error frame`() {
        val response = "1110000".toByteArray(Charsets.US_ASCII)

        assertEquals(NovitusStatus.MechanismError, NovitusFrameCodec.parseStatus(response))
    }

    @Test
    fun `parseStatus never guesses on garbage -- it reports ambiguous`() {
        val response = "garbage response".toByteArray(Charsets.US_ASCII)

        assertEquals(NovitusStatus.Ambiguous, NovitusFrameCodec.parseStatus(response))
    }

    @Test
    fun `extractDocumentNumber reads a trailing digit run`() {
        val response = "ACK RECEIPT 000123".toByteArray(Charsets.US_ASCII)

        assertEquals("000123", NovitusFrameCodec.extractDocumentNumber(response))
    }

    @Test
    fun `extractDocumentNumber returns null rather than fabricating a number`() {
        val response = "ACK RECEIPT".toByteArray(Charsets.US_ASCII)

        assertNull(NovitusFrameCodec.extractDocumentNumber(response))
        assertTrue(NovitusFrameCodec.extractDocumentNumber(ByteArray(0)) == null)
    }
}
