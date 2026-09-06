package pl.foodhub.pos.core.fiscal.posnet

import org.junit.Assert.assertEquals
import org.junit.Test

class PosnetCrc16Test {
    @Test
    fun `matches the standard CRC-16 CCITT-FALSE check value for the reference test string`() {
        // "123456789" -> 0x29B1 is the published check value for CRC-16/CCITT-FALSE
        // (poly 0x1021, init 0xFFFF, no reflection) -- confirms this implementation is
        // a correct instance of the standard, not just internally self-consistent.
        val crc = PosnetCrc16.compute("123456789".toByteArray(Charsets.US_ASCII))

        assertEquals(0x29B1, crc)
    }

    @Test
    fun `empty input returns the seed unchanged`() {
        val crc = PosnetCrc16.compute(ByteArray(0))

        assertEquals(0xFFFF, crc)
    }

    @Test
    fun `computeHex formats without leading zero padding`() {
        val hex = PosnetCrc16.computeHex("123456789".toByteArray(Charsets.US_ASCII))

        assertEquals("29b1", hex)
    }

    @Test
    fun `different inputs produce different checksums`() {
        val a = PosnetCrc16.compute("trinit\tbm=1".toByteArray(Charsets.US_ASCII))
        val b = PosnetCrc16.compute("trinit\tbm=2".toByteArray(Charsets.US_ASCII))

        assert(a != b)
    }
}
