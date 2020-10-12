package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import android.graphics.Matrix
import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import kotlinx.coroutines.Runnable

class DefaultVideoFrameTextureBuffer(
    override val width: Int,
    override val height: Int,
    override val textureId: Int,
    override val transformMatrix: Matrix?,
    override val type: VideoFrameTextureBuffer.Type,
    releaseCallback: Runnable
) : VideoFrameTextureBuffer {
    private val refCountDelegate = RefCountDelegate(releaseCallback)
    override fun retain() = refCountDelegate.retain()
    override fun release() = refCountDelegate.release()
}
