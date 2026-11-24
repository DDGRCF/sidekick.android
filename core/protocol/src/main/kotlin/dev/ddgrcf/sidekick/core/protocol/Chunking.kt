package dev.ddgrcf.sidekick.core.protocol

import java.util.concurrent.atomic.AtomicLong

object ChunkProtocol {
    const val HEADER_LEN = 9
    const val MAX_CHUNK_PAYLOAD = 60_000
    const val MAX_FRAME_SIZE = 8 * 1024 * 1024
    const val FLAG_LAST: Byte = 0x01
}

class FrameTooLargeException(message: String) : IllegalArgumentException(message)
class MalformedChunkException(message: String) : IllegalArgumentException(message)

/**
 * Splits serialized Frame bytes into <= 60,000-byte L2 chunks with 9-byte headers:
 * u48 msg_id | u8 flags (bit0 = LAST) | u16 chunk_len.
 */
class ChunkFramer(initialMsgId: Long = 0L) {
    private val nextId = AtomicLong(initialMsgId)

    fun chunk(data: ByteArray): List<ByteArray> {
        if (data.size > ChunkProtocol.MAX_FRAME_SIZE) {
            throw FrameTooLargeException("frame size ${data.size} exceeds 8 MiB limit")
        }
        val msgId = nextId.incrementAndGet()
        if (data.isEmpty()) {
            val pkt = ByteArray(ChunkProtocol.HEADER_LEN)
            encodeHeader(pkt, msgId, ChunkProtocol.FLAG_LAST, 0)
            return listOf(pkt)
        }
        val out = ArrayList<ByteArray>((data.size + ChunkProtocol.MAX_CHUNK_PAYLOAD - 1) / ChunkProtocol.MAX_CHUNK_PAYLOAD)
        var offset = 0
        while (offset < data.size) {
            val end = minOf(offset + ChunkProtocol.MAX_CHUNK_PAYLOAD, data.size)
            val len = end - offset
            val flags = if (end == data.size) ChunkProtocol.FLAG_LAST else 0
            val pkt = ByteArray(ChunkProtocol.HEADER_LEN + len)
            encodeHeader(pkt, msgId, flags, len)
            System.arraycopy(data, offset, pkt, ChunkProtocol.HEADER_LEN, len)
            out.add(pkt)
            offset = end
        }
        return out
    }

    companion object {
        internal fun encodeHeader(dst: ByteArray, msgId: Long, flags: Byte, len: Int) {
            dst[0] = ((msgId ushr 40) and 0xFF).toByte()
            dst[1] = ((msgId ushr 32) and 0xFF).toByte()
            dst[2] = ((msgId ushr 24) and 0xFF).toByte()
            dst[3] = ((msgId ushr 16) and 0xFF).toByte()
            dst[4] = ((msgId ushr 8) and 0xFF).toByte()
            dst[5] = (msgId and 0xFF).toByte()
            dst[6] = flags
            dst[7] = ((len ushr 8) and 0xFF).toByte()
            dst[8] = (len and 0xFF).toByte()
        }
    }
}

/**
 * Reassembles decrypted L2 chunks into complete serialized L3 Frame payloads,
 * enforcing the 8 MiB per-frame limit.
 */
class ChunkReassembler {
    private val pending = LinkedHashMap<Long, MutableList<ByteArray>>()
    private val sizes = HashMap<Long, Int>()

    @Synchronized
    fun feed(chunk: ByteArray): ByteArray? {
        if (chunk.size < ChunkProtocol.HEADER_LEN) {
            throw MalformedChunkException("chunk shorter than header: ${chunk.size}")
        }
        val msgId = ((chunk[0].toLong() and 0xFF) shl 40) or
            ((chunk[1].toLong() and 0xFF) shl 32) or
            ((chunk[2].toLong() and 0xFF) shl 24) or
            ((chunk[3].toLong() and 0xFF) shl 16) or
            ((chunk[4].toLong() and 0xFF) shl 8) or
            (chunk[5].toLong() and 0xFF)
        val flags = chunk[6]
        val len = ((chunk[7].toInt() and 0xFF) shl 8) or (chunk[8].toInt() and 0xFF)
        if (len > ChunkProtocol.MAX_CHUNK_PAYLOAD || ChunkProtocol.HEADER_LEN + len != chunk.size) {
            throw MalformedChunkException("chunk len mismatch: header=$len actual=${chunk.size - ChunkProtocol.HEADER_LEN}")
        }

        if (!pending.containsKey(msgId) && pending.size >= 64) {
            val oldest = pending.keys.first()
            pending.remove(oldest)
            sizes.remove(oldest)
        }

        val curSize = sizes[msgId] ?: 0
        val newSize = curSize + len
        if (newSize > ChunkProtocol.MAX_FRAME_SIZE) {
            pending.remove(msgId)
            sizes.remove(msgId)
            throw FrameTooLargeException("reassembled frame exceeds 8 MiB limit")
        }

        val payload = chunk.copyOfRange(ChunkProtocol.HEADER_LEN, chunk.size)
        val list = pending.getOrPut(msgId) { ArrayList() }
        list.add(payload)
        sizes[msgId] = newSize

        if ((flags.toInt() and ChunkProtocol.FLAG_LAST.toInt()) != 0) {
            pending.remove(msgId)
            sizes.remove(msgId)
            val result = ByteArray(newSize)
            var pos = 0
            for (part in list) {
                System.arraycopy(part, 0, result, pos, part.size)
                pos += part.size
            }
            return result
        }
        return null
    }
}
