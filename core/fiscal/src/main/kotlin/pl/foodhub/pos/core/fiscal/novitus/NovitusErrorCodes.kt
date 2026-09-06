package pl.foodhub.pos.core.fiscal.novitus

/**
 * Maps a Novitus error code (from `#n`'s response, see [NovitusFrameCodec.parseStatus])
 * to an operator-facing message. Covers the handful of conditions a cashier can act on
 * directly; anything else surfaces the raw code so it can still be reported/diagnosed.
 */
object NovitusErrorCodes {
    private val messages =
        mapOf(
            1 to "Brak papieru w drukarce fiskalnej.",
            2 to "Otwarta pokrywa drukarki fiskalnej.",
            3 to "Błąd mechanizmu drukującego.",
            4 to "Przegrzanie głowicy drukującej.",
            5 to "Pamięć fiskalna zapełniona.",
        )

    fun messageFor(code: Int): String = messages[code] ?: "Błąd drukarki fiskalnej (kod $code)."
}
