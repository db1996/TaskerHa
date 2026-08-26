package com.github.db1996.taskerha.util

import com.github.db1996.taskerha.datamodels.HaInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HaClientFactoryTest {

    private val instance = HaInstance(
        id = "abc12345",
        remoteUrl = "https://ha.example.com",
        localUrl = "http://192.168.1.50:8123",
        token = "tok",
        homeSsids = setOf("CasaMia")
    )

    @Before
    fun reset() {
        LanLatch.disarm()
    }

    @Test
    fun `selecting the local url arms the latch`() {
        HaClientFactory.noteSelected(instance, "http://192.168.1.50:8123", "CasaMia")
        assertEquals("http://192.168.1.50:8123", LanLatch.armedUrlFor("CasaMia"))
    }

    @Test
    fun `selecting the remote url disarms the latch`() {
        LanLatch.arm("CasaMia", "http://192.168.1.50:8123")
        HaClientFactory.noteSelected(instance, "https://ha.example.com", "CasaMia")
        assertNull(LanLatch.armedUrlFor("CasaMia"))
    }

    @Test
    fun `selecting the local url without an ssid does not arm`() {
        HaClientFactory.noteSelected(instance, "http://192.168.1.50:8123", null)
        assertNull(LanLatch.armedUrlFor("CasaMia"))
    }
}
