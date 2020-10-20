package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSource

/**
 * [VideoCaptureSource] is an interface for various video capture sources which can emit [VideoFrame] objects
 */
interface VideoCaptureSource :
    VideoSource {
    /**
     * Start capturing on this source and emitting video frames
     */
    fun start()

    /**
     * Stop capturing on this source and cease emitting video frames
     */
    fun stop()

    /**
     * Add a capture source observer to receive callbacks from the source
     *
     * @param observer: [CaptureSourceObserver] - New observer
     */
    fun addCaptureSourceObserver(observer: CaptureSourceObserver)

    /**
     * Remove a capture source observer
     *
     * @param observer: [CaptureSourceObserver] - Observer to remove
     */
    fun removeCaptureSourceObserver(observer: CaptureSourceObserver)
}