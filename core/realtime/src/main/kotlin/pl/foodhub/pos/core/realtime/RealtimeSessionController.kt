package pl.foodhub.pos.core.realtime

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.auth.SessionState
import pl.foodhub.pos.core.common.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts and stops [MercureSubscriber] as the session's place/token context changes.
 * A single [start] call (from [pl.foodhub.pos.FoodHubPosApplication], mirroring how
 * `core:sync`'s [pl.foodhub.pos.core.sync.SyncWorker] scheduling is wired there) is
 * enough for the lifetime of the process -- this class then reacts on its own.
 */
@Singleton
class RealtimeSessionController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val authRepository: AuthRepository,
        private val mercureSubscriber: MercureSubscriber,
        dispatcherProvider: DispatcherProvider,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)

        fun start() {
            scope.launch {
                combine(
                    authRepository.sessionState,
                    authRepository.posSession,
                    authRepository.mercureToken,
                ) { session, posSession, mercureToken -> Triple(session, posSession, mercureToken) }
                    .distinctUntilChanged()
                    .collect { (session, posSession, mercureToken) ->
                        if (session == SessionState.Authenticated && posSession != null && mercureToken != null) {
                            mercureSubscriber.start(
                                mercureUrl = context.getString(R.string.foodhub_mercure_url),
                                placeId = posSession.placeId,
                                mercureToken = mercureToken,
                            )
                        } else {
                            mercureSubscriber.stop()
                        }
                    }
            }
        }
    }
