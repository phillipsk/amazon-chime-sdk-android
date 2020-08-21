package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import android.opengl.EGL14
import android.opengl.EGLContext
import android.os.Handler
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

class DefaultSurfaceTextureCaptureSourceFactory(
    private val logger: Logger,
    public val eglCoreFactory: EglCoreFactory
) : SurfaceTextureCaptureSourceFactory {
    override fun createSurfaceTextureCaptureSource(
        width: Int,
        height: Int,
        contentHint: ContentHint
    ): SurfaceTextureCaptureSource {
        return DefaultSurfaceTextureCaptureSource(logger, width, height, contentHint, eglCoreFactory)
    }
}