package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoContentHint
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoSink
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.TimestampAligner
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.*
import kotlinx.coroutines.android.HandlerDispatcher
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test
import java.lang.Runnable

class DefaultSurfaceTextureCaptureSourceTest {
    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockEglCoreFactory: EglCoreFactory

    @MockK(relaxed = true)
    private lateinit var mockEglCore: EglCore

    private lateinit var testSurfaceTextureCaptureSource: DefaultSurfaceTextureCaptureSource

    @MockK
    private lateinit var mockLooper: Looper

    @MockK(relaxed = true)
    private lateinit var mockMatrix: Matrix

    @MockK(relaxed = true)
    private lateinit var mockEglSurface: EGLSurface

    @MockK
    private lateinit var mockVideoSink: VideoSink

    @MockK
    private lateinit var mockSurfaceTexture: SurfaceTexture

    private val testWidth = 1
    private val testHeight = 2
    private val testContentHint = VideoContentHint.Motion

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Setup handler/thread mocks
        mockkConstructor(HandlerThread::class)
        every { anyConstructed<HandlerThread>().looper } returns mockLooper
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().looper } returns mockLooper
        val slot = slot<Runnable>()
        every { anyConstructed<Handler>().post(capture(slot)) } answers {
            slot.captured.run()
            true
        }
        every { mockLooper.quit() } just runs

        every { mockEglCoreFactory.createEglCore() } returns mockEglCore

        mockkStatic(EGL14::class)
        every { EGL14.eglCreatePbufferSurface(any(), any(), any(), any()) } returns mockEglSurface

        mockkConstructor(SurfaceTexture::class)

        mockkConstructor(TimestampAligner::class)
        every { anyConstructed<TimestampAligner>().translateTimestamp(any()) } returns 0

        testSurfaceTextureCaptureSource = DefaultSurfaceTextureCaptureSource(
                mockLogger,
                testWidth,
                testHeight,
                testContentHint,
                mockEglCoreFactory
        )
    }

    @Test
    fun `constructor sets expected dimensions of surface`() {
        val testSurfaceTextureCaptureSource = DefaultSurfaceTextureCaptureSource(
                mockLogger,
                testWidth,
                testHeight,
                testContentHint,
                mockEglCoreFactory
        )

        verify { anyConstructed<SurfaceTexture>().setDefaultBufferSize(testWidth, testHeight) }
    }


    @Test
    fun `start calls setOnFrameAvailableListener with non-null listener`() {
        val slot = slot<SurfaceTexture.OnFrameAvailableListener>()
        every { anyConstructed<SurfaceTexture>().setOnFrameAvailableListener(capture(slot), any()) } just runs

        testSurfaceTextureCaptureSource.start()

        assertNotEquals(slot.captured, null)
    }

    @Test
    fun `stop calls setOnFrameAvailableListener with null listener`() {
        val slot = slot<SurfaceTexture.OnFrameAvailableListener>()
        every { anyConstructed<SurfaceTexture>().setOnFrameAvailableListener(capture(slot)) } just runs

        testSurfaceTextureCaptureSource.stop()

        verify { anyConstructed<SurfaceTexture>().setOnFrameAvailableListener(null) }
    }

    @Test
    fun `surface texture frame is passed along to video sink`() {
        val slot = slot<SurfaceTexture.OnFrameAvailableListener>()
        every { anyConstructed<SurfaceTexture>().setOnFrameAvailableListener(capture(slot), any()) } just runs

        testSurfaceTextureCaptureSource.start()
        testSurfaceTextureCaptureSource.addVideoSink(mockVideoSink)

        slot.captured.onFrameAvailable(mockSurfaceTexture)

        verify { anyConstructed<SurfaceTexture>().updateTexImage() }
        verify { mockVideoSink.onVideoFrameReceived(any()) }
    }

    @Test
    fun `release will release resources if there is no frame in flight`() {
        testSurfaceTextureCaptureSource.release()

        verify { anyConstructed<SurfaceTexture>().release() }
        verify { mockEglCore.release() }
        verify { anyConstructed<TimestampAligner>().dispose() }
        verify { mockLooper.quit() }
    }
}