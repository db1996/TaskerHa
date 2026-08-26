package com.github.db1996.taskerha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LanLatchTest {

    @Before
    fun reset() {
        LanLatch.disarm()
    }

    @Test
    fun `returns null when never armed`() {
        assertNull(LanLatch.armedUrlFor("CasaMia"))
    }

    @Test
    fun `returns the url for the ssid it was armed on`() {
        LanLatch.arm("CasaMia", "http://192.168.1.50:8123")
        assertEquals("http://192.168.1.50:8123", LanLatch.armedUrlFor("CasaMia"))
    }

    @Test
    fun `returns null for a different ssid`() {
        LanLatch.arm("CasaMia", "http://192.168.1.50:8123")
        assertNull(LanLatch.armedUrlFor("BarWifi"))
    }

    @Test
    fun `returns null for a null ssid`() {
        LanLatch.arm("CasaMia", "http://192.168.1.50:8123")
        assertNull(LanLatch.armedUrlFor(null))
    }

    @Test
    fun `disarm clears the latch`() {
        LanLatch.arm("CasaMia", "http://192.168.1.50:8123")
        LanLatch.disarm()
        assertNull(LanLatch.armedUrlFor("CasaMia"))
    }

    @Test
    fun `arming with a null ssid is a no-op`() {
        LanLatch.arm(null, "http://192.168.1.50:8123")
        assertNull(LanLatch.armedUrlFor(null))
    }

    @Test
    fun `arming with a blank url is a no-op`() {
        LanLatch.arm("CasaMia", "")
        assertNull(LanLatch.armedUrlFor("CasaMia"))
    }
}
