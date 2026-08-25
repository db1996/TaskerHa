package com.github.db1996.taskerha.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityTest {

    @Test
    fun `plain http is cleartext`() {
        assertTrue(UrlSecurity.isCleartext("http://192.168.1.50:8123"))
    }

    @Test
    fun `uppercase scheme is cleartext`() {
        assertTrue(UrlSecurity.isCleartext("HTTP://192.168.1.50:8123"))
    }

    @Test
    fun `leading whitespace does not hide cleartext`() {
        assertTrue(UrlSecurity.isCleartext("   http://192.168.1.50:8123"))
    }

    @Test
    fun `https is not cleartext`() {
        assertFalse(UrlSecurity.isCleartext("https://ha.example.com"))
    }

    @Test
    fun `httpsblank prefix is not confused with http`() {
        assertFalse(UrlSecurity.isCleartext("https://http.example.com"))
    }

    @Test
    fun `blank is not cleartext`() {
        assertFalse(UrlSecurity.isCleartext(""))
        assertFalse(UrlSecurity.isCleartext("   "))
    }

    @Test
    fun `a bare host is not flagged`() {
        assertFalse(UrlSecurity.isCleartext("ha.example.com"))
    }
}
