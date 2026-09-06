package pl.foodhub.pos.core.realtime

import com.launchdarkly.eventsource.ConnectStrategy
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import com.launchdarkly.eventsource.background.BackgroundEventHandler
import com.launchdarkly.eventsource.background.BackgroundEventSource
import okhttp3.OkHttpClient
import pl.foodhub.pos.core.realtime.di.RealtimeNetwork
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the terminal's single subscription to `places/{placeId}/pos-state`
 * (ANDROID_POS_ARCHITECTURE.md section 4/14 Faza 4). [start] replaces any existing
 * subscription; [stop] is safe to call even when nothing is running.
 */
@Singleton
class MercureSubscriber
    @Inject
    constructor(
        @RealtimeNetwork private val httpClient: OkHttpClient,
        private val eventBus: RealtimeEventBus,
    ) {
        private var backgroundEventSource: BackgroundEventSource? = null

        fun start(
            mercureUrl: String,
            placeId: String,
            mercureToken: String,
        ) {
            stop()

            val topic = URLEncoder.encode("places/$placeId/pos-state", "UTF-8")
            val uri = java.net.URI.create("$mercureUrl?topic=$topic")

            val connectStrategy =
                ConnectStrategy.http(uri)
                    .header("Authorization", "Bearer $mercureToken")
                    .httpClient(httpClient)

            val handler =
                object : BackgroundEventHandler {
                    override fun onOpen() = Unit

                    override fun onClosed() = Unit

                    override fun onComment(comment: String) = Unit

                    override fun onError(t: Throwable) = Unit

                    override fun onMessage(
                        event: String,
                        messageEvent: MessageEvent,
                    ) {
                        parsePosStatePoke(messageEvent.data)?.let(eventBus::emit)
                    }
                }

            backgroundEventSource =
                BackgroundEventSource.Builder(handler, EventSource.Builder(connectStrategy))
                    .build()
                    .also { it.start() }
        }

        fun stop() {
            backgroundEventSource?.close()
            backgroundEventSource = null
        }
    }
