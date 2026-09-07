package pl.foodhub.pos.update

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class AppUpdateViewModelTest {
    private val repository = mockk<PosAppUpdateRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `surfaces an available update once the check resolves`() =
        runTest {
            coEvery { repository.newerVersionAvailable() } returns 7
            val viewModel = AppUpdateViewModel(repository)

            viewModel.state.test {
                assertEquals(AppUpdateUiState.Idle, awaitItem())

                viewModel.checkForUpdateOnce()
                assertEquals(AppUpdateUiState.Available(7), awaitItem())
            }
        }

    @Test
    fun `stays idle when no update is available`() =
        runTest {
            coEvery { repository.newerVersionAvailable() } returns null
            val viewModel = AppUpdateViewModel(repository)

            viewModel.checkForUpdateOnce()
            runCurrent()

            assertEquals(AppUpdateUiState.Idle, viewModel.state.value)
        }

    @Test
    fun `checks for an update only once per session`() =
        runTest {
            coEvery { repository.newerVersionAvailable() } returns null
            val viewModel = AppUpdateViewModel(repository)

            viewModel.checkForUpdateOnce()
            viewModel.checkForUpdateOnce()
            runCurrent()

            coVerify(exactly = 1) { repository.newerVersionAvailable() }
        }

    @Test
    fun `startUpdate moves through downloading to ready-to-install`() =
        runTest {
            val apkFile = File("/tmp/foodhub-pos-update.apk")
            coEvery { repository.downloadApk() } returns apkFile
            val viewModel = AppUpdateViewModel(repository)

            viewModel.state.test {
                assertEquals(AppUpdateUiState.Idle, awaitItem())

                viewModel.startUpdate()
                assertEquals(AppUpdateUiState.Downloading, awaitItem())
                assertEquals(AppUpdateUiState.ReadyToInstall(apkFile), awaitItem())
            }
        }

    @Test
    fun `startUpdate falls back to idle when the download fails`() =
        runTest {
            coEvery { repository.downloadApk() } returns null
            val viewModel = AppUpdateViewModel(repository)

            viewModel.state.test {
                assertEquals(AppUpdateUiState.Idle, awaitItem())

                viewModel.startUpdate()
                assertEquals(AppUpdateUiState.Downloading, awaitItem())
                assertEquals(AppUpdateUiState.Idle, awaitItem())
            }
        }

    @Test
    fun `dismiss and installRequestLaunched both return to idle`() =
        runTest {
            coEvery { repository.newerVersionAvailable() } returns 7
            val viewModel = AppUpdateViewModel(repository)

            viewModel.checkForUpdateOnce()
            runCurrent()
            assertEquals(AppUpdateUiState.Available(7), viewModel.state.value)

            viewModel.dismiss()
            assertEquals(AppUpdateUiState.Idle, viewModel.state.value)

            viewModel.installRequestLaunched()
            assertEquals(AppUpdateUiState.Idle, viewModel.state.value)
        }
}
