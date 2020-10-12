package com.amazonaws.services.chime.sdk.meetings.internal.video.adapters

import android.graphics.Matrix
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoFrameTextureBuffer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class VideoFrameTextureBufferAdapterTest {
    @MockK
    private lateinit var mockSDKVideoFrameBuffer: VideoFrameTextureBuffer

    @MockK
    private lateinit var mockMediaVideoFrameBuffer: com.xodee.client.video.VideoFrameTextureBuffer

    private val testWidth = 1
    private val testHeight = 2

    private val testTextureId = 3
    private val testMatrix = Matrix()
    private val testSdkType = VideoFrameTextureBuffer.Type.TEXTURE_2D
    private val testMediaType = com.xodee.client.video.VideoFrameTextureBuffer.Type.RGB

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { mockSDKVideoFrameBuffer.width } returns testWidth
        every { mockSDKVideoFrameBuffer.height } returns testHeight
        every { mockSDKVideoFrameBuffer.textureId } returns testTextureId
        every { mockSDKVideoFrameBuffer.transformMatrix } returns testMatrix
        every { mockSDKVideoFrameBuffer.type } returns testSdkType
        every { mockSDKVideoFrameBuffer.retain() } just runs
        every { mockSDKVideoFrameBuffer.release() } just runs

        every { mockMediaVideoFrameBuffer.width } returns testWidth
        every { mockMediaVideoFrameBuffer.height } returns testHeight
        every { mockMediaVideoFrameBuffer.textureId } returns testTextureId
        every { mockMediaVideoFrameBuffer.transformMatrix } returns testMatrix
        every { mockMediaVideoFrameBuffer.type } returns testMediaType
        every { mockMediaVideoFrameBuffer.retain() } just runs
        every { mockMediaVideoFrameBuffer.release() } just runs
    }

    @Test
    fun `SDK to Media adapter should pass through all functions`() {
        val adapter = VideoFrameTextureBufferAdapter.SDKToMedia(mockSDKVideoFrameBuffer)
        assertEquals(adapter.width, testWidth)
        assertEquals(adapter.height, testHeight)
        assertEquals(adapter.textureId, testTextureId)
        assertEquals(adapter.type, testMediaType)
        adapter.retain()
        adapter.release()

        verify(exactly = 1) { mockSDKVideoFrameBuffer.retain() }
        verify(exactly = 1) { mockSDKVideoFrameBuffer.release() }
    }

    @Test
    fun `Media to SDK adapter should pass through all functions`() {
        val adapter = VideoFrameTextureBufferAdapter.MediaToSDK(mockMediaVideoFrameBuffer)
        assertEquals(adapter.height, testHeight)
        assertEquals(adapter.width, testWidth)
        assertEquals(adapter.textureId, testTextureId)
        assertEquals(adapter.type, testSdkType)
        adapter.retain()
        adapter.release()

        verify(exactly = 1) { mockMediaVideoFrameBuffer.retain() }
        verify(exactly = 1) { mockMediaVideoFrameBuffer.release() }
    }
}