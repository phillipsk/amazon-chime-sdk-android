package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoPauseState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileController
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.VideoSource

class LocalRenderAdapter(
    private val videoSource: VideoSource
) : VideoSink {

    private var videoClientTileObservers = mutableSetOf<VideoTileController>()


    init {
        videoSource.addVideoSink(this)
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        for (tileController in videoClientTileObservers) {
            tileController.onReceiveFrame(frame, 0, null, VideoPauseState.Unpaused)
        }
    }

    fun subscribeToLocalVideo(observer: VideoTileController) {
        videoClientTileObservers.add(observer)
    }

    fun unsubscribeToLocalVideo(observer: VideoTileController) {
        videoClientTileObservers.remove(observer)
    }
}