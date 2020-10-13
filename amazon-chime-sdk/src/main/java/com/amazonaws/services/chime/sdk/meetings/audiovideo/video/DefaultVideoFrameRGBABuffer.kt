package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import java.nio.ByteBuffer

// TODO: Should these buffers be in their own directory?
// TODO: Should these buffers need interfaces?
// TODO: Should these buffers use a base class for ref counting?
/**
 * [DefaultVideoFrameRGBABuffer] provides an reference counted wrapper of
 * an [VideoFrameRGBABuffer] interface which will call [releaseCallback]
 * when nothing is holding the buffer any longer
 */
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
