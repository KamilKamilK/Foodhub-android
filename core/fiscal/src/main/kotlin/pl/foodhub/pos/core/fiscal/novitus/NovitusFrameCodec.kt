package pl.foodhub.pos.core.fiscal.novitus

private const val ESC: Byte = 27
private const val FRAME_START: Byte = 0x50 // 'P'
private const val FRAME_END: Byte = 0x5C // '\'
private const val ENQ: Byte = 0x05
private const val DLE: Byte = 0x10
private const val ERROR_DETAIL_PREFIX = "\u001BP1#E"
private const val STATUS_FRAME_PREFIX = "110"
private const val MECHANISM_FRAME_PREFIX = "1110"
private const val STATUS_FRAME_LENGTH = 7
private val DIGIT_RUN = Regex("\\d+")

/**
 * Builds and parses Novitus protocol frames: `<ESC>P <content> <CRC> <ESC>\`. The
 * frame's end marker is the trailing [FRAME_END] byte, so [pl.foodhub.pos.core.fiscal
 * .transport.FiscalTcpConnection.exchange] reads until that byte is seen.
 */
object NovitusFrameCodec {
    val enqProbe = byteArrayOf(ENQ)
    val dleProbe = byteArrayOf(DLE)

    fun buildFrame(content: ByteArray): ByteArray {
        val crc = NovitusCrc.compute(content).toByteArray(Charsets.US_ASCII)
        return byteArrayOf(ESC, FRAME_START) + content + crc + byteArrayOf(ESC, FRAME_END)
    }

    fun buildFrame(content: String): ByteArray = buildFrame(content.toByteArray(Charsets.US_ASCII))

    /**
     * The device's direct reply to a commit (`$y`) frame is expected to echo the
     * assigned fiscal document number as a run of ASCII digits somewhere in the
     * response (the last one, since the response ends with the `<ESC>\` frame
     * markers, not a digit) -- confirm the exact position against the manufacturer's
     * protocol documentation before relying on this in production; a driver seeing no
     * digit run here treats the outcome as [NovitusStatus.Ambiguous] rather than
     * fabricating a document number.
     */
    fun extractDocumentNumber(response: ByteArray): String? =
        DIGIT_RUN.findAll(response.toString(Charsets.US_ASCII)).lastOrNull()?.value

    fun parseStatus(response: ByteArray): NovitusStatus {
        val text = response.toString(Charsets.US_ASCII)
        val bits = text.filter { it == '0' || it == '1' }

        return when {
            text.startsWith(ERROR_DETAIL_PREFIX) -> {
                val code = text.removePrefix(ERROR_DETAIL_PREFIX).takeWhile(Char::isDigit).toIntOrNull()
                if (code != null) NovitusStatus.Error(code) else NovitusStatus.Ambiguous
            }
            bits.length >= STATUS_FRAME_LENGTH && bits.startsWith(STATUS_FRAME_PREFIX) ->
                NovitusStatus.Ok(lastTransactionCorrect = bits.last() == '1')
            bits.length >= MECHANISM_FRAME_PREFIX.length && bits.startsWith(MECHANISM_FRAME_PREFIX) ->
                NovitusStatus.MechanismError
            else -> NovitusStatus.Ambiguous
        }
    }
}

sealed interface NovitusStatus {
    data class Ok(val lastTransactionCorrect: Boolean) : NovitusStatus

    data class Error(val code: Int) : NovitusStatus

    data object MechanismError : NovitusStatus

    data object Ambiguous : NovitusStatus
}
