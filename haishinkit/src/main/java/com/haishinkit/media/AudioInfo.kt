package com.haishinkit.media

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

import com.haishinkit.util.Log

import org.apache.commons.lang3.builder.ToStringBuilder

class AudioInfo {
    var channel = DEFAULT_CHANNEL
    var encoding = DEFAULT_ENCODING
    var buffer: ByteArray? = null
    var samplingRate = DEFAULT_SAMPLING_RATE
    var minBufferSize = -1
    var audioRecord: AudioRecord? = null
    var currentPresentationTimestamp = 0

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }

    companion object {
        val DEFAULT_CHANNEL = AudioFormat.CHANNEL_IN_MONO
        val DEFAULT_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        val DEFAULT_SAMPLING_RATE = 44100
    }
}
