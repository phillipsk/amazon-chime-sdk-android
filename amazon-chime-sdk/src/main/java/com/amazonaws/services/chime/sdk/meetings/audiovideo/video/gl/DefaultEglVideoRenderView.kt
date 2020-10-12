/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoRotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt


open class DefaultEglVideoRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle), SurfaceHolder.Callback,
    EglVideoRenderView {
    /**
     * Helper class for determining layout size based on layout requirements, scaling type, and video
     * aspect ratio.
     */
    class VideoLayoutMeasure {
        enum class ScalingType { SCALE_ASPECT_FIT, SCALE_ASPECT_FILL }

        var scalingType = ScalingType.SCALE_ASPECT_FILL

        fun measure(
            widthSpec: Int,
            heightSpec: Int,
            frameWidth: Int,
            frameHeight: Int
        ): Point {
            // Calculate max allowed layout size.
            val maxWidth = View.getDefaultSize(Int.MAX_VALUE, widthSpec)
            val maxHeight =
                View.getDefaultSize(Int.MAX_VALUE, heightSpec)
            if (frameWidth == 0 || frameHeight == 0 || maxWidth == 0 || maxHeight == 0) {
                return Point(maxWidth, maxHeight)
            }
            // Calculate desired display size based on scaling type, video aspect ratio,
            // and maximum layout size.
            val frameAspect = frameWidth / frameHeight.toFloat()
            val layoutSize: Point =
                getDisplaySize(
                    convertScalingTypeToVisibleFraction(scalingType),
                    frameAspect,
                    maxWidth,
                    maxHeight
                )

            // If the measure specification is forcing a specific size - yield.
            if (MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY) {
                layoutSize.x = maxWidth
            }
            if (MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
                layoutSize.y = maxHeight
            }
            return layoutSize
        }

        /**
         * Each scaling type has a one-to-one correspondence to a numeric minimum fraction of the video
         * that must remain visible.
         */
        private fun convertScalingTypeToVisibleFraction(scalingType: ScalingType): Float {
            return when (scalingType) {
                ScalingType.SCALE_ASPECT_FIT -> 1.0f
                ScalingType.SCALE_ASPECT_FILL -> 0.0f
            }
        }

        /**
         * Calculate display size based on minimum fraction of the video that must remain visible,
         * video aspect ratio, and maximum display size.
         */
        private fun getDisplaySize(
            minVisibleFraction: Float,
            videoAspectRatio: Float,
            maxDisplayWidth: Int,
            maxDisplayHeight: Int
        ): Point {
            // If there is no constraint on the amount of cropping, fill the allowed display area.
            if (minVisibleFraction == 0f || videoAspectRatio == 0f) {
                return Point(maxDisplayWidth, maxDisplayHeight)
            }
            // Each dimension is constrained on max display size and how much we are allowed to crop.
            val width =
                maxDisplayWidth.coerceAtMost((maxDisplayHeight / minVisibleFraction * videoAspectRatio).roundToInt())
            val height =
                maxDisplayHeight.coerceAtMost((maxDisplayWidth / minVisibleFraction / videoAspectRatio).roundToInt())
            return Point(width, height)
        }
    }

    // Accessed only on the main thread.
    private var rotatedFrameWidth = 0
    private var rotatedFrameHeight = 0
    private var frameRotation = VideoRotation.Rotation0

    private var surfaceWidth = 0
    private var surfaceHeight = 0

    // EGL and GL resources for drawing YUV/OES textures. After initialization, these are only
    // accessed from the render thread.
    private var eglCore: EglCore? = null
    private val surface: Any? = null
    private var renderHandler: Handler? = null

    // Cached matrix for draw command
    private val drawMatrix: Matrix = Matrix()

    // Pending frame to render. Serves as a queue with size 1. Synchronized on |pendingFrameLock|.
    private var pendingFrame: VideoFrame? = null
    private val pendingFrameLock = Any()

    // If true, mirrors the video stream horizontally.  Publicly accessible
    var mirror = false
    private var layoutAspectRatio = 0f

    private var frameDrawer = DefaultGlVideoFrameDrawer()
    private val videoLayoutMeasure: VideoLayoutMeasure = VideoLayoutMeasure()

    init {
        holder.addCallback(this)
        videoLayoutMeasure.scalingType = VideoLayoutMeasure.ScalingType.SCALE_ASPECT_FIT
    }

    override fun init(eglCoreFactory: EglCoreFactory) {
        rotatedFrameWidth = 0;
        rotatedFrameHeight = 0;

        val thread = HandlerThread("DefaultEglVideoRenderView")
        thread.start()
        this.renderHandler = Handler(thread.looper)

        val validRenderHandler = renderHandler ?: throw UnknownError("No handler in release")
        runBlocking(validRenderHandler.asCoroutineDispatcher().immediate) {
            eglCore = eglCoreFactory.createEglCore()
            surface?.let { createEglSurface(it) }
        }

    }

    override fun release() {
        val validRenderHandler = renderHandler ?: throw UnknownError("No handler in release")
        runBlocking(validRenderHandler.asCoroutineDispatcher().immediate) {
            eglCore?.release()
            eglCore = null

        }
        this.renderHandler?.looper?.quitSafely()
        this.renderHandler = null

        synchronized(pendingFrameLock) {
            pendingFrame?.release();
            pendingFrame = null;
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        releaseEglSurface();
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        surfaceWidth = 0;
        surfaceHeight = 0;
        updateSurfaceSize();

        holder?.let {
            createEglSurface(it.surface)
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val size: Point =
            videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight)
        setMeasuredDimension(size.x, size.y)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        val validRenderHandler = renderHandler ?: throw UnknownError("No handler in release")
        runBlocking(validRenderHandler.asCoroutineDispatcher().immediate) {
            layoutAspectRatio = ((right - left) / (bottom - top).toFloat())
        }
        updateSurfaceSize()
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        if (rotatedFrameWidth != frame.getRotatedWidth()
            || rotatedFrameHeight != frame.getRotatedHeight()
            || frameRotation != frame.rotation
        ) {
            rotatedFrameWidth = frame.getRotatedWidth();
            rotatedFrameHeight = frame.getRotatedHeight();
            frameRotation = frame.rotation;

            CoroutineScope(Dispatchers.Main).launch {
                updateSurfaceSize();
                requestLayout();
            }
        }

        synchronized(pendingFrameLock) {
            // Release any current frame before setting to the latest
            pendingFrame?.release()
            pendingFrame = frame

            pendingFrame?.retain()
            renderHandler?.post(::renderPendingFrame)
        }
    }

    private fun updateSurfaceSize() {
        if (rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && width != 0 && height != 0) {
            val layoutAspectRatio = width / height.toFloat()
            val frameAspectRatio: Float =
                rotatedFrameWidth.toFloat() / rotatedFrameHeight
            val drawnFrameWidth: Int
            val drawnFrameHeight: Int
            if (frameAspectRatio > layoutAspectRatio) {
                drawnFrameWidth = ((rotatedFrameHeight * layoutAspectRatio).toInt())
                drawnFrameHeight = rotatedFrameHeight
            } else {
                drawnFrameWidth = rotatedFrameWidth
                drawnFrameHeight = ((rotatedFrameWidth / layoutAspectRatio).toInt())
            }
            // Aspect ratio of the drawn frame and the view is the same.
            val width = Math.min(width, drawnFrameWidth)
            val height = Math.min(height, drawnFrameHeight)

            if (width != surfaceWidth || height != surfaceHeight) {
                surfaceWidth = width
                surfaceHeight = height
                holder.setFixedSize(width, height)
            }
        } else {
            surfaceHeight = 0
            surfaceWidth = surfaceHeight
            holder.setSizeFromLayout()
        }
    }

    private fun createEglSurface(surface: Any) {
        renderHandler?.post {
            if (eglCore != null && eglCore?.eglSurface == EGL14.EGL_NO_SURFACE) {
                val surfaceAttributess = intArrayOf(EGL14.EGL_NONE)
                eglCore?.eglSurface = EGL14.eglCreateWindowSurface(
                    eglCore?.eglDisplay, eglCore?.eglConfig, surface,
                    surfaceAttributess, 0
                )
                EGL14.eglMakeCurrent(
                    eglCore?.eglDisplay,
                    eglCore?.eglSurface,
                    eglCore?.eglSurface,
                    eglCore?.eglContext
                )

                // Necessary for YUV frames with odd width.
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            }

            // Discard any old frame
            synchronized(pendingFrameLock) {
                pendingFrame?.release()
                pendingFrame = null;
            }
        }
    }

    private fun releaseEglSurface() {
        val validRenderHandler = this.renderHandler ?: return
        runBlocking(validRenderHandler.asCoroutineDispatcher().immediate) {
            // Release frame drawer while we have a valid current context
            frameDrawer.release()

            EGL14.eglMakeCurrent(
                eglCore?.eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglCore?.eglDisplay, eglCore?.eglSurface)
            eglCore?.eglSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    private fun renderPendingFrame() {
        // View could be updating and surface may not be valid
        if (eglCore?.eglSurface == EGL14.EGL_NO_SURFACE) {
            return
        }


        // Fetch pending frame
        var frame: VideoFrame
        synchronized(pendingFrameLock) {
            if (pendingFrame == null) {
                return
            }
            frame = pendingFrame as VideoFrame
            pendingFrame = null
        }
        Log.e("timestamp", "${frame.timestampNs}")

        // Setup draw matrix
        val frameAspectRatio = frame.getRotatedWidth().toFloat() / frame.getRotatedHeight()
        val drawnAspectRatio = if (layoutAspectRatio != 0f) layoutAspectRatio else frameAspectRatio
        val scaleX: Float
        val scaleY: Float
        if (frameAspectRatio > drawnAspectRatio) {
            scaleX = drawnAspectRatio / frameAspectRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = frameAspectRatio / drawnAspectRatio
        }
        drawMatrix.reset()
        drawMatrix.preTranslate(0.5f, 0.5f)
        drawMatrix.preScale(if (mirror) -1f else 1f, 1f)
        drawMatrix.preScale(scaleX, scaleY)
        drawMatrix.preTranslate(-0.5f, -0.5f)

        // Get current surface size so we can set viewport correctly
        val widthArray = IntArray(1)
        EGL14.eglQuerySurface(
            eglCore?.eglDisplay, eglCore?.eglSurface,
            EGL14.EGL_WIDTH, widthArray, 0
        )
        val heightArray = IntArray(1)
        EGL14.eglQuerySurface(
            eglCore?.eglDisplay, eglCore?.eglSurface,
            EGL14.EGL_HEIGHT, heightArray, 0
        )

        // Draw frame
        frameDrawer.drawFrame(frame, 0, 0, widthArray[0], heightArray[0], drawMatrix)
        EGL14.eglSwapBuffers(eglCore?.eglDisplay, eglCore?.eglSurface)

        frame.release()
    }
}
