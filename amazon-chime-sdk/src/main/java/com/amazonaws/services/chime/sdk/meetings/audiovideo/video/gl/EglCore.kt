package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface

/**
 * [EGLCore] is an interface for containing all EGL state in one component.  In the future it may contain additional helper methods.
 */
interface EglCore {
    val eglContext: EGLContext
    var eglSurface: EGLSurface
    val eglDisplay: EGLDisplay
    val eglConfig: EGLConfig

    /**
     * Discards all resources held by this class, notably the EGL context.  This must be
     * called from the thread where the context was created.
     *
     * On completion, no context will be current.
     */
    fun release()
}