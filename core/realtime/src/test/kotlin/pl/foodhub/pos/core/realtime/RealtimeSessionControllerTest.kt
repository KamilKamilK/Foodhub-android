package pl.foodhub.pos.core.realtime

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.auth.PosSession
import pl.foodhub.pos.core.auth.SessionState
import pl.foodhub.pos.core.common.DispatcherProvider

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeSessionControllerTest {
    private val context = mockk<Context>()
    private val authRepository = mockk<AuthRepository>()
    private val mercureSubscriber = mockk<MercureSubscriber>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { context.getString(R.string.foodhub_mercure_url) } returns "https://mercure.test/.well-known/mercure"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun controller() =
        RealtimeSessionController(
            context,
            authRepository,
            mercureSubscriber,
            TestDispatcherProvider(),
        )

    @Test
    fun `starts the subscriber once authenticated with a place and a mercure token`() =
        runTest {
            every { authRepository.sessionState } returns flowOf(SessionState.Authenticated)
            every { authRepository.posSession } returns
                flowOf(PosSession(placeId = "place-1", placeName = "Bistro", posId = "pos-9"))
            every { authRepository.mercureToken } returns flowOf("token-abc")

            controller().start()

            verify { mercureSubscriber.start("https://mercure.test/.well-known/mercure", "place-1", "token-abc") }
        }

    @Test
    fun `stops the subscriber when logged out`() =
        runTest {
            every { authRepository.sessionState } returns flowOf(SessionState.LoggedOut)
            every { authRepository.posSession } returns flowOf(null)
            every { authRepository.mercureToken } returns flowOf(null)

            controller().start()

            verify { mercureSubscriber.stop() }
        }

    @Test
    fun `stops the subscriber when authenticated but the place is not yet known`() =
        runTest {
            every { authRepository.sessionState } returns flowOf(SessionState.Authenticated)
            every { authRepository.posSession } returns flowOf(null)
            every { authRepository.mercureToken } returns flowOf(null)

            controller().start()

            verify { mercureSubscriber.stop() }
        }
}
