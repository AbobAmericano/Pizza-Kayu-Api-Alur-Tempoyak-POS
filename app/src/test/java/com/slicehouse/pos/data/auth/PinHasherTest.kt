package com.slicehouse.pos.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun hashesVerifyAndNeverContainThePin() {
        val stored = PinHasher.hash("4821")
        assertTrue(stored.startsWith("pbkdf2-sha256$"))
        assertFalse(stored.contains("4821"))
        assertTrue(PinHasher.verify("4821", stored))
        assertFalse(PinHasher.verify("4822", stored))
        assertFalse(PinHasher.verify("", stored))
    }

    @Test
    fun samePinGetsADifferentSaltEachTime() {
        val a = PinHasher.hash("1234")
        val b = PinHasher.hash("1234")
        assertNotEquals(a, b)
        assertTrue(PinHasher.verify("1234", a))
        assertTrue(PinHasher.verify("1234", b))
    }

    @Test
    fun brokenStoredValuesNeverMatch() {
        assertFalse(PinHasher.verify("1234", ""))
        assertFalse(PinHasher.verify("1234", "1234"))
        assertFalse(PinHasher.verify("1234", "pbkdf2-sha256\$x\$abc\$def"))
        assertFalse(PinHasher.verify("1234", "md5\$1\$abc\$def"))
    }

    @Test
    fun pinsAreFourToSixDigits() {
        assertTrue(PinHasher.isValidPin("1234"))
        assertTrue(PinHasher.isValidPin("123456"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("1234567"))
        assertFalse(PinHasher.isValidPin("12a4"))
        assertFalse(PinHasher.isValidPin(""))
    }

    @Test
    fun recoveryCodesAreEasyToCopy() {
        val code = PinHasher.newRecoveryCode()
        assertEquals(12, code.length)
        assertTrue(code.none { it in "01OIL" })
        val written = PinHasher.formatRecoveryCode(code)
        assertEquals(14, written.length) // ABCD-EFGH-JKMN
        assertEquals(code, PinHasher.normalizeRecoveryCode(written.lowercase().replace("-", " - ")))
    }
}
