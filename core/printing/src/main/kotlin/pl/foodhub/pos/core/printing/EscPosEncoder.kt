package pl.foodhub.pos.core.printing

import java.io.ByteArrayOutputStream

private const val ESC = 0x1B
private const val GS = 0x1D
private const val FEED_LINES = 3

private val POLISH_DIACRITICS =
    mapOf(
        'ą' to "a", 'ć' to "c", 'ę' to "e", 'ł' to "l", 'ń' to "n", 'ó' to "o", 'ś' to "s", 'ź' to "z", 'ż' to "z",
        'Ą' to "A", 'Ć' to "C", 'Ę' to "E", 'Ł' to "L", 'Ń' to "N", 'Ó' to "O", 'Ś' to "S", 'Ź' to "Z", 'Ż' to "Z",
    )

/**
 * Minimal ESC/POS byte builder: initialize, plain text lines, feed, full cut -- the
 * only commands a kitchen ticket or receipt needs. Hand-rolled instead of a
 * third-party ESC/POS library: the subset used is tiny and has been stable for
 * decades, and this keeps core:printing dependency-free.
 *
 * Text is transliterated to plain ASCII rather than sent as UTF-8 or a guessed single-
 * byte code page: which code page (852/Mazovia/Windows-1250) a given printer expects
 * for Polish diacritics depends on the terminal hardware model, still an open decision
 * (ANDROID_POS_ARCHITECTURE.md decision D2) -- ASCII transliteration prints correctly
 * on any ESC/POS printer regardless of that choice, at the cost of dropped diacritics.
 */
object EscPosEncoder {
    fun encode(lines: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(ESC.toByte(), '@'.code.toByte()))
        lines.forEach { line ->
            out.write(line.toAsciiSafe().toByteArray(Charsets.US_ASCII))
            out.write('\n'.code)
        }
        repeat(FEED_LINES) { out.write('\n'.code) }
        out.write(byteArrayOf(GS.toByte(), 'V'.code.toByte(), 0x00))
        return out.toByteArray()
    }

    private fun String.toAsciiSafe(): String =
        buildString {
            this@toAsciiSafe.forEach { char -> append(POLISH_DIACRITICS[char] ?: char) }
        }
}
