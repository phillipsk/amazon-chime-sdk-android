package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameRGBABuffer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class VideoFrameRGBABufferAdapterTest {

    @MockK
    private lateinit var mockSDKVideoFrameBuffer: VideoFrameRGBABuffer

    private val testWidth = 1
    private val testHeight = 2

    private val testData = ByteBuffer.allocate(0)

    private val testStride = 3

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockSDKVideoFrameBuffer.width } returns testWidth
        every { mockSDKVideoFrameBuffer.height } returns testHeight
        every { mockSDKVideoFrameBuffer.retain() } just runs
        every { mockSDKVideoFrameBuffer.release() } just runs
        every { mockSDKVideoFrameBuffer.data } returns testData
        every { mockSDKVideoFrameBuffer.stride } returns testStride
    }

    @Test
    fun `SDK to Media adapter should pass through all functions`() {
        val adapter = VideoFrameRGBABufferAdapter.SDKToMedia(mockSDKVideoFrameBuffer)
        assertEquals(adapter.width, testWidth)
        assertEquals(adapter.height, testHeight)
        assertEquals(adapter.data, testData)
        assertEquals(adapter.stride, testStride)
        adapter.retain()
        adapter.release()

        verify(exactly = 1) { mockSDKVideoFrameBuffer.retain() }
        verify(exactly = 1) { mockSDKVideoFrameBuffer.release() }
    }
}