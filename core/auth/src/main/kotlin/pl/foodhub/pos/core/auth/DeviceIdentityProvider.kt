package pl.foodhub.pos.core.auth

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The stable per-device identifier sent as `device.macAddress` to `foodhub-api`.
 *
 * The concrete implementation is chosen at runtime by [DeviceIdentityProviderSelector]
 * based on the terminal manufacturer, since a single APK must run across a client's
 * mixed-vendor fleet (ANDROID_POS_ARCHITECTURE.md section 12 / decision D2).
 */
interface DeviceIdentityProvider {
    fun deviceId(): String

    fun model(): String

    fun osVersion(): String
}

/**
 * Reads the standard Android identifiers available on any GMS-certified device, with no
 * assumption about the terminal vendor. Used both as the provider for terminals not
 * recognized as Sunmi or PAX, and as the ANDROID_ID fallback inside those vendor-specific
 * providers when a hardware serial isn't available.
 */
class GenericDeviceIdentityProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceIdentityProvider {
        override fun deviceId(): String = androidId(context)

        override fun model(): String = defaultModel()

        override fun osVersion(): String = defaultOsVersion()
    }

@Suppress("HardwareIds")
internal fun androidId(context: Context): String =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"

internal fun defaultModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

internal fun defaultOsVersion(): String = "Android ${Build.VERSION.RELEASE}"
