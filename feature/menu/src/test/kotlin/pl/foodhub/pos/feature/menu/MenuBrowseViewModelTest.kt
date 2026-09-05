package pl.foodhub.pos.feature.menu

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.foodhub.pos.core.common.ApiResult

/**
 * [MenuBrowseViewModel.state] is a `combine(...).stateIn(WhileSubscribed)` flow, so
 * each test subscribes via turbine right after construction (before the `init`-launched
 * [MenuBrowseViewModel.refresh] coroutine has run), then drains the scheduler and reads
 * [app.cash.turbine.ReceiveTurbine.expectMostRecentItem] -- the settled state, whatever
 * the exact number of intermediate emissions turned out to be (a settled value that
 * happens to equal an already-buffered one is conflated by the underlying `StateFlow`
 * and never arrives as a second item, so asserting on a fixed item count is fragile).
 *
 * [menuRepository]'s `menu` flow is backed by a [MutableStateFlow] rather than a finite
 * `flowOf(...)`: `combine` completes as soon as any one upstream flow does, and a
 * finite stand-in would make [MenuBrowseViewModel.state] terminate after its first
 * emission -- unlike the real `MenuRepository.menu`, whose Room-backed DAO flows never
 * complete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuBrowseViewModelTest {
    private val menuRepository = mockk<MenuRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { menuRepository.menu } returns MutableStateFlow(Menu(emptyList(), emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful refresh clears stale and empty-offline flags`() =
        runTest {
            coEvery { menuRepository.refresh() } returns ApiResult.Success(Unit)
            coEvery { menuRepository.hasCachedMenu() } returns true

            val viewModel = MenuBrowseViewModel(menuRepository)

            viewModel.state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertFalse(state.stale)
                assertFalse(state.emptyOffline)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `failed refresh with no cache reports the empty-offline state`() =
        runTest {
            coEvery { menuRepository.refresh() } returns ApiResult.NetworkError(RuntimeException("offline"))
            coEvery { menuRepository.hasCachedMenu() } returns false

            val viewModel = MenuBrowseViewModel(menuRepository)

            viewModel.state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertTrue(state.emptyOffline)
                assertFalse(state.stale)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `failed refresh with an existing cache serves it stale instead of the empty-offline state`() =
        runTest {
            coEvery { menuRepository.refresh() } returns ApiResult.NetworkError(RuntimeException("offline"))
            coEvery { menuRepository.hasCachedMenu() } returns true

            val viewModel = MenuBrowseViewModel(menuRepository)

            viewModel.state.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertTrue(state.stale)
                assertFalse(state.emptyOffline)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
