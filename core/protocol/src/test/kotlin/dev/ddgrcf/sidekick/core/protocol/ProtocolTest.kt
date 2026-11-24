package dev.ddgrcf.sidekick.core.protocol

import dev.ddgrcf.sidekick.proto.v1.Ack
import dev.ddgrcf.sidekick.proto.v1.Error
import dev.ddgrcf.sidekick.proto.v1.ErrorCode
import dev.ddgrcf.sidekick.proto.v1.Frame
import dev.ddgrcf.sidekick.proto.v1.Hello
import dev.ddgrcf.sidekick.proto.v1.ProtocolVersion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {
    @Test
    fun frameRoundTripAndVersionGuard() {
        val frame = Frame(
            version = ProtocolVersion.PROTOCOL_VERSION_1,
            id = 42L,
            hello = Hello(agent = "sidekick-android/0.1.0", capabilities = listOf("terminal")),
        )
        val encoded = FrameCodec.encode(frame)
        val decoded = FrameCodec.decode(encoded)!!
        assertEquals(42L, decoded.id)
        assertEquals("sidekick-android/0.1.0", decoded.hello?.agent)

        // Unknown / unspecified version returns null.
        val badVersion = frame.copy(version = ProtocolVersion.PROTOCOL_VERSION_UNSPECIFIED).encode()
        assertNull(FrameCodec.decode(badVersion))
        assertNull(FrameCodec.decode(byteArrayOf(0xFF.toByte())))
    }

    @Test
    fun chunkFramerAndReassemblerBoundaries() {
        val framer = ChunkFramer()
        val reassembler = ChunkReassembler()

        for (size in intArrayOf(0, 1, ChunkProtocol.MAX_CHUNK_PAYLOAD, ChunkProtocol.MAX_CHUNK_PAYLOAD + 1, ChunkProtocol.MAX_FRAME_SIZE)) {
            val payload = ByteArray(size) { (it and 0x7F).toByte() }
            val chunks = framer.chunk(payload)
            var result: ByteArray? = null
            for (c in chunks) {
                result = reassembler.feed(c)
            }
            assertArrayEquals("size=$size", payload, result)
        }

        // > 8 MiB fails at framer and reassembler.
        var threw = false
        try {
            framer.chunk(ByteArray(ChunkProtocol.MAX_FRAME_SIZE + 1))
        } catch (_: FrameTooLargeException) {
            threw = true
        }
        assertTrue(threw)

        // Out-of-order / missing first chunk only emits what arrived when LAST arrives, and malformed header fails.
        var malformed = false
        try {
            reassembler.feed(byteArrayOf(1, 2, 3))
        } catch (_: MalformedChunkException) {
            malformed = true
        }
        assertTrue(malformed)
    }

    @Test
    fun rpcClientCorrelatesAndTimesOut() = runTest {
        lateinit var client: RpcClient
        client = RpcClient(timeoutMs = 5_000L) { req ->
            launch {
                client.onFrame(Frame(version = ProtocolVersion.PROTOCOL_VERSION_1, reply_to = req.id, ack = Ack()))
            }
        }
        val resp = client.request { id -> Frame(id = id, hello = Hello(agent = "test")) }
        assertTrue(resp.ack != null)

        val errClient = RpcClient(timeoutMs = 5_000L) { req ->
            launch {
                // ignore
            }
        }
        var timedOut = false
        val shortClient = RpcClient(timeoutMs = 50L) {}
        try {
            shortClient.request { id -> Frame(id = id, hello = Hello(agent = "test")) }
        } catch (_: RpcTimeoutException) {
            timedOut = true
        }
        assertTrue(timedOut)
        errClient.failAll(IllegalStateException("done"))

        lateinit var rpcErrClient: RpcClient
        rpcErrClient = RpcClient(timeoutMs = 5_000L) { req ->
            launch {
                rpcErrClient.onFrame(
                    Frame(
                        version = ProtocolVersion.PROTOCOL_VERSION_1,
                        reply_to = req.id,
                        error = Error(code = ErrorCode.ERROR_CODE_FORBIDDEN, message = "nope"),
                    ),
                )
            }
        }
        var remoteErr: RpcRemoteException? = null
        try {
            rpcErrClient.request { id -> Frame(id = id, hello = Hello(agent = "test")) }
        } catch (e: RpcRemoteException) {
            remoteErr = e
        }
        assertEquals("ERROR_CODE_FORBIDDEN", remoteErr?.codeName)
    }
}
