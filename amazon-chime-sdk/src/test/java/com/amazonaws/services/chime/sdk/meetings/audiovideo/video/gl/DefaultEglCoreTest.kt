package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrame
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.buffer.VideoFrameTextureBuffer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test

class DefaultEglCoreTest {

    @MockK
    private lateinit var mockDisplay: EGLDisplay

    @MockK
    private lateinit var mockContext: EGLContext

    @MockK
    private lateinit var mockConfig: EGLConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Set up some static functions which need to return valid values

        mockkStatic(EGL14::class)
        every { EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY) } returns mockDisplay
        every { EGL14.eglInitialize(any(), any(), any(), any(), any()) } returns true
        every { EGL14.eglQueryContext(any(), any(), any(), any(), any()) } returns true

        val slot = slot<Array<EGLConfig>>()
        every { EGL14.eglChooseConfig(any(), any(), any(), capture(slot), any(), any(), any(), any()) } answers {
            slot.captured[0] = mockConfig
            true
        }
    }

    @Test
    fun `constructor calls eglInitialize and eglCreateContext`() {
        val testEglCore = DefaultEglCore(Runnable {}, mockContext)

        verify { EGL14.eglInitialize(any(), any(), any(), any(), any()) }
        verify { EGL14.eglCreateContext(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `release calls eglDestroyContext and eglTerminate`() {
        val testEglCore = DefaultEglCore(Runnable {}, mockContext)
        testEglCore.release()

        verify { EGL14.eglDestroyContext(any(), any()) }
        verify { EGL14.eglTerminate(any()) }
    }
}