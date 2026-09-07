package pl.foodhub.pos.core.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads the standard Android hardware-serial API ([DeviceBuildInfo.serialNumber]) as the
 * device identifier on PAX terminals, falling back to the same ANDROID_ID logic as
 * [GenericDeviceIdentityProvider] when the serial is unavailable (no READ_PHONE_STATE
 * grant, or the OEM image doesn't expose one).
 *
 * This reads the standard Android serial API, not PAX's own NeptuneLite/DAL SDK — we
 * don't have a PAX developer account or its proprietary `.aar` yet. Swap [deviceId] for
 * the real PAX DAL API call once that dependency is in hand.
 */
class PaxDeviceIdentityProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val deviceBuildInfo: DeviceBuildInfo,
    ) : DeviceIdentityProvider {
        override fun deviceId(): String =
            deviceBuildInfo.serialNumber()?.takeIf { it.isNotBlank() } ?: androidId(context)

        override fun model(): String = defaultModel()

        override fun osVersion(): String = defaultOsVersion()
    }
