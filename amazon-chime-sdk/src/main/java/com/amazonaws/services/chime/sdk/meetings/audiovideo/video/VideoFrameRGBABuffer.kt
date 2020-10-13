package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

import java.nio.ByteBuffer

// TODO: Should these buffers need interfaces?
interface VideoFrameRGBABuffer : VideoFrameBuffer {
    /**
     * RGBA plane data of video frame in memory
     */
    val data: ByteBuffer

    /**
     * Stride of RGBA plane of video frame
     */
    val stride: Int
}
