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

class GenericDeviceIdentityProviderTest {
    private val fakeContentResolver = mockk<ContentResolver>()
    private val context =
        mockk<Context> {
            every { contentResolver } returns fakeContentResolver
        }
    private val provider = GenericDeviceIdentityProvider(context)

    @Before
    fun setUp() {
        mockkStatic(Settings.Secure::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
    }

    @Test
    fun `reports the ANDROID_ID as the device id`() {
        every { Settings.Secure.getString(fakeContentResolver, Settings.Secure.ANDROID_ID) } returns "android-id-42"

        assertEquals("android-id-42", provider.deviceId())
    }

    @Test
    fun `falls back to a placeholder when ANDROID_ID is unavailable`() {
        every { Settings.Secure.getString(fakeContentResolver, Settings.Secure.ANDROID_ID) } returns null

        assertEquals("unknown-device", provider.deviceId())
    }
}
