package pl.foodhub.pos.core.auth

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Test

class DeviceIdentityProviderSelectorTest {
    private val context = mockk<Context>(relaxed = true)
    private val fakeBuildInfo = FakeDeviceBuildInfo(manufacturer = "irrelevant-for-construction")
    private val generic = GenericDeviceIdentityProvider(context)
    private val sunmi = SunmiDeviceIdentityProvider(context, fakeBuildInfo)
    private val pax = PaxDeviceIdentityProvider(context, fakeBuildInfo)

    @Test
    fun `selects Sunmi for a manufacturer string containing sunmi, case-insensitively`() {
        val buildInfo = FakeDeviceBuildInfo(manufacturer = "SUNMI Technology")

        val selected = DeviceIdentityProviderSelector.select(buildInfo, generic, sunmi, pax)

        assertSame(sunmi, selected)
    }

    @Test
    fun `selects Pax for a manufacturer string containing pax, case-insensitively`() {
        val buildInfo = FakeDeviceBuildInfo(manufacturer = "PAX Technology")

        val selected = DeviceIdentityProviderSelector.select(buildInfo, generic, sunmi, pax)

        assertSame(pax, selected)
    }

    @Test
    fun `selects Generic for an unrecognized manufacturer`() {
        val buildInfo = FakeDeviceBuildInfo(manufacturer = "Samsung")

        val selected = DeviceIdentityProviderSelector.select(buildInfo, generic, sunmi, pax)

        assertSame(generic, selected)
    }

    @Test
    fun `selects Generic for an empty manufacturer`() {
        val buildInfo = FakeDeviceBuildInfo(manufacturer = "")

        val selected = DeviceIdentityProviderSelector.select(buildInfo, generic, sunmi, pax)

        assertSame(generic, selected)
    }
}
