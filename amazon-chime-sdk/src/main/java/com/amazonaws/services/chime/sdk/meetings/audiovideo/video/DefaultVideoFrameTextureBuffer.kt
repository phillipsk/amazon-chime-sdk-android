package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import android.graphics.Matrix
import android.opengl.GLES20
import android.os.Handler
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.*
import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.JniUtil
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import kotlin.math.roundToInt


class DefaultVideoFrameTextureBuffer(
    override val width: Int,
    override val height: Int,
    override val textureId: Int,
    override val transformMatrix: Matrix?,
    override val type: VideoFrameTextureBuffer.Type,
    releaseCallback: Runnable
) : VideoFrameTextureBuffer {
    private val refCountDelegate =
        RefCountDelegate(
            releaseCallback
        )

    override fun retain() {
        refCountDelegate.retain()
    }

    override fun release() {
        refCountDelegate.release()
    }

}