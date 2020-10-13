package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import android.graphics.Matrix
import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import kotlinx.coroutines.Runnable

// TODO: Should these buffers be in their own directory?
// TODO: Should these buffers need interfaces?
// TODO: Should these buffers use a base class for ref counting?
/**
 * [DefaultVideoFrameTextureBuffer] provides an reference counted wrapper of
 * an [VideoFrameTextureBuffer] interface which will call [releaseCallback]
 * when nothing is holding the buffer any longer
 */
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
