package com.haishinkit.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.io.IOException

class AACEncoder : EncoderBase(MIME) {

    @Throws(IOException::class)
    override fun createMediaCodec(): MediaCodec {
        val codec = MediaCodec.createEncoderByType(MIME)
        val mediaFormat = MediaFormat.createAudioFormat(MIME, 44100, 1)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000)
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return codec
    }

    companion object {
        val MIME = "audio/mp4a-latm"
    }
}
