package pl.foodhub.pos.core.fiscal.posnet

private const val STX: Byte = 0x02
private const val ETX: Byte = 0x03
private const val STX_CHAR = '\u0002'
private const val ETX_CHAR = '\u0003'
private const val TAB = '\t'
private const val HASH = '#'
private val ERROR_PATTERN = Regex("\\?(\\w+)#")

/**
 * Builds and parses Posnet online protocol frames:
 * `<STX> tab-joined tokens # <crc-hex> <ETX>`.
 */
object PosnetFrameCodec {
    const val END_MARKER = ETX

    fun buildFrame(tokens: List<String>): ByteArray {
        val body = tokens.joinToString(TAB.toString())
        val bodyBytes = body.toByteArray(Charsets.US_ASCII)
        val crcHex = PosnetCrc16.computeHex(bodyBytes)
        val content = "$body$HASH$crcHex"
        return byteArrayOf(STX) + content.toByteArray(Charsets.US_ASCII) + byteArrayOf(ETX)
    }

    fun parse(response: ByteArray): PosnetResponse {
        val text = response.toString(Charsets.US_ASCII).trim(STX_CHAR, ETX_CHAR)

        val errorMatch = ERROR_PATTERN.find(text)
        if (errorMatch != null) return PosnetResponse.Error(errorMatch.groupValues[1])

        val body = text.substringBefore(HASH)
        if (body.isEmpty()) return PosnetResponse.Ambiguous

        val fields =
            body.split(TAB).filter { it.isNotEmpty() }.associate { token ->
                val key = token.takeWhile(Char::isLetter)
                key to token.drop(key.length)
            }
        return PosnetResponse.Fields(fields)
    }
}

sealed interface PosnetResponse {
    data class Fields(val values: Map<String, String>) : PosnetResponse

    data class Error(val code: String) : PosnetResponse

    data object Ambiguous : PosnetResponse
}
