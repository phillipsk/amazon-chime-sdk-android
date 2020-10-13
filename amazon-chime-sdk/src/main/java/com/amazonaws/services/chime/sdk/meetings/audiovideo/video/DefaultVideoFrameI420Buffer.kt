package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import java.nio.ByteBuffer

// TODO: Should these buffers be in their own directory?
// TODO: Should these buffers need interfaces?
// TODO: Should these buffers use a base class for ref counting?
/**
 * [DefaultVideoFrameI420Buffer] provides an reference counted wrapper of
 * an [VideoFrameI420Buffer] interface which will call [releaseCallback]
 * when nothing is holding the buffer any longer
 */
class DefaultVideoFrameI420Buffer(
    override val width: Int,
    override val height: Int,
    override val dataY: ByteBuffer,
    override val dataU: ByteBuffer,
    override val dataV: ByteBuffer,
    override val strideY: Int,
    override val strideU: Int,
    override val strideV: Int,
    releaseCallback: Runnable
) : VideoFrameI420Buffer {
    private val refCountDelegate = RefCountDelegate(releaseCallback)
    override fun retain() = refCountDelegate.retain()
    override fun release() = refCountDelegate.release()
}
