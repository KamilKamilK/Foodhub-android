package pl.foodhub.pos.core.fiscal

import java.net.ServerSocket
import java.net.Socket

/**
 * A real (JVM-local) TCP listener standing in for a physical fiscal printer, so
 * driver tests exercise the actual framed-read state machine in
 * [pl.foodhub.pos.core.fiscal.transport.SocketFiscalTcpConnection] end-to-end,
 * without needing hardware -- same discipline as Faza 3's ESC/POS printer
 * verification. For every incoming request it reads whatever bytes are currently
 * available (after a short settle delay) and writes back the next scripted
 * response, in order.
 */
class FakeFiscalDeviceServer(private val responses: List<ByteArray>) : AutoCloseable {
    private val serverSocket = ServerSocket(0)
    val port: Int get() = serverSocket.localPort

    private val thread =
        Thread {
            serverSocket.accept().use { socket -> serveAll(socket) }
        }.apply {
            isDaemon = true
            start()
        }

    private fun serveAll(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        for (response in responses) {
            if (!readOneRequest(input)) return
            output.write(response)
            output.flush()
        }
    }

    private fun readOneRequest(input: java.io.InputStream): Boolean {
        val first = input.read()
        if (first == -1) return false
        Thread.sleep(SETTLE_DELAY_MS)
        while (input.available() > 0) input.read()
        return true
    }

    override fun close() {
        runCatching { serverSocket.close() }
        thread.interrupt()
    }

    companion object {
        private const val SETTLE_DELAY_MS = 30L
    }
}
