/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLExt

class DefaultEglCore(
    private val releaseCallback: Runnable? = null,
    sharedContext: EGLContext = EGL14.EGL_NO_CONTEXT
) : EglCore {
    override var eglContext = EGL14.EGL_NO_CONTEXT
    override var eglSurface = EGL14.EGL_NO_SURFACE
    override var eglDisplay = EGL14.EGL_NO_DISPLAY
    override lateinit var eglConfig: EGLConfig

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = null
            throw RuntimeException("Unable to initialize EGL14")
        }

        getConfig()?.also {
            val attributeList = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            val context = EGL14.eglCreateContext(
                eglDisplay, it, sharedContext,
                attributeList, 0
            )
            eglConfig = it
            eglContext = context
        }

        // Confirm with query.
        val values = IntArray(1)
        if (!EGL14.eglQueryContext(
                eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0
            )
        ) {
            throw RuntimeException("Failed to query context")
        }
    }

    override fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT

        releaseCallback?.run()
    }

    private fun getConfig(): EGLConfig? {
        var renderableType = EGL14.EGL_OPENGL_ES2_BIT
        renderableType = renderableType or EGLExt.EGL_OPENGL_ES3_BIT_KHR

        val attributeList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderableType,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE, 0,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attributeList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            return null
        }
        return configs[0]
    }
}
