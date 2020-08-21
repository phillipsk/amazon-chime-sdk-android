package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import java.nio.ByteBuffer

interface VideoFrameRGBABuffer: VideoFrameBuffer {
    /**
     * Y plane data of video frame in memory
     */
    val data: ByteBuffer

    /**
     * Stride of Y plane of video frame
     */
    val stride: Int
}