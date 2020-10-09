package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.opengl.EGLContext

/**
 * [EglCoreFactory] is an factory interface for creating new [EglCore] objects, possible using shared state
 */
interface EglCoreFactory {
    /**
     * Shared context which will be used for any [EglCore] objects created by this factory.  Can be used to explicitly
     * share the context without having to make a new [EglCore] object
     */
    val sharedContext: EGLContext

    /**
     * Create a new [EglCore] object
     *
     * @return [EglCore] - Newly created and initialized [EglCore] object
     */
    fun createEglCore(): EglCore
}