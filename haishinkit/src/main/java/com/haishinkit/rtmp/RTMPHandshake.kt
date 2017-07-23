package com.haishinkit.rtmp

import org.apache.commons.lang3.builder.ToStringBuilder

import java.nio.ByteBuffer
import com.haishinkit.util.Log;
import java.util.Random

internal class RTMPHandshake {
    var c0C1Packet: ByteBuffer = ByteBuffer.allocate(SIGNAL_SIZE + 1)
        get() {
            if (field.position() == 0) {
                val random = Random()
                field.put(0x03)
                field.position(1 + 8)
                for (i in 0 .. SIGNAL_SIZE - 9) {
                    field.put(random.nextInt().toByte())
                }
                field.flip()
            }
            return field
        }

    var c2Packet: ByteBuffer = ByteBuffer.allocate(SIGNAL_SIZE)

    private var S0S1Packet: ByteBuffer = ByteBuffer.allocate(SIGNAL_SIZE + 1)
    var s0S1Packet: ByteBuffer
        get() = S0S1Packet
        set(S0S1Packet) {
            this.S0S1Packet = ByteBuffer.wrap(S0S1Packet.array(), 0, SIGNAL_SIZE + 1)
            c2Packet.clear()
            c2Packet.put(S0S1Packet.array(), 1, 4)
            c2Packet.position(8)
            c2Packet.put(S0S1Packet.array(), 9, SIGNAL_SIZE - 8)
        }

    private var S2Packet: ByteBuffer = ByteBuffer.allocate(SIGNAL_SIZE)
    var s2Packet: ByteBuffer
        get() = S2Packet
        set(S2Packet) {
            this.S2Packet = ByteBuffer.wrap(S2Packet.array(), 0, SIGNAL_SIZE)
        }

    fun clear() {
        c0C1Packet.clear()
        S0S1Packet.clear()
        c2Packet.clear()
        S2Packet.clear()
    }

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }

    companion object {
        var SIGNAL_SIZE = 1536
    }
}
