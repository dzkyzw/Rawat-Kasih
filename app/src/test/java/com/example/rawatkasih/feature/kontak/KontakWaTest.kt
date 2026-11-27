package com.example.rawatkasih.feature.kontak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KontakWaTest {
    @Test
    fun nomorDinormalkanKeFormatWaMe() {
        assertEquals("6281234567890", normalTelepon("0812-3456-7890"))
        assertEquals("6281234567890", normalTelepon("+62 812 3456 7890"))
        assertEquals("6281234567890", normalTelepon("81234567890"))
        assertTrue(nomorWaValid(normalTelepon("0812 3456 789")))
        assertFalse(nomorWaValid(normalTelepon("12345")))
        assertFalse(nomorWaValid(normalTelepon("")))
    }

    @Test
    fun nomorDitampilkanRapi() {
        assertEquals("+62 812-3456-7890", tampilNomor("6281234567890"))
        assertEquals("123", tampilNomor("123"))
    }
}
