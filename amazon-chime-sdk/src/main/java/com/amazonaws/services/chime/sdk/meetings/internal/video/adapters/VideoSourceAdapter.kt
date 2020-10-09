package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.*
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.ContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.VideoSource

/**
 * [VideoSourceAdapter] provides two classes to adapt [VideoSource] to
 * [com.xodee.client.video.VideoSource].  Reverse will never occur.
 */
class VideoSourceAdapter(private val source: VideoSource) : VideoSink, com.xodee.client.video.VideoSource {
    private var sinks = mutableSetOf<com.xodee.client.video.VideoSink>()

    init {
        source.addVideoSink(this)
    }

    override fun addSink(sink: com.xodee.client.video.VideoSink) {
        sinks.add(sink)
    }

    override fun removeSink(sink: com.xodee.client.video.VideoSink) {
        sinks.remove(sink)
    }

    override fun getContentHint(): com.xodee.client.video.ContentHint {
        return when(source.contentHint) {
            ContentHint.None -> com.xodee.client.video.ContentHint.NONE
            ContentHint.Motion -> com.xodee.client.video.ContentHint.MOTION
            ContentHint.Detail -> com.xodee.client.video.ContentHint.DETAIL
            ContentHint.Text -> com.xodee.client.video.ContentHint.TEXT
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        val buffer = when (frame.buffer) {
            is VideoFrameTextureBuffer -> VideoFrameTextureBufferAdapter.SDKToMedia(frame.buffer)
            is VideoFrameI420Buffer -> VideoFrameI420BufferAdapter.SDKToMedia(frame.buffer)
            is VideoFrameRGBABuffer -> VideoFrameRGBABufferAdapter.SDKToMedia(frame.buffer)
            else -> VideoFrameBufferAdapter.SDKToMedia(frame.buffer)
        }

        val videoClientFrame = com.xodee.client.video.VideoFrame(
            frame.width, frame.height, frame.timestampNs, frame.rotation, buffer)
        sinks.forEach { it.onFrameCaptured(videoClientFrame) }
    }
}