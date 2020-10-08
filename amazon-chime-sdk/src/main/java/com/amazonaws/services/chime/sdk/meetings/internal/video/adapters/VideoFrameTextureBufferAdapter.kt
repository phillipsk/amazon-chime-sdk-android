package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import android.graphics.Matrix
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameTextureBuffer
import java.security.InvalidParameterException

/**
 * [VideoFrameTextureBufferAdapter] provides two classes to adapt [VideoFrameTextureBuffer] to
 * [com.xodee.client.video.VideoFrameTextureBuffer].  Reverse will never occur.
 */
class VideoFrameTextureBufferAdapter {
    class SDKToMedia(
            private val textureBuffer: VideoFrameTextureBuffer
    ) : com.xodee.client.video.VideoFrameTextureBuffer {
        override fun getWidth(): Int = textureBuffer.width
        override fun getHeight(): Int = textureBuffer.height
        override fun getType(): com.xodee.client.video.VideoFrameTextureBuffer.Type {
            return when (textureBuffer.type) {
                VideoFrameTextureBuffer.Type.TEXTURE_OES -> com.xodee.client.video.VideoFrameTextureBuffer.Type.OES
                VideoFrameTextureBuffer.Type.TEXTURE_2D -> com.xodee.client.video.VideoFrameTextureBuffer.Type.RGB
            }
        }

        override fun getTransformMatrix(): Matrix? = textureBuffer.transformMatrix
        override fun getTextureId(): Int = textureBuffer.textureId
        override fun release() = textureBuffer.release()
        override fun retain() = textureBuffer.retain()
    }

    class MediaToSDK(
            private val textureBuffer: com.xodee.client.video.VideoFrameTextureBuffer
    ) : VideoFrameTextureBuffer {
        override val width: Int = textureBuffer.width
        override val height: Int = textureBuffer.height
        override val textureId: Int = textureBuffer.textureId
        override val transformMatrix: Matrix? = textureBuffer.transformMatrix
        override val type: VideoFrameTextureBuffer.Type
            get() {
                return when (textureBuffer.type) {
                    com.xodee.client.video.VideoFrameTextureBuffer.Type.OES -> VideoFrameTextureBuffer.Type.TEXTURE_OES
                    com.xodee.client.video.VideoFrameTextureBuffer.Type.RGB -> VideoFrameTextureBuffer.Type.TEXTURE_2D
                    else -> throw InvalidParameterException("Cannot have null type")
                }
            }

        override fun release() = textureBuffer.release()
        override fun retain() = textureBuffer.retain()
    }
}