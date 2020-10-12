package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameBuffer
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VideoFrameBufferAdapterTest {
    @MockK
    private lateinit var mockSDKVideoFrameBuffer: VideoFrameBuffer

    @MockK
    private lateinit var mockMediaVideoFrameBuffer: com.xodee.client.video.VideoFrameBuffer

    private val testWidth = 1
    private val testHeight = 2

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockSDKVideoFrameBuffer.width } returns testWidth
        every { mockSDKVideoFrameBuffer.height } returns testHeight
        every { mockSDKVideoFrameBuffer.retain() } just runs
        every { mockSDKVideoFrameBuffer.release() } just runs

        every { mockMediaVideoFrameBuffer.width } returns testWidth
        every { mockMediaVideoFrameBuffer.height } returns testHeight
        every { mockMediaVideoFrameBuffer.retain() } just runs
        every { mockMediaVideoFrameBuffer.release() } just runs
    }

    @Test
    fun `SDK to Media adapter should pass through all functions`() {
        val adapter = VideoFrameBufferAdapter.SDKToMedia(mockSDKVideoFrameBuffer)
        assertEquals(adapter.width, testWidth)
        assertEquals(adapter.height, testHeight)
        adapter.retain()
        adapter.release()

        verify(exactly = 1) { mockSDKVideoFrameBuffer.retain() }
        verify(exactly = 1) { mockSDKVideoFrameBuffer.release() }
    }

    @Test
    fun `Media to SDK adapter should pass through all functions`() {
        val adapter = VideoFrameBufferAdapter.MediaToSDK(mockMediaVideoFrameBuffer)
        assertEquals(adapter.height, testHeight)
        assertEquals(adapter.width, testWidth)
        adapter.retain()
        adapter.release()

        verify(exactly = 1) { mockMediaVideoFrameBuffer.retain() }
        verify(exactly = 1) { mockMediaVideoFrameBuffer.release() }
    }
}
