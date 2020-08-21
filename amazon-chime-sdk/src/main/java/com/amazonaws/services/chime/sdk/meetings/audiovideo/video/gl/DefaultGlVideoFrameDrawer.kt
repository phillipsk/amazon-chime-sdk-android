package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

/**
 * [DefaultGlVideoFrameDrawer] simply draws the frames as opaque quads onto the current surface
 */
class DefaultGlVideoFrameDrawer : GenericGlVideoFrameDrawer(GENERIC_FRAGMENT_SHADER_BASE) {
    companion object {
        private val GENERIC_FRAGMENT_SHADER_BASE =
                """
                void main() {
                    gl_FragColor = $SAMPLER_NAME($INPUT_TEXTURE_COORDINATE_NAME);
                }
                """
    }
}