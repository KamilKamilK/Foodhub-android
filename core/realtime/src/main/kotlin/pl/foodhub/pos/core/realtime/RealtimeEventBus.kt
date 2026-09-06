package pl.foodhub.pos.core.realtime

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide fan-out for pokes received over Mercure. A buffer of a few slots is enough
 * -- a screen catching up after a brief background pause only needs to know that
 * *something* changed since, not how many separate pokes arrived.
 */
@Singleton
class RealtimeEventBus
    @Inject
    constructor() {
        private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 8)
        val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

        fun emit(event: RealtimeEvent) {
            _events.tryEmit(event)
        }
    }
