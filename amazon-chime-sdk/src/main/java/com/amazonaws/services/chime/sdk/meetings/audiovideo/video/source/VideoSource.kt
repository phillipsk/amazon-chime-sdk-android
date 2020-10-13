package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink

/**
 * [VideoSource] is an interface for sources which produce video frames, and can send to a [VideoSink].
 * Implementations can be passed to the [AudioVideoFacade] to be used as the video source sent to remote
 * participlants
 */
interface VideoSource {
    /**
     * Add a video sink which will immediately begin to receive new frames
     *
     * @param sink: [VideoSink] - New video sink
     */
    fun addVideoSink(sink: VideoSink)

    /**
     * Remove a video sink which will no longer receive new frames on return
     *
     * @param sink: [VideoSink] - Video sink to remove
     */
    fun removeVideoSink(sink: VideoSink)

    /**
     * Content hint for downstream processing
     */
    val contentHint: ContentHint
}
