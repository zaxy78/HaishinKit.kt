package com.haishinkit.rtmp

import java.util.ArrayList
import java.nio.ByteBuffer

import com.haishinkit.rtmp.messages.RTMPMessage

enum class RTMPChunk(val rawValue: Byte) {
    ZERO(0x00),
    ONE(0x01),
    TWO(0x02),
    THREE(0x03);

    fun encode(socket: RTMPSocket?, message: RTMPMessage?): List<ByteBuffer> {
        if (socket == null || message == null) {
            throw IllegalArgumentException()
        }

        val payload = message.encode(socket)
        payload.flip()

        val list = ArrayList<ByteBuffer>()
        val length = payload.limit()
        val timestamp = message.timestamp
        val chunkSize = socket.chunkSizeS
        var buffer = ByteBuffer.allocate(length(message.chunkStreamID) + if (length < chunkSize) length else chunkSize)
        message.length = length

        buffer.put(header(message.chunkStreamID))
        buffer.put(byteArrayOf((timestamp shr 16).toByte(), (timestamp shr 8).toByte(), timestamp.toByte()))

        when (this) {
            ZERO -> {
                buffer.put(byteArrayOf((length shr 16).toByte(), (length shr 8).toByte(), length.toByte()))
                buffer.put(message.type.rawValue)
                val streamID = message.streamID
                // message streamID is a litleEndian
                buffer.put(byteArrayOf(streamID.toByte(), (streamID shr 8).toByte(), (streamID shr 16).toByte(), (streamID shr 24).toByte()))
            }
            ONE -> {
                buffer.put(byteArrayOf((length shr 16).toByte(), (length shr 8).toByte(), length.toByte()))
                buffer.put(message.type.rawValue)
            }
            else -> {
            }
        }

        if (length < chunkSize) {
            buffer.put(payload.array(), 0, length)
            buffer.flip()
            list.add(buffer)
            return list
        }

        val mod = length % chunkSize
        val three = RTMPChunk.THREE.header(message.chunkStreamID)
        buffer.put(payload.array(), 0, chunkSize)
        list.add(buffer)
        for (i in 1..(length - mod) / chunkSize - 1) {
            buffer = ByteBuffer.allocate(three.size + chunkSize)
            buffer.put(three)
            buffer.put(payload.array(), chunkSize * i, chunkSize)
            list.add(buffer)
        }
        buffer = ByteBuffer.allocate(three.size + mod)
        buffer.put(three)
        buffer.put(payload.array(), length - mod, mod)
        list.add(buffer)

        return list
    }

    fun decode(chunkStreamID: Short, connection: RTMPConnection?, buffer: ByteBuffer?): RTMPMessage {
        if (connection == null || buffer == null) {
            throw IllegalArgumentException()
        }

        var timestamp = 0
        var length = 0
        var type: Byte = 0
        var streamID = 0

        when (this) {
            ZERO -> {
                timestamp = getInt(buffer)
                length = getInt(buffer)
                type = buffer.get()
                streamID = Integer.reverseBytes(buffer.int)
            }
            ONE -> {
                timestamp = getInt(buffer)
                length = getInt(buffer)
                type = buffer.get()
            }
            TWO -> {
                var message = connection.messages[chunkStreamID]!!
                message.timestamp = getInt(buffer)
                return message
            }
            else -> {
            }
        }

        var message = RTMPMessage.create(type)
        message.chunkStreamID = chunkStreamID
        message.streamID = streamID
        message.timestamp = timestamp
        message.length = length

        return message
    }

    fun length(streamID: Short): Int {
        var basic = 3
        if (streamID <= 63) {
            basic = 1
        } else if (streamID <= 319) {
            basic = 2
        }
        when (this) {
            ZERO -> return basic + 11
            ONE -> return basic + 7
            TWO -> return basic + 3
            THREE -> return basic + 0
            else -> return 0
        }
    }

    fun header(streamID: Short): ByteArray {
        if (streamID <= 63) {
            return byteArrayOf((rawValue.toInt() shl 6 or streamID.toInt()).toByte())
        }
        if (streamID <= 319) {
            return byteArrayOf((rawValue.toInt() shl 6 or 0).toByte(), (streamID - 64).toByte())
        }
        return byteArrayOf((rawValue.toInt() shl 6 or 63).toByte(), (streamID - 64 shr 8).toByte(), (streamID - 64).toByte())
    }

    fun getInt(buffer: ByteBuffer): Int {
        val bytes = ByteArray(3)
        buffer.get(bytes)
        return (bytes[0].toInt() and 0xFF).toInt() shl 16 or ((bytes[1].toInt() and 0xFF).toInt() shl 8) or (bytes[2].toInt() and 0xFF).toInt()
    }

    fun getStreamID(buffer: ByteBuffer): Short {
        buffer.position(buffer.position() - 1)
        val first = buffer.get()
        when (first.toInt() and 63) {
            0 -> {
                var bytes = ByteArray(2)
                buffer.get(bytes)
                return (bytes[1] + 64).toShort()
            }
            1 -> {
                var bytes = ByteArray(3)
                buffer.get(bytes)
                return (bytes[1].toInt() shl 8 or bytes[2].toInt() or 64.toInt()).toShort()
            }
            else -> return (first.toInt() and 63).toShort()
        }
    }

    companion object {
        val CONTROL: Short = 0x02
        val COMMAND: Short = 0x03
        val AUDIO: Short = 0x04
        val VIDEO: Short = 0x05
        val DEFAULT_SIZE = 128
    }
}
