package pl.foodhub.pos.core.fiscal.novitus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NovitusCrcTest {
    @Test
    fun `is deterministic for the same input`() {
        val bytes = "0;0;0;0\$h".toByteArray(Charsets.US_ASCII)

        assertEquals(NovitusCrc.compute(bytes), NovitusCrc.compute(bytes))
    }

    @Test
    fun `formats as 4-char uppercase hex`() {
        val crc = NovitusCrc.compute("test".toByteArray(Charsets.US_ASCII))

        assertEquals(4, crc.length)
        assertEquals(crc, crc.uppercase())
        assert(crc.all { it.isDigit() || it in 'A'..'F' })
    }

    @Test
    fun `different content produces different checksums`() {
        val a = NovitusCrc.compute("0;0;0;0\$h".toByteArray(Charsets.US_ASCII))
        val b = NovitusCrc.compute("0;0;0;1\$h".toByteArray(Charsets.US_ASCII))

        assertNotEquals(a, b)
    }

    @Test
    fun `empty input returns the seed formatted as hex`() {
        val crc = NovitusCrc.compute(ByteArray(0))

        assertEquals("FFFF", crc)
    }
}
