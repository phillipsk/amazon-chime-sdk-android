package com.amazonaws.services.chime.sdkdemo.utils

import android.graphics.Matrix
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.*
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultGlVideoFrameDrawer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.ContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.VideoSource
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.JniUtil
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

class CpuVideoProcessor(private val logger: Logger, eglCoreFactory: EglCoreFactory): VideoSource, VideoSink {
    private val sinks = mutableSetOf<VideoSink>()

    private lateinit var eglCore: EglCore
    private val thread: HandlerThread = HandlerThread("DemoGpuVideoProcessor")
    private val handler: Handler

    // The camera capture source currently output OES texture frames, so we draw them to a frame buffer that
    // we can read to host memory from
    private val rectDrawer = DefaultGlVideoFrameDrawer()
    private val textureFrameBuffer = GlTextureFrameBufferHelper(GLES20.GL_RGBA)

    override val contentHint: ContentHint = ContentHint.Motion

    private val TAG = "DemoCPUVideoProcessor"

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

            logger.info(TAG, "Created demo GPU video processor")
        }
    }

    override fun addVideoSink(sink: VideoSink) {
        sinks.add(sink)
    }

    override fun removeVideoSink(sink: VideoSink) {
        sinks.remove(sink)
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        frame.retain()
        handler.post {
            // Note: This processor assumes that the incoming call will be on a valid EGL context
            textureFrameBuffer.setSize(frame.getRotatedWidth(), frame.getRotatedHeight())
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureFrameBuffer.frameBufferId)

            val matrix = Matrix()
            // Shift before flipping
            matrix.preTranslate(0.5f, 0.5f)
            // RGBA frames are upside down relative to texture coordinates
            matrix.preScale(1f, -1f)
            // Unshift following flip
            matrix.preTranslate(-0.5f, -0.5f)
            // Note the draw call will account for any rotation, so we need to account for that in viewport width/height
            rectDrawer.drawFrame(frame, 0, 0, frame.getRotatedWidth(), frame.getRotatedHeight(), matrix)

            // Read RGBA data to native byte buffer
            val rgbaData = JniUtil.nativeAllocateByteBuffer(frame.width * frame.height * 4);
            GLES20.glReadPixels(0, 0, frame.getRotatedWidth(), frame.getRotatedHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, rgbaData);
            GlUtil.checkGlError("glReadPixels");

            val rgbaBuffer =
                    DefaultVideoFrameRGBABuffer(
                            frame.getRotatedWidth(),
                            frame.getRotatedHeight(),
                            rgbaData, frame.getRotatedWidth() * 4,
                            Runnable { JniUtil.nativeFreeByteBuffer(rgbaData) })

            convertToBlackAndWhite(rgbaBuffer)

            val processedFrame = VideoFrame(frame.timestampNs, rgbaBuffer)

            sinks.forEach { it.onVideoFrameReceived(processedFrame) }
            frame.release()
        }
    }

    fun release() {
        handler.post {
            rectDrawer.release()
            textureFrameBuffer.release()
        }
    }

    // This is of course an extremely inefficient way of converting to black and white
    private fun convertToBlackAndWhite(rgbaBuffer: VideoFrameRGBABuffer) {
        // So we don't need to pollute with @ExperimentalUnsignedTypes annotation
        fun Byte.toPositiveInt() = toInt() and 0xFF

        for (x in 0 until rgbaBuffer.width) {
            for (y in 0 until rgbaBuffer.height) {
                val rLocation = y * rgbaBuffer.stride + x * 4
                val gLocation = rLocation + 1
                val bLocation = rLocation + 2
                val aLocation = rLocation + 3

                val rValue = rgbaBuffer.data[rLocation].toPositiveInt()
                val gValue = rgbaBuffer.data[gLocation].toPositiveInt()
                val bValue = rgbaBuffer.data[bLocation].toPositiveInt()

                val newValue= ((rValue + gValue + bValue) / (3.0)).toByte()

                rgbaBuffer.data.put(rLocation, newValue)
                rgbaBuffer.data.put(gLocation, newValue)
                rgbaBuffer.data.put(bLocation, newValue)
            }
        }
    }
}