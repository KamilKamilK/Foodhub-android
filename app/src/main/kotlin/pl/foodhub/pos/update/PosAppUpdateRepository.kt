package pl.foodhub.pos.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import pl.foodhub.pos.BuildConfig
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.network.api.PosAppApi
import pl.foodhub.pos.core.network.apiCall
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks `GET /v1/pos-app/version` against this build's own [BuildConfig.VERSION_CODE]
 * and, once the cashier accepts an offered update, downloads `GET /v1/pos-app/apk` to
 * the app's private cache dir for [AppUpdateHost] to hand to the system installer.
 */
@Singleton
class PosAppUpdateRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val posAppApi: PosAppApi,
        private val dispatcherProvider: DispatcherProvider,
    ) {
        /**
         * The server's versionCode, or null when this build is already current or the
         * check failed -- a failed check is a silent no-op, never a blocker for a POS
         * terminal trying to complete a sale.
         */
        suspend fun newerVersionAvailable(): Int? {
            val result = apiCall { posAppApi.version() }
            val remote = (result as? ApiResult.Success)?.value ?: return null
            return remote.versionCode.takeIf { it > BuildConfig.VERSION_CODE }
        }

        /** Downloads the APK to the cache dir, or null if the download failed. */
        suspend fun downloadApk(): File? =
            withContext(dispatcherProvider.io) {
                val result = apiCall { posAppApi.apk() }
                val body = (result as? ApiResult.Success)?.value ?: return@withContext null
                val target = File(context.cacheDir, APK_FILE_NAME)
                body.byteStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                target
            }

        private companion object {
            const val APK_FILE_NAME = "foodhub-pos-update.apk"
        }
    }
