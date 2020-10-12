package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import java.nio.ByteBuffer

class DefaultVideoFrameRGBABuffer(
    override val width: Int,
    override val height: Int,
    override val data: ByteBuffer,
    override val stride: Int,
    releaseCallback: Runnable
) : VideoFrameRGBABuffer {
    private val refCountDelegate = RefCountDelegate(releaseCallback)
    override fun retain() = refCountDelegate.retain()
    override fun release() = refCountDelegate.release()
}