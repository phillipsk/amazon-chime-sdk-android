package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

/**
 * [VideoCaptureFormat] describes a given capture format that may be possible to apply to a [VideoCaptureSource]
 */
data class VideoCaptureFormat(
    /**
     * Capture width
     */
    val width: Int,

    /**
     * Capture height
     */
    val height: Int,

    /**
     * Max FPS.  When used as input this implies the desired FPS as well
     */
    val maxFps: Int
) {
    override fun toString(): String {
        return "$width x $height @ $maxFps FPS"
    }
}
