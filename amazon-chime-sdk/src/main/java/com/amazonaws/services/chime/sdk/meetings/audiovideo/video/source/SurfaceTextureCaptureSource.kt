package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import android.os.Handler
import android.view.Surface

interface SurfaceTextureCaptureSource : VideoCaptureSource {
    /**
     * [Surface] from which any buffers submitted to will be emitted as a video frame
     */
    val surface: Surface

    /**
     * Deallocate any state or resources held by this object
     */
    fun release()
}