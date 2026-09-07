package pl.foodhub.pos.core.auth

/**
 * Picks the [DeviceIdentityProvider] implementation for this terminal at runtime, so a
 * single APK works across a client's mixed-vendor fleet (ANDROID_POS_ARCHITECTURE.md
 * section 12 / decision D2). Matches on the manufacturer string reported by
 * [DeviceBuildInfo], falling back to the vendor-neutral [GenericDeviceIdentityProvider]
 * for any terminal not recognized as Sunmi or PAX.
 */
object DeviceIdentityProviderSelector {
    private const val SUNMI_MANUFACTURER = "sunmi"
    private const val PAX_MANUFACTURER = "pax"

    fun select(
        deviceBuildInfo: DeviceBuildInfo,
        generic: GenericDeviceIdentityProvider,
        sunmi: SunmiDeviceIdentityProvider,
        pax: PaxDeviceIdentityProvider,
    ): DeviceIdentityProvider {
        val manufacturer = deviceBuildInfo.manufacturer().lowercase()
        return when {
            manufacturer.contains(SUNMI_MANUFACTURER) -> sunmi
            manufacturer.contains(PAX_MANUFACTURER) -> pax
            else -> generic
        }
    }
}
