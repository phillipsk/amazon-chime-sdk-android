package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

/**
 * [SurfaceTextureCaptureSourceFactory] is an factory interface for creating new [SurfaceTextureCaptureSource] objects,
 * possible using shared state.  This provides flexibility over use of [SurfaceTextureCaptureSource] objects since
 * they may not allow reuse, or may have a delay before possible reuse.
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
