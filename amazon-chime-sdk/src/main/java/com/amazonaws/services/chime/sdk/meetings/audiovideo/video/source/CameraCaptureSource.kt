package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice

/**
 * [CameraCaptureSource] is a interface for camera capture sources with additional features
 * not covered by [VideoCaptureSource\]
 */
interface CameraCaptureSource :  VideoCaptureSource {
    /**
     * Current camera device.  This is only null if the phone/device doesn't have any cameras
     */
    var device: MediaDevice?

    /**
     * Toggle for flashlight on the current device.  Capturer does not need to be started to use.
     */
    var flashlightEnabled: Boolean

    /**
     * Helper function to switch from front to back cameras or reverse.  This also switches from
     * any external cameras to the front camera.
     */
    fun switchCamera()
}