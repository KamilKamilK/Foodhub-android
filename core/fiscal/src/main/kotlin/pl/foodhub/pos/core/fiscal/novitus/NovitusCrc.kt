package pl.foodhub.pos.core.fiscal.novitus

private const val SEED = 0xFFFF
private const val BYTE_MASK = 0xFF
private const val WORD_MASK = 0xFFFF
private const val SHIFT_OFFSET = 0xAA
private const val SHIFT_MODULUS = 9

/**
 * The Novitus fiscal-printer protocol's checksum -- not a standard CRC16, a custom
 * XOR-rotate over the content bytes. Formatted as 4-char uppercase hex, as the
 * protocol expects it inline in the frame (see [NovitusFrameCodec]).
 */
object NovitusCrc {
    fun compute(bytes: ByteArray): String {
        var crc = SEED
        for (i in bytes.indices) {
            val shift = (i + SHIFT_OFFSET) % SHIFT_MODULUS
            crc = crc xor (((BYTE_MASK and bytes[i].toInt()) shl shift) and WORD_MASK)
        }
        return "%04X".format(crc)
    }
}
