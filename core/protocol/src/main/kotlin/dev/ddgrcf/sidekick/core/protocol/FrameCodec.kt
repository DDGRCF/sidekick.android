package dev.ddgrcf.sidekick.core.protocol

import dev.ddgrcf.sidekick.proto.v1.Frame
import dev.ddgrcf.sidekick.proto.v1.ProtocolVersion
import java.io.IOException

object FrameCodec {
    fun encode(frame: Frame): ByteArray {
        val normalized = if (frame.version == ProtocolVersion.PROTOCOL_VERSION_UNSPECIFIED) {
            frame.copy(version = ProtocolVersion.PROTOCOL_VERSION_1)
        } else {
            frame
        }
        return normalized.encode()
    }

    /**
     * Decodes a protobuf Frame. Returns null for unknown/unsupported protocol versions
     * or malformed frames per remote-sync-android.md §3.
     */
    fun decode(bytes: ByteArray): Frame? {
        val frame = try {
            Frame.ADAPTER.decode(bytes)
        } catch (_: IOException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (frame.version != ProtocolVersion.PROTOCOL_VERSION_1) {
            return null
        }
        return frame
    }
}
