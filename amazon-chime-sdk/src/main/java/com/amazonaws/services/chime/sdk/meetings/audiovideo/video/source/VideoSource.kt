package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink

/**
 * [VideoSource] is an interface for sources which produce video frames, and can send to a [VideoSink]
 */
interface VideoSource {
    /**
     * Add a video sink which will immediately begin to consume new frames
     *
     * @param sink: [VideoSink] - New video sink
     */
    fun addVideoSink(sink: VideoSink)

    /**
     * Remove a video sink
     *
     * @param sink: [VideoSink] - Video sink to remove
     */
    fun removeVideoSink(sink: VideoSink)

    /**
     * Content hint for downstream processing
     */
    val contentHint: ContentHint
}
