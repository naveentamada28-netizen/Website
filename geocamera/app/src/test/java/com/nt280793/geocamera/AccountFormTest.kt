package com.nt280793.geocamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountFormTest {
    @Test
    fun acceptsAMobileNumberAndRejectsAShortOne() {
        assertEquals("9876543210", AccountForm.normalizeMobile("+91 98765 43210"))
        assertTrue(AccountForm.mobileOk("9876543210"))
        assertFalse(AccountForm.mobileOk("12345"))
        assertTrue(AccountForm.passwordOk("secret1"))
        assertFalse(AccountForm.passwordOk("short"))
        assertTrue(AccountForm.serverOk("https://example.com/hosting"))
        assertFalse(AccountForm.serverOk("example.com"))
    }
}
