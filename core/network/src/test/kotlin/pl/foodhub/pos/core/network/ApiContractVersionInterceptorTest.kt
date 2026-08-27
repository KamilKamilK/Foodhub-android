package pl.foodhub.pos.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ApiContractVersionInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `declares the API contract version on every request`() {
        server.enqueue(MockResponse())
        val client =
            OkHttpClient.Builder()
                .addInterceptor(ApiContractVersionInterceptor(contractVersion = 3))
                .build()

        client.newCall(Request.Builder().url(server.url("/v1/menu")).build()).execute().close()

        assertEquals("3", server.takeRequest().getHeader("X-Api-Contract-Version"))
    }
}
