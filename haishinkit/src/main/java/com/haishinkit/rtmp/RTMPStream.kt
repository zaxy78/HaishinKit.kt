package com.haishinkit.rtmp

import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.AudioRecord

import com.haishinkit.events.Event
import com.haishinkit.events.EventDispatcher
import com.haishinkit.events.IEventListener
import com.haishinkit.media.AACEncoder
import com.haishinkit.media.AudioInfo
import com.haishinkit.media.H264Encoder
import com.haishinkit.media.IEncoder
import com.haishinkit.rtmp.messages.RTMPCommandMessage
import com.haishinkit.rtmp.messages.RTMPDataMessage
import com.haishinkit.rtmp.messages.RTMPMessage
import com.haishinkit.util.EventUtils

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ToStringBuilder

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

class RTMPStream(connection: RTMPConnection) : EventDispatcher(null) {

    enum class HowToPublish(val rawValue: String) {
        RECORD("record"),
        APPEND("append"),
        APPEND_WITH_GAP("appendWithGap"),
        LIVE("live");
    }

    enum class Codes(val rawValue: String, val level: String) {
        BUFFER_EMPTY("NetStream.Buffer.Empty", "status"),
        BUFFER_FLUSH("NetStream.Buffer.Flush", "status"),
        BUFFER_FULL("NetStream.Buffer.Full", "status"),
        CONNECT_CLOSED("NetStream.Connect.Closed", "status"),
        CONNECT_FAILED("NetStream.Connect.Failed", "error"),
        CONNECT_REJECTED("NetStream.Connect.Rejected", "error"),
        CONNECT_SUCCESS("NetStream.Connect.Success", "status"),
        DRM_UPDATE_NEEDED("NetStream.DRM.UpdateNeeded", "status"),
        FAILED("NetStream.Failed", "error"),
        MULTI_STREAM_RESET("NetStream.MulticastStream.Reset", "status"),
        PAUSE_NOTIFY("NetStream.Pause.Notify", "status"),
        PLAY_FAILED("NetStream.Play.Failed", "error"),
        PLAY_FILE_STRUCTURE_INVALID("NetStream.Play.FileStructureInvalid", "error"),
        PLAY_INSUFFICIENT_BW("NetStream.Play.InsufficientBW", "status"),
        PLAY_NO_SUPPORTED_TRACK_FOUND("NetStream.Play.NoSupportedTrackFound", "status"),
        PLAY_REST("NetStream.Play.Reset", "status"),
        PLAY_START("NetStream.Play.Start", "status"),
        PLAY_STOP("NetStream.Play.Stop", "status"),
        PLAY_STREAM_NOT_FOUND("NetStream.Play.StreamNotFound", "error"),
        PLAY_TRANSITION("NetStream.Play.Transition", "status"),
        PLAY_UNPUBLISH_NOTIFY("NetStream.Play.UnpublishNotify", "status"),
        PUBLISH_BAD_NAME("NetStream.Publish.BadName", "status"),
        PUBLISH_IDLE("NetStream.Publish.Idle", "status"),
        PUBLISH_START("NetStream.Publish.Start", "status"),
        RECORD_ALREADY_EXISTS("NetStream.Record.AlreadyExists", "status"),
        RECORD_FAILED("NetStream.Record.Failed", "status"),
        RECORD_NO_ACCESS("NetStream.Record.NoAccess", "error"),
        RECORD_START("NetStream.Record.Start", "status"),
        RECORD_STOP("NetStream.Record.Stop", "status"),
        RECORD_DISK_QUOTA_EXCEEDED("NetStream.Record.DiskQuotaExceeded", "error"),
        SECOND_SCREEN_START("NetStream.SecondScreen.Start", "status"),
        SECOND_SCREEN_STOP("NetStream.SecondScreen.Stop", "status"),
        SEEK_FAILDED("NetStream.Seek.Failed", "error"),
        SEEK_INVALID_TIME("NetStream.Seek.InvalidTime", "error"),
        SEEK_NOTIFY("NetStream.Seek.Notify", "status"),
        STEP_NOTIFY("NetStream.Step.Notify", "status"),
        UNPAUSE_NOTIFY("NetStream.Unpause.Notify", "status"),
        UNPUBLISH_SUCCESS("NetStream.Unpublish.Success", "status"),
        VIDEO_DIMENSION_CHANGE("NetStream.Video.DimensionChange", "status");

        fun data(description: String): Map<String, Any> {
            val data = HashMap<String, Any>()
            data.put("code", rawValue)
            data.put("level", level)
            if (StringUtils.isNoneEmpty(description)) {
                data.put("description", description)
            }
            return data
        }
    }

    inner class EventListener internal constructor(private val stream: RTMPStream) : IEventListener {

        override fun handleEvent(event: Event) {
            val data = EventUtils.toMap(event)
            when (data["code"].toString()) {
                RTMPConnection.Codes.CONNECT_SUCCESS.rawValue -> connection!!.createStream(stream)
                "NetStream.Publish.Start" -> stream.setReadyState(ReadyState.PUBLISHING)
                else -> {
                }
            }
        }

        override fun toString(): String {
            return ToStringBuilder.reflectionToString(this)
        }
    }

    internal enum class ReadyState(val rawValue: Byte) {
        INITIALIZED(0x00),
        OPEN(0x01),
        PLAY(0x02),
        PLAYING(0x03),
        PUBLISH(0x04),
        PUBLISHING(0x05),
        CLOSED(0x06);
    }

    internal var connection: RTMPConnection? = null
    internal var id = 0
    internal var muxer: RTMPMuxer = RTMPMuxer(this)
    internal val camera: Camera? = null
    internal val audio: AudioInfo? = null
    internal val audioBuffer: ShortArray? = null
    internal val encoders = ConcurrentHashMap<String, IEncoder>()
    internal var readyState = ReadyState.INITIALIZED
    internal val messages = ArrayList<RTMPMessage>()
    internal val listener = EventListener(this)

    init {
        this.connection = connection
        this.connection!!.addEventListener(Event.RTMP_STATUS, listener)
        if (this.connection!!.isConnected) {
            this.connection!!.createStream(this)
        }
        addEventListener(Event.RTMP_STATUS, listener)
    }

    fun attachAudio(audio: AudioInfo?) {
        if (audio == null) {
            return
        }
        getEncoderByName("audio/mp4a-latm")
        val record = audio.audioRecord
        val frameBufferSize = audio.minBufferSize
        record?.positionNotificationPeriod = frameBufferSize / 2
        record?.setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {
            override fun onMarkerReached(audioRecord: AudioRecord) {}
            override fun onPeriodicNotification(audioRecord: AudioRecord) {
                val buffer = audio.buffer
                record.read(buffer, 0, frameBufferSize)
                getEncoderByName("audio/mp4a-latm").encodeBytes(buffer!!, audio.currentPresentationTimestamp.toLong())
            }
        })
        record?.startRecording()
        record?.read(audio.buffer, 0, frameBufferSize)
    }

    fun attachCamera(camera: Camera?) {
        if (camera == null) {
            return
        }
        getEncoderByName("video/avc")
        val parameters = camera.parameters
        parameters.previewFormat = ImageFormat.NV21
        camera.parameters = parameters
        camera.setPreviewCallback { bytes, camera ->
            getEncoderByName("video/avc").encodeBytes(bytes, System.nanoTime() / 1000000)
        }
    }

    @JvmOverloads fun publish(name: String?, howToPublish: HowToPublish = HowToPublish.LIVE) {
        val message = RTMPCommandMessage(connection!!.objectEncoding)
        message.transactionID = 0
        message.commandName = if (name != null) "publish" else "closeStream"
        message.chunkStreamID = RTMPChunk.AUDIO
        message.streamID = id

        if (name == null) {
            when (readyState) {
                RTMPStream.ReadyState.PUBLISHING -> connection!!.socket.doOutput(RTMPChunk.ZERO, message)
                else -> {
                }
            }
            return
        }

        var arguments = mutableListOf<Any?>()
        arguments.add(name)
        arguments.add(howToPublish.rawValue)
        message.arguments = arguments

        when (readyState) {
            RTMPStream.ReadyState.INITIALIZED -> {
                messages.add(message)
            }
            RTMPStream.ReadyState.OPEN -> {
                connection!!.socket.doOutput(RTMPChunk.ZERO, message)
                setReadyState(ReadyState.PUBLISH)
            }
            else -> {
            }
        }
    }

    fun play(vararg arguments: Any) {
        val streamName = arguments[0]
        val message = RTMPCommandMessage(connection!!.objectEncoding)
        message.transactionID = 0
        message.commandName = if (streamName != null) "play" else "closeStream"
        message.arguments = Arrays.asList(*arguments)
        message.chunkStreamID = RTMPChunk.CONTROL
        message.streamID = id

        if (streamName == null) {
            when (readyState) {
                RTMPStream.ReadyState.PLAYING -> connection!!.socket.doOutput(RTMPChunk.ZERO, message)
                else -> {
                }
            }
            return
        }

        when (readyState) {
            RTMPStream.ReadyState.INITIALIZED -> messages.add(message)
            RTMPStream.ReadyState.OPEN, RTMPStream.ReadyState.PLAYING -> connection!!.socket.doOutput(RTMPChunk.ZERO, message)
            else -> {
            }
        }
    }

    fun send(handlerName: String, vararg arguments: Any) {
        if (readyState == ReadyState.INITIALIZED || readyState == ReadyState.CLOSED) {
            return
        }
        var message = RTMPDataMessage(connection!!.objectEncoding)
        message.handlerName = handlerName
        arguments.forEach { value ->
            message.arguments.add(value)
        }
        message.streamID = id
        message.chunkStreamID = RTMPChunk.COMMAND
        connection!!.socket.doOutput(RTMPChunk.ZERO, message)
    }

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }

    internal fun setReadyState(readyState: ReadyState): RTMPStream {
        this.readyState = readyState
        when (readyState) {
            RTMPStream.ReadyState.OPEN -> {
                for (message in messages) {
                    message.streamID = id
                    connection!!.socket.doOutput(RTMPChunk.ZERO, message)
                }
                messages.clear()
            }
            RTMPStream.ReadyState.PUBLISHING -> {
                send("@setDataFrame", "onMetaData", toMetaData())
                for (encoder in encoders.values) {
                    encoder.listener = muxer
                    encoder.startRunning()
                }
            }
            else -> {
            }
        }
        return this
    }

    protected fun toMetaData(): Map<String, Any> {
        val data = HashMap<String, Any>()
        return data
    }

    protected fun getEncoderByName(mime: String): IEncoder {
        if (!encoders.containsKey(mime)) {
            when (mime) {
                "video/avc" -> encoders.put(mime, H264Encoder())
                "audio/mp4a-latm" -> encoders.put(mime, AACEncoder())
                else -> {}
            }
        }
        return encoders[mime]!!
    }
}
