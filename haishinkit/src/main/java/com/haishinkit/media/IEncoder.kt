package com.haishinkit.media

import com.haishinkit.lang.IRunnable

interface IEncoder : IRunnable {
    var listener: IEncoderListener?
    fun encodeBytes(data: ByteArray, presentationTimeUs: Long)
}
