package pl.foodhub.pos.core.auth

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SunmiDeviceIdentityProviderTest {
    private val fakeContentResolver = mockk<ContentResolver>()
    private val context =
        mockk<Context> {
            every { contentResolver } returns fakeContentResolver
        }

    @Before
    fun setUp() {
        mockkStatic(Settings.Secure::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
    }

    @Test
    fun `reports the hardware serial when available`() {
        val buildInfo = FakeDeviceBuildInfo(manufacturer = "Sunmi", serial = "SN-SUNMI-1")
        val provider = SunmiDeviceIdentityProvider(context, buildInfo)

        assertEquals("SN-SUNMI-1", provider.deviceId())
    }

    @Test
    fun `falls back to ANDROID_ID when the serial is null`() {
        every { Settings.Secure.getString(fakeContentResolver, Settings.Secure.ANDROID_ID) } returns "android-id-42"
        val buildInfo = FakeDeviceBuildInfo(manufacturer = "Sunmi", serial = null)
        val provider = SunmiDeviceIdentityProvider(context, buildInfo)

        assertEquals("android-id-42", provider.deviceId())
    }

    @Test
    fun `falls back to ANDROID_ID when the serial is blank`() {
        every { Settings.Secure.getString(fakeContentResolver, Settings.Secure.ANDROID_ID) } returns "android-id-42"
        val buildInfo = FakeDeviceBuildInfo(manufacturer = "Sunmi", serial = "   ")
        val provider = SunmiDeviceIdentityProvider(context, buildInfo)

        assertEquals("android-id-42", provider.deviceId())
    }
}
