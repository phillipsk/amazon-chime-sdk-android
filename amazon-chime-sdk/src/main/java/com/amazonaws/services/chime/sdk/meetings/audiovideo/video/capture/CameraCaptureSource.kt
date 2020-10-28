package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice

/**
 * [CameraCaptureSource] is an interface for camera capture sources with additional features
 * not covered by [VideoCaptureSource]
 */
interface CameraCaptureSource : VideoCaptureSource {
    /**
     * Current camera device.  This is only null if the phone/device doesn't have any cameras
     * May be called regardless of whether [start] or [stop] has been called.
     */
    var device: MediaDevice?

    /**
     * Toggle for flashlight on the current device.  Will succeed if current device has access to
     * flashlight, otherwise will stay [false].  May be called regardless of whether [start] or [stop]
     * has been called.
     */
    var flashlightEnabled: Boolean

    /**
     * Current camera capture format.
     * May be called regardless of whether [start] or [stop] has been called.
     * */
    var format: VideoCaptureFormat

    /**
     * Helper function to switch from front to back cameras or reverse.  This also switches from
     * any external cameras to the front camera.
     */
    fun switchCamera()
}
