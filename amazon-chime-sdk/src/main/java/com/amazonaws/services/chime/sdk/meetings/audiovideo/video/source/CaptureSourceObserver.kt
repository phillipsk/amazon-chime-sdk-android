package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

/**
 * [CaptureSourceObserver] observes events resulting from different types of capture devices. Builders
 * may desire this input to decide when to show certain UI elements, or to notify users of failure.
 */
interface CaptureSourceObserver {
    /**
     * Called when the capture source has started successfully and has started emitting frames
     */
    fun onCaptureStarted()

    /**
     * Called when the capture source has stopped when expected.  This may occur when switching cameras, for example.
     */
    fun onCaptureStopped()

    /**
     * Called when the capture source failed permanently
     *
    @param error: [CaptureSourceError] - The reason why the source has stopped.
     */
    fun onCaptureFailed(error: CaptureSourceError)
}
