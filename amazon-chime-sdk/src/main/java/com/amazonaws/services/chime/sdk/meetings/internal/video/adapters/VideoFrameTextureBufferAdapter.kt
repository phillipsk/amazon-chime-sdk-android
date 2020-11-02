package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import android.graphics.Matrix
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameTextureBuffer
import java.security.InvalidParameterException

class VideoFrameTextureBufferAdapter{
    class SDKToMedia(
        private val textureBuffer: VideoFrameTextureBuffer
    ) : com.xodee.client.video.VideoFrameTextureBuffer {

        override fun getWidth(): Int {
            return textureBuffer.width
        }

        override fun getHeight(): Int {
            return textureBuffer.height
        }

        override fun getType(): com.xodee.client.video.VideoFrameTextureBuffer.Type {
            return when (textureBuffer.type) {
                VideoFrameTextureBuffer.Type.TEXTURE_OES -> com.xodee.client.video.VideoFrameTextureBuffer.Type.OES
                VideoFrameTextureBuffer.Type.TEXTURE_2D -> com.xodee.client.video.VideoFrameTextureBuffer.Type.RGB
            }
        }

        override fun getTransformMatrix(): Matrix? {
            return textureBuffer.transformMatrix
        }

        override fun getTextureId(): Int {
            return textureBuffer.textureId;
        }

        override fun release() {
            textureBuffer.release()
        }

        override fun retain() {
            textureBuffer.retain()
        }
    }

    class MediaToSDK(
        private val textureBuffer: com.xodee.client.video.VideoFrameTextureBuffer
    ) : VideoFrameTextureBuffer {
        override val width: Int
            get() = textureBuffer.width

        override val height: Int
            get() = textureBuffer.height

        override val textureId: Int
            get() = textureBuffer.textureId

        override val transformMatrix: Matrix?
            get() = textureBuffer.transformMatrix

        override val type: VideoFrameTextureBuffer.Type
            get() {
                return when (textureBuffer.type) {
                    com.xodee.client.video.VideoFrameTextureBuffer.Type.OES -> VideoFrameTextureBuffer.Type.TEXTURE_OES
                    com.xodee.client.video.VideoFrameTextureBuffer.Type.RGB -> VideoFrameTextureBuffer.Type.TEXTURE_2D
                    else -> throw InvalidParameterException("Cannot have null type")
                }
            }

        override fun release() {
            textureBuffer.release()
        }

        override fun retain() {
            textureBuffer.retain()
        }
    }
}