package dev.ddgrcf.sidekick.core.protocol

import dev.ddgrcf.sidekick.proto.v1.Frame
import dev.ddgrcf.sidekick.proto.v1.ProtocolVersion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class RpcTimeoutException(val requestId: Long) : RuntimeException("RPC request $requestId timed out")
class RpcRemoteException(val codeName: String, override val message: String) : RuntimeException("$codeName: $message")

/**
 * Correlates monotonically increasing request Frame ids with incoming reply_to Frames.
 */
class RpcClient(
    private val timeoutMs: Long = 60_000L,
    private val send: suspend (Frame) -> Unit,
) {
    private val nextId = AtomicLong(0L)
    private val inflight = ConcurrentHashMap<Long, CompletableDeferred<Frame>>()

    suspend fun request(build: (Long) -> Frame): Frame {
        val id = nextId.incrementAndGet()
        val deferred = CompletableDeferred<Frame>()
        inflight[id] = deferred
        val frame = build(id).copy(version = ProtocolVersion.PROTOCOL_VERSION_1, id = id)
        try {
            send(frame)
            val resp = withTimeout(timeoutMs) { deferred.await() }
            resp.error?.let { err ->
                throw RpcRemoteException(err.code.name, err.message)
            }
            return resp
        } catch (t: TimeoutCancellationException) {
            throw RpcTimeoutException(id).initCause(t)
        } finally {
            inflight.remove(id)
        }
    }

    fun onFrame(frame: Frame): Boolean {
        val replyTo = frame.reply_to
        if (replyTo <= 0L) return false
        val waiter = inflight.remove(replyTo) ?: return false
        waiter.complete(frame)
        return true
    }

    fun failAll(cause: Throwable) {
        val keys = inflight.keys.toList()
        for (k in keys) {
            inflight.remove(k)?.completeExceptionally(cause)
        }
    }
}
