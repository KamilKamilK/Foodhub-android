package pl.foodhub.pos.core.printing

import kotlinx.coroutines.withContext
import pl.foodhub.pos.core.common.DispatcherProvider
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

private const val CONNECT_TIMEOUT_MS = 5_000

/**
 * Sends raw ESC/POS bytes to a receipt/kitchen printer over the local network --
 * simpler than routing through the backend and works with no internet connectivity
 * (ANDROID_POS_ARCHITECTURE.md section 7.1).
 */
class LanPrinterClient
    @Inject
    constructor(private val dispatchers: DispatcherProvider) {
        suspend fun send(
            ip: String,
            port: Int,
            bytes: ByteArray,
        ): Result<Unit> =
            withContext(dispatchers.io) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
                        val output = socket.getOutputStream()
                        output.write(bytes)
                        output.flush()
                    }
                    Result.success(Unit)
                } catch (e: IOException) {
                    Result.failure(e)
                }
            }
    }
