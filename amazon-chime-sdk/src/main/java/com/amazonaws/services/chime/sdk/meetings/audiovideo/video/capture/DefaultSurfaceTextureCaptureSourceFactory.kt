package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.ContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

/**
 * [DefaultSurfaceTextureCaptureSourceFactory] creates [DefaultSurfaceTextureCaptureSource] objects
 */
class DefaultSurfaceTextureCaptureSourceFactory(
    private val logger: Logger,
    private val eglCoreFactory: EglCoreFactory
) : SurfaceTextureCaptureSourceFactory {
    override fun createSurfaceTextureCaptureSource(
        width: Int,
        height: Int,
        contentHint: ContentHint
    ): SurfaceTextureCaptureSource {
        return DefaultSurfaceTextureCaptureSource(
            logger,
            width,
            height,
            contentHint,
            eglCoreFactory
        )
    }
}
