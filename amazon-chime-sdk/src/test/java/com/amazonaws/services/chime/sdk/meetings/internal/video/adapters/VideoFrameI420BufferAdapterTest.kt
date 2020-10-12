package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameI420Buffer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class VideoFrameI420BufferAdapterTest {

    @MockK
    private lateinit var mockSDKVideoFrameBuffer: VideoFrameI420Buffer

    @MockK
    private lateinit var mockMediaVideoFrameBuffer: com.xodee.client.video.VideoFrameI420Buffer

    private val testWidth = 1
    private val testHeight = 2

    private val testDataY = ByteBuffer.allocate(0)
    private val testDataU = ByteBuffer.allocate(0)
    private val testDataV = ByteBuffer.allocate(0)

    private val testStrideY = 3
    private val testStrideU = 4
    private val testStrideV = 5

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockSDKVideoFrameBuffer.width } returns testWidth
        every { mockSDKVideoFrameBuffer.height } returns testHeight
        every { mockSDKVideoFrameBuffer.retain() } just runs
        every { mockSDKVideoFrameBuffer.release() } just runs
        every { mockSDKVideoFrameBuffer.dataY } returns testDataY
        every { mockSDKVideoFrameBuffer.dataU } returns testDataU
        every { mockSDKVideoFrameBuffer.dataV } returns testDataV
        every { mockSDKVideoFrameBuffer.strideY } returns testStrideY
        every { mockSDKVideoFrameBuffer.strideU } returns testStrideU
        every { mockSDKVideoFrameBuffer.strideV } returns testStrideV

        every { mockMediaVideoFrameBuffer.width } returns testWidth
        every { mockMediaVideoFrameBuffer.height } returns testHeight
        every { mockMediaVideoFrameBuffer.retain() } just runs
        every { mockMediaVideoFrameBuffer.release() } just runs
        every { mockMediaVideoFrameBuffer.dataY } returns testDataY
        every { mockMediaVideoFrameBuffer.dataU } returns testDataU
        every { mockMediaVideoFrameBuffer.dataV } returns testDataV
        every { mockMediaVideoFrameBuffer.strideY } returns testStrideY
        every { mockMediaVideoFrameBuffer.strideU } returns testStrideU
        every { mockMediaVideoFrameBuffer.strideV } returns testStrideV
    }

    @Test
    fun `SDK to Media adapter should pass through all functions`() {
        val adapter = VideoFrameI420BufferAdapter.SDKToMedia(mockSDKVideoFrameBuffer)
        assertEquals(adapter.width, testWidth)
        assertEquals(adapter.height, testHeight)
        assertEquals(adapter.dataY, testDataY)
        assertEquals(adapter.dataU, testDataU)
        assertEquals(adapter.dataV, testDataV)
        assertEquals(adapter.strideY, testStrideY)
        assertEquals(adapter.strideU, testStrideU)
        assertEquals(adapter.strideV, testStrideV)
        adapter.retain()
        adapter.release()

        verify(exactly = 1) { mockSDKVideoFrameBuffer.retain() }
        verify(exactly = 1) { mockSDKVideoFrameBuffer.release() }
    }

    @Test
    fun `Media to SDK adapter should pass through all functions`() {
        val adapter = VideoFrameI420BufferAdapter.MediaToSDK(mockMediaVideoFrameBuffer)
        assertEquals(adapter.height, testHeight)
        assertEquals(adapter.width, testWidth)
        assertEquals(adapter.dataY, testDataY)
        assertEquals(adapter.dataU, testDataU)
        assertEquals(adapter.dataV, testDataV)
        assertEquals(adapter.strideY, testStrideY)
        assertEquals(adapter.strideU, testStrideU)
        assertEquals(adapter.strideV, testStrideV)
        adapter.retain()
        adapter.release()

        verify(exactly = 1) { mockMediaVideoFrameBuffer.retain() }
        verify(exactly = 1) { mockMediaVideoFrameBuffer.release() }
    }
}