/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo

import com.amazonaws.services.chime.sdk.meetings.audiovideo.metric.MetricsObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.VideoSource

/**
 * [AudioVideoControllerFacade] manages the signaling and peer connections.
 */
interface AudioVideoControllerFacade {
    /**
     * Starts audio and video.
     */
    fun start()

    /**
     * Stops audio and video.
     */
    fun stop()

    /**
     * Subscribe to audio, video, and connection events with an [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to subscribe to events with.
     */
    fun addAudioVideoObserver(observer: AudioVideoObserver)

    /**
     * Unsubscribes from audio, video, and connection events by removing specified [AudioVideoObserver].
     *
     * @param observer: [AudioVideoObserver] - The observer to unsubscribe from events with.
     */
    fun removeAudioVideoObserver(observer: AudioVideoObserver)

    /**
     * Subscribe to metrics events with an [MetricsObserver].
     *
     * @param observer: [MetricsObserver] - The observer to subscribe to events with.
     */
    fun addMetricsObserver(observer: MetricsObserver)

    /**
     * Unsubscribes from metrics by removing specified [MetricsObserver].
     *
     * @param observer: [MetricsObserver] - The observer to unsubscribe from events with.
     */
    fun removeMetricsObserver(observer: MetricsObserver)

    /**
     * Starts sending video for local attendee.  Will internally create a default [CameraCaptureSource] and
     * start, pass to video client. [stopLocalVideo] will stop the internal capture source if being used.
     *
     * Calling this after passing in a custom [VideoSource] will replace it with the internal capture source.
     */
    fun startLocalVideo()

    /**
     * Start local video with a provided custom [VideoSource] which can be used to provide custom
     * [VideoFrame]s to be transmitted to remote clients
     *
     * Calling this function repeatedly will replace the previous [VideoSource] as the one being
     * transmitted.  It will also stop and replace the internal capture source if [startLocalVideo]
     * was called with no arguments.
     *
     * @param source: [VideoSource] - The source of video frames to be sent to other clients
     */
    fun startLocalVideo(source: VideoSource)

    /**
     * Stop local video.
     */
    fun stopLocalVideo()

    /**
     * Start remote video.
     */
    fun startRemoteVideo()

    /**
     * Stop remote video.
     */
    fun stopRemoteVideo()
}
