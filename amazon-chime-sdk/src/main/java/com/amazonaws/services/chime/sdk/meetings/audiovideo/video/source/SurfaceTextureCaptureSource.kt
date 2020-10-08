package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import android.view.Surface

/**
 * [SurfaceTextureCaptureSource] provides a [Surface] which can be passed to system sources like the camera.
 * It will listen to the surface and emit any new images as [VideoFrame] objects to downstream [VideoSink]
 */
interface SurfaceTextureCaptureSource : VideoCaptureSource {
    /**
     * [Surface] from which any buffers submitted to will be emitted as a [VideoFrame]
     */
    val surface: Surface

    /**
     * Deallocate any state or resources held by this object
     */
    fun release()
}