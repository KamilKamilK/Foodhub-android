package pl.foodhub.pos.core.fiscal.posnet

private const val POLYNOMIAL = 0x1021
private const val INITIAL = 0xFFFF
private const val WORD_MASK = 0xFFFF
private const val HIGH_BIT = 0x8000
private const val BYTE_SHIFT = 8
private const val BITS_PER_BYTE = 8
private const val BYTE_MASK = 0xFF
private const val HEX_RADIX = 16

/**
 * CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF, no reflection) -- the documented
 * standard the Posnet control-sum is based on; confirm against Posnet's published
 * protocol documentation before relying on it against real hardware.
 */
object PosnetCrc16 {
    fun compute(bytes: ByteArray): Int {
        var crc = INITIAL
        for (byte in bytes) {
            crc = crc xor ((byte.toInt() and BYTE_MASK) shl BYTE_SHIFT)
            repeat(BITS_PER_BYTE) {
                crc =
                    if (crc and HIGH_BIT != 0) {
                        (crc shl 1) xor POLYNOMIAL
                    } else {
                        crc shl 1
                    }
                crc = crc and WORD_MASK
            }
        }
        return crc
    }

    fun computeHex(bytes: ByteArray): String = compute(bytes).toString(HEX_RADIX)
}
