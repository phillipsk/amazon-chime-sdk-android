package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import android.os.Handler

/**
 * [SurfaceTextureCaptureSourceFactory] is an factory interface for creating new [SurfaceTextureCaptureSource] objects, possible using shared state
 */
interface SurfaceTextureCaptureSourceFactory {
    /**
     * Create a new [SurfaceTextureCaptureSource] object
     *
     * @return [SurfaceTextureCaptureSource] - Newly created and initialized [SurfaceTextureCaptureSource] object
     */
    fun createSurfaceTextureCaptureSource(
        width: Int,
        height: Int,
        contentHint: ContentHint
    ): SurfaceTextureCaptureSource
}