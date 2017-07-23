package com.haishinkit.net

import com.haishinkit.util.Log

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.builder.ToStringBuilder

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

abstract class Socket {
    protected var inputBuffer: ByteBuffer? = null
    private var socket: java.net.Socket? = null
    private var output: Thread? = null
    private var network: Thread? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var outputQueue: BlockingQueue<ByteBuffer>? = null

    init {
        inputBuffer = ByteBuffer.allocate(0)
        outputQueue = ArrayBlockingQueue<ByteBuffer>(128)
    }

    fun connect(dstName: String, dstPort: Int) {
        outputQueue!!.clear()
        network = object : Thread() {
            override fun run() {
                doConnection(dstName, dstPort)
            }
        }
        network!!.start()
    }

    fun close(disconnected: Boolean) {
        IOUtils.closeQuietly(socket)
        try {
            network!!.join()
        } catch (e: InterruptedException) {
            Log.w(javaClass.getName() + "#close", e.toString())
        }

    }

    fun doOutput(buffer: ByteBuffer) {
        try {
            outputQueue!!.put(buffer)
        } catch (e: InterruptedException) {
            Log.v(javaClass.getName(), e.toString())
        }

    }

    protected abstract fun onConnect()
    protected abstract fun listen(buffer: ByteBuffer)

    private fun doInput() {
        try {
            val available = inputStream!!.available()
            if (available == 0) {
                return
            }
            val buffer = ByteBuffer.allocate(inputBuffer!!.capacity() + available)
            buffer.put(inputBuffer)
            inputStream!!.read(buffer.array(), inputBuffer!!.capacity(), available)
            buffer.position(0)
            listen(buffer)
            inputBuffer = buffer.slice()
        } catch (e: IOException) {
            close(true)
        }

    }

    private fun doOutput() {
        while (socket != null && socket!!.isConnected) {
            for (buffer in outputQueue!!) {
                try {
                    buffer.flip()
                    outputStream!!.write(buffer.array())
                    outputStream!!.flush()
                    outputQueue?.remove(buffer)
                } catch (e: IOException) {
                    //IOUtils.closeQuietly(socket);
                    Log.e(javaClass.getName() + "#doOutput()", e.toString())
                }

            }
        }
    }

    private fun doConnection(dstName: String, dstPort: Int) {
        try {
            socket = java.net.Socket(dstName, dstPort)
            if (socket!!.isConnected) {
                inputStream = socket!!.getInputStream()
                outputStream = socket!!.getOutputStream()
                output = object : Thread() {
                    override fun run() {
                        doOutput()
                    }
                }
                output!!.start()
                onConnect()
            }
            while (socket != null && socket!!.isConnected) {
                doInput()
            }
        } catch (e: Exception) {
            Log.e(javaClass.getName() + "#doConnection", e.toString())
            close(true)
        }

    }

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }
}


