package com.github.db1996.taskerha.client

import com.github.db1996.taskerha.enums.HomeassistantStatus
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeAssistantClientPingTest {

    private lateinit var first: MockWebServer
    private lateinit var second: MockWebServer

    @Before
    fun setUp() {
        first = MockWebServer().apply { start() }
        second = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        first.shutdown()
        second.shutdown()
    }

    private fun url(server: MockWebServer): String =
        server.url("/").toString().trimEnd('/')

    @Test
    fun `settles on the first candidate when it answers`() {
        first.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val selected = mutableListOf<String>()
        val client = HomeAssistantClient(
            baseUrl = url(first),
            accessToken = "tok",
            urlCandidates = listOf(url(first), url(second)),
            onEndpointSelected = { selected.add(it) }
        )

        val ok = runBlocking { client.ping() }

        assertTrue(ok)
        assertEquals(url(first), client.baseUrl)
        assertEquals(listOf(url(first)), selected)
        assertEquals(HomeassistantStatus.CONNECTED, client.homeAssistantStatus)
        assertEquals(0, second.requestCount)
    }

    @Test
    fun `falls back to the second candidate when the first fails`() {
        first.enqueue(MockResponse().setResponseCode(502))
        second.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val selected = mutableListOf<String>()
        val client = HomeAssistantClient(
            baseUrl = url(first),
            accessToken = "tok",
            urlCandidates = listOf(url(first), url(second)),
            onEndpointSelected = { selected.add(it) }
        )

        val ok = runBlocking { client.ping() }

        assertTrue(ok)
        assertEquals(url(second), client.baseUrl)
        assertEquals(listOf(url(second)), selected)
        assertEquals(HomeassistantStatus.CONNECTED, client.homeAssistantStatus)
    }

    @Test
    fun `fails and restores the first candidate when every candidate fails`() {
        first.enqueue(MockResponse().setResponseCode(502))
        second.enqueue(MockResponse().setResponseCode(503))
        val selected = mutableListOf<String>()
        val client = HomeAssistantClient(
            baseUrl = url(first),
            accessToken = "tok",
            urlCandidates = listOf(url(first), url(second)),
            onEndpointSelected = { selected.add(it) }
        )

        val ok = runBlocking { client.ping() }

        assertFalse(ok)
        assertEquals(url(first), client.baseUrl)
        assertTrue(selected.isEmpty())
        assertEquals(HomeassistantStatus.NO_CONNECTION, client.homeAssistantStatus)
        assertTrue(client.error.isNotEmpty())
    }

    @Test
    fun `with no candidates it pings baseUrl as before`() {
        first.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val client = HomeAssistantClient(baseUrl = url(first), accessToken = "tok")

        val ok = runBlocking { client.ping() }

        assertTrue(ok)
        assertEquals(url(first), client.baseUrl)
    }
}
