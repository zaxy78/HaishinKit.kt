package com.haishinkit.media

import java.io.IOException

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaCodecInfo
import android.provider.MediaStore

internal class H264Encoder : EncoderBase(MIME) {

    private var bitRate = DEFAULT_BIT_RATE
    private var frameRate = DEFAULT_FRAME_RATE
    private var IFrameInterval = DEFAULT_I_FRAME_INTERVAL
    private var width = DEFAULT_WIDTH
    private var height = DEFAULT_HEIGHT
    private var profile = DEFAULT_PROFILE
    private var level = DEFAULT_LEVEL

    fun getBitRate(): Int {
        return bitRate
    }

    fun setBitRate(bitRate: Int): H264Encoder {
        this.bitRate = bitRate
        return this
    }

    fun getFrameRate(): Int {
        return frameRate
    }

    fun setFrameRate(frameRate: Int): H264Encoder {
        this.frameRate = frameRate
        return this
    }

    fun getIFrameInterval(): Int {
        return IFrameInterval
    }

    fun setIFrameInterval(IFrameInterval: Int): H264Encoder {
        this.IFrameInterval = IFrameInterval
        return this
    }

    fun getWidth(): Int {
        return width
    }

    fun setWidth(width: Int): H264Encoder {
        this.width = width
        return this
    }

    fun getHeight(): Int {
        return height
    }

    fun setHeight(height: Int): H264Encoder {
        this.height = height
        return this
    }

    fun getProfile(): Int {
        return profile
    }

    fun setProfile(profile: Int): H264Encoder {
        this.profile = profile
        return this
    }

    fun getLevel(): Int {
        return level
    }

    fun setLevel(level: Int): H264Encoder {
        this.level = level
        return this
    }

    @Throws(IOException::class)
    override fun createMediaCodec(): MediaCodec {
        val codec = MediaCodec.createEncoderByType(MIME)
        val mediaFormat = MediaFormat.createVideoFormat(MIME, getWidth(), getHeight())
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, getBitRate())
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, getFrameRate())
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, getIFrameInterval())
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, getProfile())
        mediaFormat.setInteger(MediaFormat.KEY_AAC_ENCODED_TARGET_LEVEL, getLevel())
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return codec
    }

    companion object {
        val MIME = "video/avc"
        val DEFAULT_BIT_RATE = 125000
        val DEFAULT_FRAME_RATE = 15
        val DEFAULT_I_FRAME_INTERVAL = 2
        val DEFAULT_WIDTH = 320
        val DEFAULT_HEIGHT = 240
        val DEFAULT_PROFILE = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
        val DEFAULT_LEVEL = MediaCodecInfo.CodecProfileLevel.AVCLevel31
    }
}
