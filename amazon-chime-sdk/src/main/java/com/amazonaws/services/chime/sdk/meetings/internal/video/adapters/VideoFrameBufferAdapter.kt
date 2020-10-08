package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameBuffer

/**
 * [VideoFrameBufferAdapter] provides two classes to adapt [VideoFrameBuffer] to
 * [com.xodee.client.video.VideoFrameBuffer] or vice-versa
 */
object VideoFrameBufferAdapter {
    class SDKToMedia(
            private val buffer: VideoFrameBuffer
    ) : com.xodee.client.video.VideoFrameBuffer {
        override fun getWidth(): Int = buffer.width
        override fun getHeight(): Int = buffer.height
        override fun release() = buffer.release()
        override fun retain() = buffer.retain()
    }

    class MediaToSDK(
            private val buffer: com.xodee.client.video.VideoFrameBuffer
    ) : VideoFrameBuffer {
        override val width: Int = buffer.width
        override val height: Int = buffer.height
        override fun release() = buffer.release()
        override fun retain() = buffer.retain()
    }
}