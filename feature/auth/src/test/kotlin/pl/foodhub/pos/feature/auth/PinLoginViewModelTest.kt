package pl.foodhub.pos.feature.auth

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.foodhub.pos.core.auth.AuthRepository
import pl.foodhub.pos.core.common.ApiResult

class PinLoginViewModelTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit is blocked until the pin is long enough`() {
        val viewModel = PinLoginViewModel(authRepository)

        viewModel.onPinChange("12")
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onPinChange("1234")
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `a rejected pin surfaces INVALID_PIN and clears the field`() =
        runTest {
            coEvery { authRepository.loginWithPin(any(), any()) } returns
                ApiResult.HttpError(status = 401, errorCode = null, message = null)
            val viewModel = PinLoginViewModel(authRepository)

            viewModel.state.test {
                assertEquals(PinLoginUiState(), awaitItem())

                viewModel.onPinChange("1234")
                assertEquals("1234", awaitItem().pin)

                viewModel.submit()
                assertTrue(awaitItem().submitting)

                val failed = awaitItem()
                assertEquals(PinLoginError.INVALID_PIN, failed.error)
                assertEquals("", failed.pin)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a successful login flips loggedIn`() =
        runTest {
            coEvery { authRepository.loginWithPin(any(), any()) } returns ApiResult.Success(Unit)
            val viewModel = PinLoginViewModel(authRepository)

            viewModel.onPinChange("4321")
            viewModel.submit()
            runCurrent()

            assertTrue(viewModel.state.value.loggedIn)
            assertFalse(viewModel.state.value.submitting)
        }
}
