package pl.foodhub.pos.core.realtime.di

import javax.inject.Qualifier

/**
 * The OkHttpClient used for the long-lived Mercure SSE connection: unlike
 * `core:network`'s clients, it targets a different host (the Mercure hub, not
 * foodhub-api) and needs an unbounded read timeout instead of a fixed one.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealtimeNetwork
