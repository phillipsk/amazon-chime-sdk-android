package com.amazonaws.services.chime.sdkdemo.utils

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.GlUtil
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.GlVideoFrameDrawer

import android.graphics.Matrix
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameI420Buffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameTextureBuffer
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.GenericGlVideoFrameDrawer
import com.xodee.client.video.YuvUtil
import java.nio.ByteBuffer
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * [BlackAndWhiteGlVideoFrameDrawer] simply draws the frames as opaque quads onto the current surface
 */
class BlackAndWhiteGlVideoFrameDrawer : GenericGlVideoFrameDrawer(GENERIC_FRAGMENT_SHADER) {
    companion object {
        private val GENERIC_FRAGMENT_SHADER =
                """
                void main() {
                    vec4 Color = $SAMPLER_NAME($INPUT_TEXTURE_COORDINATE_NAME);
                    gl_FragColor = vec4(vec3(Color.r + Color.g + Color.b) / 3.0, Color.a);
                }
                """

    }
}