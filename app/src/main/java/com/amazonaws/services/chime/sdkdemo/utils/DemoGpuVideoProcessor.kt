package com.amazonaws.services.chime.sdkdemo.utils

import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultGlVideoFrameDrawer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.ContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.VideoSource
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

class DemoGpuVideoProcessor(private val logger: Logger, eglCoreFactory: EglCoreFactory) : VideoSource, VideoSink {
    override val contentHint: ContentHint = ContentHint.Motion

    // Pending frame to render. Serves as a queue with size 1. Synchronized on |frameLock|.
    private var pendingFrame: VideoFrame? = null
    private val pendingFrameLock = Any()

    private val bwDrawer = BlackAndWhiteGlVideoFrameDrawer()
    private val rectDrawer = DefaultGlVideoFrameDrawer()

    private lateinit var textureFrameBuffer: GlTextureFrameBufferHelper

    private lateinit var eglCore: EglCore
    private val thread: HandlerThread = HandlerThread("DemoGpuVideoProcessor")
    private val handler: Handler

    private val TAG = "DemoGpuVideoProcessor"

    // Texture is in use, possibly in another thread
    private var textureInUse = false

    // Dispose has been called and we are waiting on texture to be released
    private var released = false

    private val sinks = mutableSetOf<VideoSink>()

    init {
        thread.start()
        handler = Handler(thread.looper)

        runBlocking(handler.asCoroutineDispatcher().immediate) {
            eglCore = eglCoreFactory.createEglCore()

            // We need to create a dummy surface before we can set the cotext as current
            val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            eglCore.eglSurface = EGL14.eglCreatePbufferSurface(eglCore.eglDisplay, eglCore.eglConfig, surfaceAttribs, 0)
            EGL14.eglMakeCurrent(eglCore.eglDisplay, eglCore.eglSurface, eglCore.eglSurface, eglCore.eglContext)
            GlUtil.checkGlError("Failed to set dummy surface to initialize surface texture video source")

            textureFrameBuffer = GlTextureFrameBufferHelper(GLES20.GL_RGBA)

            logger.info(TAG, "Created demo GPU video processor")
        }
    }

    fun release() {
        handler.post {
            logger.info(TAG, "Releasing surface texture capture source")
            released = true
            // We cannot release until no downstream users have access to texture buffer
            if (!textureInUse) {
                completeRelease();
            }
        }
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        synchronized(pendingFrameLock) {
            if (pendingFrame != null) {
                pendingFrame?.release()
            }
            pendingFrame = frame
            pendingFrame?.retain()
        }

        handler.post(::tryCapturingFrame)
    }

    private fun tryCapturingFrame() {
        check(Looper.myLooper() == handler.looper)
        // Fetch and render |pendingFrame|.
        var frame: VideoFrame
        synchronized(pendingFrameLock) {
            if (pendingFrame == null) {
                return
            }
            frame = pendingFrame as VideoFrame
            pendingFrame = null
        }

        if (released || textureInUse) {
            frame.release()
            return;
        }
        textureInUse = true

        textureFrameBuffer.setSize(frame.getRotatedWidth(), frame.getRotatedHeight())
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureFrameBuffer.frameBufferId)

        // Convert to black and white
        bwDrawer.drawFrame(frame, 0, 0, frame.getRotatedWidth(), frame.getRotatedHeight(), null)
        // Draw the original frame in the bottom left corner
        rectDrawer.drawFrame(frame, 0, 0, frame.getRotatedWidth() / 2, frame.getRotatedHeight() / 2, null)

        // Must call this otherwise downstream users will not have a synchronized texture
        GLES20.glFinish()
        // Reset to default framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        val processedBuffer =
                DefaultVideoFrameTextureBuffer(frame.getRotatedWidth(), frame.getRotatedHeight(), textureFrameBuffer.textureId, null, VideoFrameTextureBuffer.Type.TEXTURE_2D, Runnable { frameReleased() })
        // Drawer gets rid of any rotation
        val processedFrame = VideoFrame(frame.timestampNs, processedBuffer)

        sinks.forEach { it.onVideoFrameReceived(processedFrame) }
        processedFrame.release()
        frame.release()
    }

    // Called once texture buffer ref count reaches 0
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

        synchronized(pendingFrameLock) {
            if (pendingFrame != null) {
                pendingFrame?.release();
                pendingFrame = null;
            }
        }

        rectDrawer.release()
        bwDrawer.release()
        textureFrameBuffer.release()

        handler.looper.quit();
    }
}