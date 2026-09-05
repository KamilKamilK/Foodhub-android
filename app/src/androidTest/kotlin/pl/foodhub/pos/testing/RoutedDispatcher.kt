package pl.foodhub.pos.testing

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.net.InetAddress

/**
 * Routes a [okhttp3.mockwebserver.MockWebServer] request to a canned response by method and
 * path instead of a plain FIFO queue. core:sync's `SyncWorker` and a screen's own ViewModel
 * can call the fake `foodhub-api` concurrently on different threads, so a queue that hands
 * out responses strictly in enqueue order is not reliable here -- matching on the actual
 * request is. An unmatched request gets a 404 rather than blocking the caller.
 */
class RoutedDispatcher : Dispatcher() {
    private val routes = mutableListOf<Route>()

    fun on(
        method: String,
        pathPattern: String,
        respond: (RecordedRequest) -> MockResponse,
    ) {
        routes += Route(method, Regex(pathPattern), respond)
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.requestUrl?.encodedPath ?: return MockResponse().setResponseCode(NOT_FOUND)
        val route = routes.firstOrNull { it.method == request.method && it.pathPattern.matches(path) }
        return route?.respond?.invoke(request) ?: MockResponse().setResponseCode(NOT_FOUND)
    }

    private class Route(
        val method: String,
        val pathPattern: Regex,
        val respond: (RecordedRequest) -> MockResponse,
    )

    private companion object {
        const val NOT_FOUND = 404
    }
}

fun jsonResponse(
    body: String,
    code: Int = 200,
): MockResponse =
    MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

/**
 * The address [pl.foodhub.pos.testing.di.TestNetworkModule] builds every fake `Retrofit`
 * base URL from -- calling plain `start()` lets the JVM pick IPv4 or IPv6 loopback, which
 * on some hosts disagrees with that hardcoded address and makes every call fail with a
 * connection error instead of reaching the dispatcher.
 */
fun MockWebServer.startOnLoopback() = start(InetAddress.getByName("127.0.0.1"), 0)
