package pl.foodhub.pos.core.auth

internal class FakeDeviceBuildInfo(
    private val manufacturer: String = "Generic",
    private val serial: String? = null,
) : DeviceBuildInfo {
    override fun manufacturer(): String = manufacturer

    override fun serialNumber(): String? = serial
}
