package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameRGBABuffer
import java.nio.ByteBuffer

/**
 * [VideoFrameRGBABufferAdapter] provides one classes to adapt [VideoFrameRGBABuffer] to
 * [com.xodee.client.video.VideoFrameRGBABuffer].  Reverse will never occur.
 */
class VideoFrameRGBABufferAdapter {
    class SDKToMedia(private val rgbaBuffer: VideoFrameRGBABuffer) : com.xodee.client.video.VideoFrameRGBABuffer {
        override fun getWidth(): Int = rgbaBuffer.width
        override fun getHeight(): Int = rgbaBuffer.height
        override fun getData(): ByteBuffer? = rgbaBuffer.data
        override fun getStride(): Int = rgbaBuffer.stride
        override fun retain() = rgbaBuffer.retain()
        override fun release() = rgbaBuffer.release()
    }
}