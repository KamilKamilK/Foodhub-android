package pl.foodhub.pos.core.fiscal.transport

import kotlinx.coroutines.withContext
import pl.foodhub.pos.core.common.DispatcherProvider
import java.io.Closeable
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject

private const val CONNECT_TIMEOUT_MS = 5_000
private const val READ_POLL_TIMEOUT_MS = 200

/**
 * A held-open, request/response socket to a fiscal printer. Unlike
 * [pl.foodhub.pos.core.printing.LanPrinterClient] (write-only, one shot per ticket), a
 * fiscal receipt needs several round trips over one connection (begin -> N lines -> N
 * payments -> commit -> status poll), so the connection is opened once per
 * fiscalization attempt and each command is a real, framed read -- not a fixed delay
 * followed by a blind read, which cannot tell a slow device from a truncated response.
 */
interface FiscalTcpConnection : Closeable {
    /**
     * Writes [frame], then reads bytes until [endMarker] is seen (inclusive) or
     * [timeoutMs] elapses with no further bytes arriving. For framed commands
     * (Novitus's `<ESC>P ... <ESC>\`, Posnet's `<STX> ... <ETX>`).
     */
    suspend fun exchange(
        frame: ByteArray,
        endMarker: Byte,
        timeoutMs: Long,
    ): Result<ByteArray>

    /**
     * Writes [frame], then reads exactly [expectedLength] bytes or times out. For a
     * short, fixed-format handshake reply with no frame envelope of its own (e.g. a
     * raw ENQ/DLE status probe).
     */
    suspend fun exchangeFixedLength(
        frame: ByteArray,
        expectedLength: Int,
        timeoutMs: Long,
    ): Result<ByteArray>
}

class SocketFiscalTcpConnection(
    private val socket: Socket,
    private val dispatchers: DispatcherProvider,
) : FiscalTcpConnection {
    override suspend fun exchange(
        frame: ByteArray,
        endMarker: Byte,
        timeoutMs: Long,
    ): Result<ByteArray> =
        withContext(dispatchers.io) {
            try {
                writeFrame(frame)
                Result.success(readUntil(endMarker, timeoutMs))
            } catch (e: IOException) {
                Result.failure(e)
            }
        }

    override suspend fun exchangeFixedLength(
        frame: ByteArray,
        expectedLength: Int,
        timeoutMs: Long,
    ): Result<ByteArray> =
        withContext(dispatchers.io) {
            try {
                writeFrame(frame)
                Result.success(readExactly(expectedLength, timeoutMs))
            } catch (e: IOException) {
                Result.failure(e)
            }
        }

    private fun writeFrame(frame: ByteArray) {
        val output = socket.getOutputStream()
        output.write(frame)
        output.flush()
    }

    private fun readExactly(
        length: Int,
        timeoutMs: Long,
    ): ByteArray {
        val input = socket.getInputStream()
        val buffer = ByteArray(length)
        var read = 0
        val deadline = System.currentTimeMillis() + timeoutMs
        socket.soTimeout = READ_POLL_TIMEOUT_MS

        while (read < length && System.currentTimeMillis() < deadline) {
            // A per-read poll timeout, not a device error -- keep polling until the
            // overall deadline (checked above), so the exception carries nothing to act on.
            @Suppress("SwallowedException")
            val count =
                try {
                    input.read(buffer, read, length - read)
                } catch (e: SocketTimeoutException) {
                    continue
                }
            if (count == -1) throw IOException("Fiscal device closed the connection.")
            read += count
        }

        if (read < length) throw SocketTimeoutException("Timed out waiting for fiscal device response.")
        return buffer
    }

    private fun readUntil(
        endMarker: Byte,
        timeoutMs: Long,
    ): ByteArray {
        val input = socket.getInputStream()
        val buffer = mutableListOf<Byte>()
        val deadline = System.currentTimeMillis() + timeoutMs
        socket.soTimeout = READ_POLL_TIMEOUT_MS

        while (System.currentTimeMillis() < deadline) {
            // A per-byte poll timeout, not a device error -- keep polling until the
            // overall deadline (checked above), so the exception carries nothing to act on.
            @Suppress("SwallowedException")
            val next =
                try {
                    input.read()
                } catch (e: SocketTimeoutException) {
                    continue
                }
            if (next == -1) throw IOException("Fiscal device closed the connection.")

            buffer.add(next.toByte())
            if (next.toByte() == endMarker) return buffer.toByteArray()
        }

        throw SocketTimeoutException("Timed out waiting for fiscal device response.")
    }

    override fun close() {
        socket.close()
    }
}

class FiscalTcpConnectionFactory
    @Inject
    constructor(private val dispatchers: DispatcherProvider) {
        suspend fun connect(
            ip: String,
            port: Int,
            connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
        ): Result<FiscalTcpConnection> =
            withContext(dispatchers.io) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(ip, port), connectTimeoutMs)
                    Result.success(SocketFiscalTcpConnection(socket, dispatchers))
                } catch (e: IOException) {
                    Result.failure(e)
                }
            }
    }
