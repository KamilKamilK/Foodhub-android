package pl.foodhub.pos.update

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pl.foodhub.pos.BuildConfig
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.network.api.PosAppApi
import pl.foodhub.pos.core.network.model.PosAppVersionDto
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
private class TestDispatcherProvider(dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()) :
    DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}

class PosAppUpdateRepositoryTest {
    @get:Rule val tempFolder = TemporaryFolder()

    private val posAppApi = mockk<PosAppApi>()
    private lateinit var context: Context
    private lateinit var repository: PosAppUpdateRepository

    private fun withCacheDir() {
        context = mockk { every { cacheDir } returns tempFolder.newFolder("cache") }
        repository = PosAppUpdateRepository(context, posAppApi, TestDispatcherProvider())
    }

    @Test
    fun `reports the remote versionCode when it is newer than this build`() =
        runTest {
            withCacheDir()
            coEvery { posAppApi.version() } returns PosAppVersionDto(versionCode = BuildConfig.VERSION_CODE + 1)

            assertEquals(BuildConfig.VERSION_CODE + 1, repository.newerVersionAvailable())
        }

    @Test
    fun `reports no update when the remote versionCode is not newer`() =
        runTest {
            withCacheDir()
            coEvery { posAppApi.version() } returns PosAppVersionDto(versionCode = BuildConfig.VERSION_CODE)

            assertNull(repository.newerVersionAvailable())
        }

    @Test
    fun `reports no update when the version check fails`() =
        runTest {
            withCacheDir()
            coEvery { posAppApi.version() } throws IOException("offline")

            assertNull(repository.newerVersionAvailable())
        }

    @Test
    fun `downloads the apk body to the cache dir`() =
        runTest {
            withCacheDir()
            coEvery { posAppApi.apk() } returns "fake-apk-bytes".toResponseBody()

            val file = repository.downloadApk()

            assertEquals("fake-apk-bytes", file?.readText())
        }

    @Test
    fun `returns null when the apk download fails`() =
        runTest {
            withCacheDir()
            coEvery { posAppApi.apk() } throws IOException("offline")

            assertNull(repository.downloadApk())
        }
}
