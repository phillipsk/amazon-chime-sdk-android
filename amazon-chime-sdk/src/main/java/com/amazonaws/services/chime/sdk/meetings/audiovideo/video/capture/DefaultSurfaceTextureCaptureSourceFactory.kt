/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

/**
 * [DefaultSurfaceTextureCaptureSourceFactory] creates [DefaultSurfaceTextureCaptureSource] objects
 */
class DefaultSurfaceTextureCaptureSourceFactory(
    private val logger: Logger,
    private val eglCoreFactory: EglCoreFactory
) : SurfaceTextureCaptureSourceFactory {
    override fun createSurfaceTextureCaptureSource(
        width: Int,
        height: Int,
        contentHint: VideoContentHint
    ): SurfaceTextureCaptureSource {
        return DefaultSurfaceTextureCaptureSource(
            logger,
            width,
            height,
            contentHint,
            eglCoreFactory
        )
    }
}
