package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * [VideoFrame] is a class which contains a [VideoFrameBuffer] and metadata necessary for transmission
 */
class VideoFrame(
        /**
         * Timestamp in nanoseconds at which the video frame was captured
         */
        val timestampNs: Long,

        /**
         * Object containing actual video frame data in some form
         */
        val buffer: VideoFrameBuffer,

        /**
         * Rotation of the video frame buffer in degrees clockwise
         */
        val rotation: VideoRotation = VideoRotation.Rotation0
) {
    /**
     * Width of the video frame
     *
     * @return [Int] - Frame width in pixels
     */
    val width: Int
        get() = buffer.width

    /**
     * Height of the video frame
     *
     * @return [Int] - Frame height in pixels
     */
    val height: Int
        get() = buffer.height

    /**
     * Width of frame when rotation is removed
     *
     * @return [Int] - Frame width when rotation is removed
     */
    fun getRotatedWidth(): Int {
        return if (rotation.degrees % 180 == 0) {
            buffer.width
        } else {
            buffer.height
        }
    }

    /**
     * Height of frame when rotation is removed
     *
     * @return [Int] - Frame height when rotation is removed
     */
    fun getRotatedHeight(): Int {
        return if (rotation.degrees % 180 == 0) {
            buffer.height
        } else {
            buffer.width
        }
    }

    /**
     * Helper function to call [VideoFrameBuffer.retain] on the owned buffer
     */
    fun retain() = buffer.retain()

    /**
     * Helper function to call [VideoFrameBuffer.release] on the owned buffer
     */
    fun release() = buffer.release()
}