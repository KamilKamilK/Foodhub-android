package pl.foodhub.pos.core.auth

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The stable per-device identifier sent as `device.macAddress` to `foodhub-api`.
 *
 * TODO(D2): the backend field is named macAddress, but modern Android hides the real
 * Wi-Fi MAC (Android 6+/10+). Until a terminal model is chosen we send ANDROID_ID,
 * which is stable per app-signing-key + device. Swap for a hardware serial once the
 * device SDK is known (ANDROID_POS_ARCHITECTURE.md D2 / section 12).
 */
interface DeviceIdentityProvider {
    fun deviceId(): String

    fun model(): String

    fun osVersion(): String
}

class AndroidDeviceIdentityProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceIdentityProvider {
        @Suppress("HardwareIds")
        override fun deviceId(): String =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown-device"

        override fun model(): String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

        override fun osVersion(): String = "Android ${android.os.Build.VERSION.RELEASE}"
    }
