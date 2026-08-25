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
            candidateProvider = { listOf(url(first), url(second)) },
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
            candidateProvider = { listOf(url(first), url(second)) },
            onEndpointSelected = { selected.add(it) }
        )

        val ok = runBlocking { client.ping() }

        assertTrue(ok)
        assertEquals(url(second), client.baseUrl)
        assertEquals(listOf(url(second)), selected)
        assertEquals(HomeassistantStatus.CONNECTED, client.homeAssistantStatus)
        // The first candidate's failure must not be left behind: BaseViewModel treats
        // a non-empty error as a broken connection even after a successful ping.
        assertEquals("", client.error)
    }

    @Test
    fun `asks the provider again on every ping so a changed list takes effect`() {
        first.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        second.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        var invocations = 0
        var candidates = listOf(url(first))
        val client = HomeAssistantClient(
            baseUrl = url(first),
            accessToken = "tok",
            candidateProvider = {
                invocations++
                candidates
            }
        )

        assertTrue(runBlocking { client.ping() })
        assertEquals(1, invocations)
        assertEquals(url(first), client.baseUrl)

        // Same client, different network: the provider now offers a different endpoint.
        candidates = listOf(url(second))

        assertTrue(runBlocking { client.ping() })
        assertEquals(2, invocations)
        assertEquals(url(second), client.baseUrl)
        assertEquals(1, first.requestCount)
        assertEquals(1, second.requestCount)
    }

    @Test
    fun `fails and restores the first candidate when every candidate fails`() {
        first.enqueue(MockResponse().setResponseCode(502))
        second.enqueue(MockResponse().setResponseCode(503))
        val selected = mutableListOf<String>()
        val client = HomeAssistantClient(
            baseUrl = url(first),
            accessToken = "tok",
            candidateProvider = { listOf(url(first), url(second)) },
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
