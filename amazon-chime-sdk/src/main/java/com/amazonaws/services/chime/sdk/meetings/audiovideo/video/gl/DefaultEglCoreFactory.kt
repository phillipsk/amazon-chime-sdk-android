package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.opengl.EGL14
import android.opengl.EGLContext
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.utils.RefCountDelegate

/**
 * [DefaultEglCoreFactory] will create a root [EglCore] lazily if no shared context is provided.
 * It will track all child [EglCore] objects, and if they are all release, will release the root core
 */
class DefaultEglCoreFactory(private var sharedContext: EGLContext = EGL14.EGL_NO_CONTEXT) :
    EglCoreFactory {
    private var rootEglCoreLock = Any()
    private var rootEglCore: EglCore? = null
    private var refCountDelegate: RefCountDelegate? = null

    override fun createEglCore(): EglCore {
        synchronized(rootEglCoreLock) {
            if (rootEglCore == null && sharedContext == EGL14.EGL_NO_CONTEXT) {
                refCountDelegate = RefCountDelegate(Runnable { release() })
                rootEglCore = DefaultEglCore().also {
                    sharedContext = it.eglContext
                }
            } else {
                refCountDelegate?.retain()
            }
            return DefaultEglCore(Runnable { eglCoreReleased() }, sharedContext)
        }
    }

    private fun eglCoreReleased() {
        refCountDelegate?.release()
    }

    private fun release() {
        synchronized(rootEglCoreLock) {
            if (rootEglCore != null) {
                rootEglCore?.release()
                rootEglCore = null
                sharedContext = EGL14.EGL_NO_CONTEXT
                refCountDelegate = null
            }
        }
    }
}