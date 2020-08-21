package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameRGBABuffer
import java.nio.ByteBuffer


class VideoFrameRGBABufferAdapter {
    class SDKToMedia(private val rgbaBuffer: VideoFrameRGBABuffer) : com.xodee.client.video.VideoFrameRGBABuffer {
        override fun getWidth(): Int {
            return rgbaBuffer.width
        }

        override fun getHeight(): Int {
            return rgbaBuffer.height
        }

        override fun getData(): ByteBuffer? {
            return rgbaBuffer.data
        }

        override fun getStride(): Int {
            return rgbaBuffer.stride
        }

        override fun retain() {
            rgbaBuffer.retain()
        }

        override fun release() {
            rgbaBuffer.release()
        }
    }
}