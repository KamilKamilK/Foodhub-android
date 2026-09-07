package pl.foodhub.pos.core.auth

import android.os.Build
import javax.inject.Inject

/**
 * Indirection over the static [Build] facts the device-identity providers need, so they
 * can be unit-tested without touching raw `android.os.Build` statics. Bound to the real
 * implementation by a Hilt module in core:auth.
 */
interface DeviceBuildInfo {
    fun manufacturer(): String

    fun serialNumber(): String?
}

class AndroidDeviceBuildInfo
    @Inject
    constructor() : DeviceBuildInfo {
        override fun manufacturer(): String = Build.MANUFACTURER

        override fun serialNumber(): String? =
            try {
                Build.getSerial().takeUnless { it == Build.UNKNOWN }
            } catch (ignored: SecurityException) {
                null
            }
    }
