package pl.foodhub.pos.core.fiscal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {
    @Test
    fun `rounds half up instead of truncating`() {
        // A naive (value * 100).toInt() truncates 12.005 to 1200 grosz, silently
        // dropping the last cent -- this must round to 1201.
        assertEquals(1201L, BigDecimal("12.005").toGrosz())
    }

    @Test
    fun `exact values convert without drift`() {
        assertEquals(1299L, BigDecimal("12.99").toGrosz())
        assertEquals(0L, BigDecimal("0").toGrosz())
        assertEquals(100L, BigDecimal("1").toGrosz())
    }

    @Test
    fun `rounds down when the third decimal is below the midpoint`() {
        assertEquals(1200L, BigDecimal("12.001").toGrosz())
    }
}
