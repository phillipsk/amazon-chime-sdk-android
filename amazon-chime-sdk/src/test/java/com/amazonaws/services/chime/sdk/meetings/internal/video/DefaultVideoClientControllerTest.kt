/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.video

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import com.amazonaws.services.chime.sdk.meetings.TestConstant
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCore
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.VideoSource
import com.amazonaws.services.chime.sdk.meetings.session.MeetingSessionConfiguration
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.video.VideoClient
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import java.security.InvalidParameterException
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

class DefaultVideoClientControllerTest {
    private val messageTopic = "topic"
    private val messageData = "data"
    private val messageLifetimeMs = 3000

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockPackageInfo: PackageInfo

    @MockK
    private lateinit var mockLogger: Logger

    @MockK
    private lateinit var mockVideoClientStateController: VideoClientStateController

    @MockK
    private lateinit var mockVideoClientObserver: VideoClientObserver

    @MockK
    private lateinit var mockConfiguration: MeetingSessionConfiguration

    @MockK
    private lateinit var mockVideoClient: VideoClient

    @MockK
    private lateinit var mockVideoClientFactory: DefaultVideoClientFactory

    @MockK
    private lateinit var mockCameraCaptureSource: CameraCaptureSource

    @MockK
    private lateinit var mockEglCoreFactory: EglCoreFactory

    @MockK(relaxed = true)
    private lateinit var mockEglCore: EglCore

    @InjectMockKs
    private lateinit var testVideoClientController: DefaultVideoClientController

    @MockK
    private lateinit var mockVideoSource: VideoSource

    @Before
    fun setUp() {
        mockkStatic(System::class, Log::class, VideoClient::class)
        every { Log.d(any(), any()) } returns 0
        every { System.loadLibrary(any()) } just runs
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockVideoClient.sendDataMessage(any(), any(), any()) } just runs
        every { mockVideoClient.setExternalVideoSource(any(), any()) } just runs
        every { mockVideoClient.setSending(any()) } just runs
        every { mockVideoClientFactory.getVideoClient(mockVideoClientObserver) } returns mockVideoClient
        every { mockEglCoreFactory.createEglCore() } returns mockEglCore
    }

    @Test
    fun `start should call VideoClientStateController start and create EglCore`() {
        // Run this first
        runBlockingTest {
            testVideoClientController.stopAndDestroy()
        }
        testVideoClientController.start()

        verify { mockEglCoreFactory.createEglCore() }
        verify { mockVideoClientStateController.start() }
    }

    @Test
    fun `stopAndDestroy should call VideoClientStateController stop and release EglCore`() {
        runBlockingTest {
            testVideoClientController.stopAndDestroy()
        }

        coVerify(exactly = 1, timeout = TestConstant.globalScopeTimeoutMs) { mockVideoClientStateController.stop() }
        coVerify { mockEglCore.release() }
    }

    @Test
    fun `startLocalVideo without source should start camera capture and then video client`() {
        every { mockVideoClientStateController.canAct(any()) } returns true

        testVideoClientController.startLocalVideo()

        verify { mockCameraCaptureSource.start() }
        verify { mockVideoClient.setExternalVideoSource(any(), any()) }
        verify { mockVideoClient.setSending(true) }
    }

    @Test
    fun `startLocalVideo with source should not start camera capture and then video client`() {
        every { mockVideoClientStateController.canAct(any()) } returns true

        testVideoClientController.startLocalVideo(mockVideoSource)

        verify(exactly = 0) { mockCameraCaptureSource.start() }
        verify { mockVideoClient.setExternalVideoSource(any(), any()) }
        verify { mockVideoClient.setSending(true) }
    }

    @Test
    fun `stopLocalVideo should stop camera capture source if startLocalVideo was called with no source`() {
        every { mockVideoClientStateController.canAct(any()) } returns true

        testVideoClientController.startLocalVideo()
        testVideoClientController.stopLocalVideo()

        verify { mockCameraCaptureSource.stop() }
        verify { mockVideoClient.setSending(false) }
    }

    @Test
    fun `sendDataMessage should not call videoClient when state is not started`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns false

        testVideoClientController.sendDataMessage(messageTopic, messageData, messageLifetimeMs)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test
    fun `sendDataMessage should call videoClient when state is started`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage(messageTopic, messageData, messageLifetimeMs)

        verify(exactly = 1) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test(expected = InvalidParameterException::class)
    fun `sendDataMessage should throw exception when topic is invalid`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage("!@#$%", messageData, messageLifetimeMs)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test(expected = InvalidParameterException::class)
    fun `sendDataMessage should throw exception when data is oversize`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage(messageTopic, ByteArray(2049), messageLifetimeMs)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }

    @Test(expected = InvalidParameterException::class)
    fun `sendDataMessage should throw exception when lifetime ms is negative`() {
        every { mockVideoClientStateController.canAct(VideoClientState.STARTED) } returns true

        testVideoClientController.sendDataMessage(messageTopic, messageData, -1)

        verify(exactly = 0) { mockVideoClient.sendDataMessage(any(), any(), any()) }
    }
}
