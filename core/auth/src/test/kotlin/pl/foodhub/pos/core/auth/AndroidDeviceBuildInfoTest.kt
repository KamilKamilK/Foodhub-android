package pl.foodhub.pos.core.auth

import android.os.Build
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AndroidDeviceBuildInfoTest {
    private val buildInfo = AndroidDeviceBuildInfo()

    @Before
    fun setUp() {
        mockkStatic(Build::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Build::class)
    }

    @Test
    fun `reports the hardware serial number`() {
        every { Build.getSerial() } returns "SN-123"

        assertEquals("SN-123", buildInfo.serialNumber())
    }

    @Test
    fun `reports null when the platform reports an unknown serial`() {
        every { Build.getSerial() } returns Build.UNKNOWN

        assertNull(buildInfo.serialNumber())
    }

    @Test
    fun `reports null when reading the serial is not permitted`() {
        every { Build.getSerial() } throws SecurityException("no READ_PHONE_STATE grant")

        assertNull(buildInfo.serialNumber())
    }
}
