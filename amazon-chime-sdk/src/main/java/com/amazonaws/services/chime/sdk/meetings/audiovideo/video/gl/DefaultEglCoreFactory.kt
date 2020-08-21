package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.opengl.EGL14
import android.opengl.EGLContext

class DefaultEglCoreFactory(override var sharedContext: EGLContext = EGL14.EGL_NO_CONTEXT) :
    EglCoreFactory {
    var rootEglCore: EglCore? = null

    init {
        if (sharedContext == EGL14.EGL_NO_CONTEXT) {
            rootEglCore = DefaultEglCore().also {
                sharedContext = it.eglContext
            }
        }
    }

    override fun release() {
        rootEglCore?.release()
    }

    override fun createEglCore(): EglCore {
        return DefaultEglCore(sharedContext)
    }
}