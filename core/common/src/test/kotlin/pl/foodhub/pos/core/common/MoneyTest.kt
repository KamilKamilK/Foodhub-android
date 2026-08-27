package pl.foodhub.pos.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun `formats whole and fractional grosze`() {
        assertEquals("12,00 zł", Money(1200).formatPln())
        assertEquals("12,05 zł", Money(1205).formatPln())
        assertEquals("0,09 zł", Money(9).formatPln())
    }

    @Test
    fun `formats negative amounts`() {
        assertEquals("-3,50 zł", Money(-350).formatPln())
    }

    @Test
    fun `adds and multiplies in minor units`() {
        assertEquals(Money(3600), Money(1200) * 3)
        assertEquals(Money(1500), Money(1200) + Money(300))
    }
}
