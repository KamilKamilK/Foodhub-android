package pl.foodhub.pos.core.printing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosEncoderTest {
    @Test
    fun `starts with the initialize command and ends with a full cut`() {
        val bytes = EscPosEncoder.encode(listOf("line one"))

        assertEquals(0x1B, bytes[0].toInt() and 0xFF)
        assertEquals('@'.code, bytes[1].toInt() and 0xFF)

        val last3 = bytes.takeLast(3)
        assertEquals(0x1D, last3[0].toInt() and 0xFF)
        assertEquals('V'.code, last3[1].toInt() and 0xFF)
        assertEquals(0x00, last3[2].toInt())
    }

    @Test
    fun `renders each line as plain ascii text separated by newlines`() {
        val bytes = EscPosEncoder.encode(listOf("Pizza", "Cola"))
        val text = String(bytes, Charsets.US_ASCII)

        assertTrue(text.contains("Pizza\n"))
        assertTrue(text.contains("Cola\n"))
    }

    @Test
    fun `transliterates polish diacritics to their ascii equivalents`() {
        val bytes = EscPosEncoder.encode(listOf("Zażółć gęślą jaźń"))
        val text = String(bytes, Charsets.US_ASCII)

        assertTrue(text.contains("Zazolc gesla jazn"))
    }
}
