package com.slicehouse.pos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun formatsSen() {
        assertEquals("RM25.50", money(2550))
        assertEquals("RM0.05", money(5))
        assertEquals("RM0.00", money(0))
        assertEquals("-RM2.50", money(-250))
        assertEquals("+RM3.00", signedMoney(300))
        assertEquals("-RM3.00", signedMoney(-300))
    }

    @Test
    fun parsesTypedPrices() {
        assertEquals(2500L, parseMoney("25"))
        assertEquals(2550L, parseMoney("25.5"))
        assertEquals(2550L, parseMoney("25,50"))
        assertEquals(1L, parseMoney("0.005")) // rounds half up to the nearest sen
        assertNull(parseMoney(""))
        assertNull(parseMoney("abc"))
        assertNull(parseMoney("-1"))
        assertNull(parseMoney("1.2.3"))
    }

    @Test
    fun editableTextRoundTrips() {
        assertEquals("25", moneyInput(2500))
        assertEquals("4.50", moneyInput(450))
        assertEquals("0.05", moneyInput(5))
        for (sen in listOf(0L, 5L, 450L, 2500L, 123456L)) {
            assertEquals(sen, parseMoney(moneyInput(sen)))
        }
    }

    @Test
    fun convertsOldDoublesWithoutDrift() {
        assertEquals(30L, toSen(0.1 + 0.2)) // 0.30000000000000004
        assertEquals(1015L, toSen(10.15))
        assertEquals(2999L, toSen(29.99))
        assertEquals(800, toBasisPoints(8.0))
        assertEquals(650, toBasisPoints(6.5))
    }

    @Test
    fun taxRoundsHalfUpToTheSen() {
        assertEquals(160L, taxSen(2000, 800)) // 8% of RM20
        assertEquals(299L, taxSen(4600, 650)) // 6.5% of RM46 = RM2.99
        assertEquals(1L, taxSen(13, 400)) // 0.52 sen -> 1 sen
        assertEquals(0L, taxSen(12, 400)) // 0.48 sen -> 0 sen
        assertEquals(0L, taxSen(2000, 0))
    }

    @Test
    fun percentFieldRoundTrips() {
        assertEquals("8", percentLabel(800))
        assertEquals("6.5", percentLabel(650))
        assertEquals("0", percentLabel(0))
        assertEquals(800, parsePercent("8"))
        assertEquals(650, parsePercent("6,5"))
        assertNull(parsePercent("101"))
        assertNull(parsePercent("-1"))
        assertNull(parsePercent(""))
    }
}
