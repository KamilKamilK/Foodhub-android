package pl.foodhub.pos.core.fiscal.posnet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosnetFrameCodecTest {
    @Test
    fun `buildFrame wraps the tab-joined tokens between STX and ETX with a trailing checksum`() {
        val frame = PosnetFrameCodec.buildFrame(listOf("trinit", "bm=1"))

        assertEquals(0x02, frame.first().toInt())
        assertEquals(0x03, frame.last().toInt())
        val body = frame.copyOfRange(1, frame.size - 1).toString(Charsets.US_ASCII)
        assertTrue(body.startsWith("trinit\tbm=1#"))
    }

    @Test
    fun `a frame built by this codec round-trips through parse as Fields`() {
        val frame = PosnetFrameCodec.buildFrame(listOf("scnt"))
        // Simulate a device echoing back a plausible response for the same command shape.
        val response = PosnetFrameCodec.buildFrame(listOf("rd10", "bt00042"))

        assertTrue(PosnetFrameCodec.parse(frame) is PosnetResponse.Fields)
        val parsed = PosnetFrameCodec.parse(response)
        assertTrue(parsed is PosnetResponse.Fields)
    }

    @Test
    fun `parse extracts named fields from a tab-delimited response`() {
        val response = PosnetFrameCodec.buildFrame(listOf("bt00042", "rd0010"))

        val parsed = PosnetFrameCodec.parse(response) as PosnetResponse.Fields

        assertEquals("00042", parsed.values["bt"])
        assertEquals("0010", parsed.values["rd"])
    }

    @Test
    fun `parse recognizes an error frame`() {
        val response = "?12#".toByteArray(Charsets.US_ASCII)

        val parsed = PosnetFrameCodec.parse(response)

        assertEquals(PosnetResponse.Error("12"), parsed)
    }

    @Test
    fun `parse never guesses on an empty body -- it reports ambiguous`() {
        val response = "#".toByteArray(Charsets.US_ASCII)

        assertEquals(PosnetResponse.Ambiguous, PosnetFrameCodec.parse(response))
    }
}
