package com.github.db1996.taskerha.datamodels

import org.junit.Assert.assertEquals
import org.junit.Test

class HaInstanceCandidatesTest {

    private fun instance(
        remoteUrl: String = "https://ha.example.com",
        localUrl: String = "http://192.168.1.50:8123",
        homeSsids: Set<String> = setOf("CasaMia")
    ) = HaInstance(
        id = "abc12345",
        remoteUrl = remoteUrl,
        localUrl = localUrl,
        token = "tok",
        homeSsids = homeSsids
    )

    @Test
    fun `no local url means remote only`() {
        val candidates = instance(localUrl = "")
            .resolveUrlCandidates(currentSsid = "CasaMia", latchedUrl = null)
        assertEquals(listOf("https://ha.example.com"), candidates)
    }

    @Test
    fun `no home ssids means remote only`() {
        val candidates = instance(homeSsids = emptySet())
            .resolveUrlCandidates(currentSsid = "CasaMia", latchedUrl = null)
        assertEquals(listOf("https://ha.example.com"), candidates)
    }

    @Test
    fun `unknown ssid means remote only`() {
        val candidates = instance()
            .resolveUrlCandidates(currentSsid = "BarWifi", latchedUrl = null)
        assertEquals(listOf("https://ha.example.com"), candidates)
    }

    @Test
    fun `null ssid means remote only`() {
        val candidates = instance()
            .resolveUrlCandidates(currentSsid = null, latchedUrl = null)
        assertEquals(listOf("https://ha.example.com"), candidates)
    }

    @Test
    fun `home ssid puts remote first and local as fallback`() {
        val candidates = instance()
            .resolveUrlCandidates(currentSsid = "CasaMia", latchedUrl = null)
        assertEquals(
            listOf("https://ha.example.com", "http://192.168.1.50:8123"),
            candidates
        )
    }

    @Test
    fun `an armed latch puts local first but keeps remote as fallback`() {
        val candidates = instance().resolveUrlCandidates(
            currentSsid = "CasaMia",
            latchedUrl = "http://192.168.1.50:8123"
        )
        assertEquals(
            listOf("http://192.168.1.50:8123", "https://ha.example.com"),
            candidates
        )
    }

    @Test
    fun `a latch for a stale url is ignored`() {
        val candidates = instance().resolveUrlCandidates(
            currentSsid = "CasaMia",
            latchedUrl = "http://192.168.9.9:8123"
        )
        assertEquals(
            listOf("https://ha.example.com", "http://192.168.1.50:8123"),
            candidates
        )
    }

    @Test
    fun `resolveUrl returns the first candidate`() {
        assertEquals(
            "https://ha.example.com",
            instance(homeSsids = emptySet()).resolveUrl()
        )
    }
}
