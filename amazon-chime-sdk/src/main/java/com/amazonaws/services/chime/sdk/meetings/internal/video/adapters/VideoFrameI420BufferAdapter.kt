package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameI420Buffer
import java.nio.ByteBuffer

/**
 * [VideoFrameI420BufferAdapter] provides two classes to adapt [VideoFrameI420Buffer] to
 * [com.xodee.client.video.VideoFrameI420Buffer] or vice-versa
 */
class VideoFrameI420BufferAdapter {
    class SDKToMedia(
        private val i420Buffer: VideoFrameI420Buffer
    ) : com.xodee.client.video.VideoFrameI420Buffer {
        override fun getWidth(): Int = i420Buffer.width
        override fun getHeight(): Int = i420Buffer.height
        override fun getDataY(): ByteBuffer? = i420Buffer.dataY
        override fun getDataU(): ByteBuffer? = i420Buffer.dataU
        override fun getDataV(): ByteBuffer? = i420Buffer.dataV
        override fun getStrideY(): Int = i420Buffer.strideY
        override fun getStrideU(): Int = i420Buffer.strideU
        override fun getStrideV(): Int = i420Buffer.strideV
        override fun retain() = i420Buffer.retain()
        override fun release() = i420Buffer.release()
    }

    class MediaToSDK(
        private val i420Buffer: com.xodee.client.video.VideoFrameI420Buffer
    ) : VideoFrameI420Buffer {
        override val width: Int = i420Buffer.width
        override val height: Int = i420Buffer.height
        override val dataY: ByteBuffer = i420Buffer.dataY
        override val dataU: ByteBuffer = i420Buffer.dataU
        override val dataV: ByteBuffer = i420Buffer.dataV
        override val strideY: Int = i420Buffer.strideY
        override val strideU: Int = i420Buffer.strideU
        override val strideV: Int = i420Buffer.strideV
        override fun retain() = i420Buffer.retain()
        override fun release() = i420Buffer.release()
    }
}
