package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.TimestampAligner
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * [DefaultSurfaceTextureCaptureSource] will provide a [Surface] which it will listen to
 * and convert to [VideoFrameTextureBuffer] objects
 */
class DefaultSurfaceTextureCaptureSource(
    private val logger: Logger,
    private val width: Int,
    private val height: Int,
    override val contentHint: ContentHint = ContentHint.None,
    private val eglCoreFactory: EglCoreFactory
) : SurfaceTextureCaptureSource {
    override lateinit var surface: Surface

    private var textureId: Int = 0
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var eglCore: EglCore
    private val thread: HandlerThread = HandlerThread("DefaultSurfaceTextureCaptureSource")
    private val handler: Handler

    // This class helps align timestamps from the surface to timestamps
    // originating from different monotonic clocks in native code
    private val timestampAligner = TimestampAligner()

    // Frame available listener was called when a texture was already in use
    private var pendingTexture = false

    // Texture is in use, possibly in another thread
    private var textureInUse = false

    // Dispose has been called and we are waiting on texture to be released
    private var released = false

    private var sinks = mutableSetOf<VideoSink>()

    private val TAG = "SurfaceTextureCaptureSource"

    init {
        thread.start()
        handler = Handler(thread.looper)

        runBlocking(handler.asCoroutineDispatcher().immediate) {
            eglCore = eglCoreFactory.createEglCore()

            val surfaceAttributes =
                intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE)
            eglCore.eglSurface = EGL14.eglCreatePbufferSurface(
                eglCore.eglDisplay,
                eglCore.eglConfig,
                surfaceAttributes,
                0
            )
            EGL14.eglMakeCurrent(
                eglCore.eglDisplay,
                eglCore.eglSurface,
                eglCore.eglSurface,
                eglCore.eglContext
            )
            GlUtil.checkGlError("Failed to set dummy surface to initialize surface texture video source")

            textureId = GlUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)

            surfaceTexture = SurfaceTexture(textureId)
            surfaceTexture.setDefaultBufferSize(width, height)
            @SuppressLint("Recycle")
            surface = Surface(surfaceTexture)

            logger.info(
                TAG,
                "Created surface texture for video source with dimensions $width x $height"
            )
        }
    }

    override fun start() {
        handler.post {
            surfaceTexture.setOnFrameAvailableListener({
                pendingTexture = true
                tryCapturingFrame()
            }, handler)
        }
    }

    override fun stop() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            logger.info(TAG, "Setting on frame available listener to null")
            surfaceTexture.setOnFrameAvailableListener(null)
        }
    }

    override fun addCaptureSourceObserver(observer: CaptureSourceObserver) {}

    override fun removeCaptureSourceObserver(observer: CaptureSourceObserver) {}

    override fun addVideoSink(sink: VideoSink) {
        handler.post { sinks.add(sink) }
    }

    override fun removeVideoSink(sink: VideoSink) {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            sinks.remove(
                sink
            )
        }
    }

    override fun release() {
        handler.post {
            logger.info(TAG, "Releasing surface texture capture source")
            released = true
            if (!textureInUse) {
                completeRelease()
            }
        }
    }

    private fun tryCapturingFrame() {
        check(Looper.myLooper() == handler.looper)

        if (released || !pendingTexture || textureInUse) {
            return
        }
        textureInUse = true
        pendingTexture = false

        // This call is what actually updates the texture
        surfaceTexture.updateTexImage()

        val transformMatrix = FloatArray(16)
        surfaceTexture.getTransformMatrix(transformMatrix)

        val buffer = DefaultVideoFrameTextureBuffer(
            width,
            height,
            textureId,
            GlUtil.convertToMatrix(transformMatrix),
            VideoFrameTextureBuffer.Type.TEXTURE_OES,
            Runnable { frameReleased() })
        val timestamp = timestampAligner.translateTimestamp(surfaceTexture.timestamp)

        logger.info(TAG, "timestamp before ${surfaceTexture.timestamp} after $timestamp")
        val frame = VideoFrame(timestamp, buffer)

        sinks.forEach { it.onVideoFrameReceived(frame) }
        frame.release()
    }

    private fun frameReleased() {
        // Cannot assume this occurs on correct thread
        handler.post {
            textureInUse = false
            if (released) {
                this.completeRelease()
            } else {
                // May have pending frame
                tryCapturingFrame()
            }
        }
    }

    private fun completeRelease() {
        check(Looper.myLooper() == handler.looper)

        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        surfaceTexture.release()
        surface.release()

        EGL14.eglMakeCurrent(
            eglCore.eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(eglCore.eglDisplay, eglCore.eglSurface)
        eglCore.release()

        timestampAligner.dispose()
        logger.info(TAG, "Finished releasing surface texture capture source")

        handler.looper.quit()
    }
}
