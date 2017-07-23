package com.haishinkit.media

import android.media.MediaCodec

import com.haishinkit.util.Log
import java.io.IOException
import java.lang.Runnable
import java.util.concurrent.atomic.AtomicBoolean

abstract class EncoderBase(private val mime: String) : IEncoder, Runnable {
    private var dequeue: Thread? = null
    private var codec: MediaCodec? = null
    private val running = AtomicBoolean(false)
    override var listener: IEncoderListener? = null

    override val isRunning: Boolean
        get() = running.get()

    override fun startRunning() {
        synchronized(this) {
            if (running.get()) {
                return
            }
            try {
                dequeue = Thread(this)
                codec = createMediaCodec()
                codec!!.start()
                running.set(true)
                dequeue!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun stopRunning() {
        synchronized(this) {
            if (!running.get()) {
                return
            }
            codec!!.stop()
            codec!!.release()
            codec = null
            running.set(false)
            dequeue = null
        }
    }

    @Synchronized override fun encodeBytes(data: ByteArray, presentationTimeUs: Long) {
        if (!running.get()) {
            return
        }
        try {
            val inputBuffers = codec!!.inputBuffers
            val inputBufferIndex = codec!!.dequeueInputBuffer(-1)
            if (0 <= inputBufferIndex) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                inputBuffer.clear()
                inputBuffer.put(data)
                codec!!.queueInputBuffer(inputBufferIndex, 0, data.size, presentationTimeUs, 0)
            }
        } catch (e: Exception) {
            Log.w(javaClass.getName(), e.toString())
        }

    }

    override fun run() {
        var outputBuffers = codec!!.outputBuffers
        while (running.get()) {
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = codec!!.dequeueOutputBuffer(bufferInfo, -1)
            when (outputBufferIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d(javaClass.getName(), "INFO_OUTPUT_FORMAT_CHANGED")
                    if (listener != null) {
                        listener!!.onFormatChanged(mime, codec!!.outputFormat)
                    }
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d(javaClass.getName(), "INFO_TRY_AGAIN_LATER")
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    Log.d(javaClass.getName(), "OUTPUT_BUFFERS_CHANGED")
                    outputBuffers = codec!!.outputBuffers
                }
                else -> if (0 <= outputBufferIndex) {
                    val outData = ByteArray(bufferInfo.size)
                    val outputBuffer = outputBuffers[outputBufferIndex]
                    outputBuffer.get(outData)
                    outputBuffer.flip()
                    if (listener != null) {
                        listener!!.onSampleOutput(mime, bufferInfo, outputBuffer)
                    }
                    codec!!.releaseOutputBuffer(outputBufferIndex, false)
                }
            }
        }
    }

    @Throws(IOException::class)
    protected abstract fun createMediaCodec(): MediaCodec
}

