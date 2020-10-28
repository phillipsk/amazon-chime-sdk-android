/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video

enum class VideoRotation(val degrees: Int) {
    /**
     * Not rotated
     */
    Rotation0(0),

    /**
     * Rotated 90 degrees clockwise
     */
    Rotation90(90),

    /**
     * Rotated 180 degrees clockwise
     */
    Rotation180(180),

    /**
     * Rotated 270 degrees clockwise
     */
    Rotation270(270);

    companion object {
        fun from(intValue: Int): VideoRotation? = values().find { it.degrees == intValue }
    }
}
