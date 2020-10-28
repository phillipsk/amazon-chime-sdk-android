/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

/**
 * A [VideoSink] consumes video frames, typically from a [VideoSource];
 * connected via video [VideoSource.addVideoSink] and disconnected via [VideoSource.removeVideoSink]
 */
interface VideoSink {
    /**
     * Receive a video frame from some upstream source.
     * The [VideoSink] may render, store, process, and forward the frame, among other applications.
     *
     * @param frame: [VideoFrame] - New video frame to consume
     */
    fun onVideoFrameReceived(frame: VideoFrame)
}
