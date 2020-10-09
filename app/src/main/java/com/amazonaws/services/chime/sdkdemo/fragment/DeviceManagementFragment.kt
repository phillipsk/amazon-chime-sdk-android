/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.fragment

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source.VideoCaptureFormat
import com.amazonaws.services.chime.sdk.meetings.device.DeviceChangeObserver
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.R
import com.amazonaws.services.chime.sdkdemo.activity.HomeActivity
import com.amazonaws.services.chime.sdkdemo.activity.MeetingActivity
import com.amazonaws.services.chime.sdkdemo.utils.isLandscapeMode
import kotlinx.coroutines.*
import java.lang.ClassCastException

class DeviceManagementFragment : Fragment(),
    DeviceChangeObserver {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val audioDevices = mutableListOf<MediaDevice>()
    private val videoDevices = mutableListOf<MediaDevice>()
    private val videoFormats = mutableListOf<VideoCaptureFormat>()

    private lateinit var cameraManager: CameraManager

    private lateinit var listener: DeviceManagementEventListener
    private lateinit var audioVideo: AudioVideoFacade
    private lateinit var cameraCaptureSource: CameraCaptureSource
    private lateinit var videoPreview: DefaultVideoRenderView

    private val TAG = "DeviceManagementFragment"

    private lateinit var audioDeviceArrayAdapter: ArrayAdapter<MediaDevice>
    private lateinit var videoDeviceArrayAdapter: ArrayAdapter<MediaDevice>
    private lateinit var videoCaptureFormatArrayAdapter: ArrayAdapter<VideoCaptureFormat>

    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625

    companion object {
        fun newInstance(meetingId: String, name: String): DeviceManagementFragment {
            val fragment = DeviceManagementFragment()

            fragment.arguments =
                Bundle().apply {
                    putString(HomeActivity.MEETING_ID_KEY, meetingId)
                    putString(HomeActivity.NAME_KEY, name)
                }
            return fragment
        }
    }

    interface DeviceManagementEventListener {
        fun onJoinMeetingClicked()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is DeviceManagementEventListener) {
            listener = context
        } else {
            logger.error(TAG, "$context must implement DeviceManagementEventListener.")
            throw ClassCastException("$context must implement DeviceManagementEventListener.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_device_management, container, false)
        val context = activity as Context

        val meetingId = arguments?.getString(HomeActivity.MEETING_ID_KEY)
        val name = arguments?.getString(HomeActivity.NAME_KEY)
        audioVideo = (activity as MeetingActivity).getAudioVideo()

        val displayedText = getString(R.string.preview_meeting_info, meetingId, name)
        view.findViewById<TextView>(R.id.textViewMeetingPreview)?.text = displayedText

        view.findViewById<Button>(R.id.buttonJoin)?.setOnClickListener {
            listener.onJoinMeetingClicked()
        }

        val spinnerAudioDevice = view.findViewById<Spinner>(R.id.spinnerAudioDevice)
        audioDeviceArrayAdapter = createMediaDeviceSpinnerAdapter(context, audioDevices)
        spinnerAudioDevice.adapter = audioDeviceArrayAdapter
        spinnerAudioDevice.onItemSelectedListener = onAudioDeviceSelected

        val spinnerVideoDevice = view.findViewById<Spinner>(R.id.spinnerVideoDevice)
        videoDeviceArrayAdapter = createMediaDeviceSpinnerAdapter(context, videoDevices)
        spinnerVideoDevice.adapter = videoDeviceArrayAdapter
        spinnerVideoDevice.onItemSelectedListener = onVideoDeviceSelected

        val spinnerVideoFormat = view.findViewById<Spinner>(R.id.spinnerVideoFormat)
        videoCaptureFormatArrayAdapter =
                createVideoCaptureFormatSpinnerAdapter(context, videoFormats)
        spinnerVideoFormat.adapter = videoCaptureFormatArrayAdapter
        spinnerVideoFormat.onItemSelectedListener = onVideoFormatSelected

        audioVideo.addDeviceChangeObserver(this)

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraCaptureSource = (activity as MeetingActivity).getCameraCaptureSource()
        view.findViewById<DefaultVideoRenderView>(R.id.videoPreview)?.let{
            val displayMetrics = context.resources.displayMetrics
            val width = if (isLandscapeMode(context) == true) displayMetrics.widthPixels / 2 else displayMetrics.widthPixels
            val height = (width * VIDEO_ASPECT_RATIO_16_9).toInt()
            it.layoutParams.width = width
            it.layoutParams.height = height

            it.init((activity as MeetingActivity).getEglCoreFactory())
            cameraCaptureSource.addVideoSink(it)
            videoPreview = it
        }


        uiScope.launch {
            populateAudioDeviceList(listAudioDevices())
            populateVideoDeviceList(listVideoDevices())
            populateVideoFormatList(listVideoFormats())

            videoPreview.mirror = cameraCaptureSource.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA
            cameraCaptureSource.start()
        }


        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraCaptureSource.stop()
        cameraCaptureSource.removeVideoSink(videoPreview)
        videoPreview.release()
    }

    private val onAudioDeviceSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            audioVideo.chooseAudioDevice(parent?.getItemAtPosition(position) as MediaDevice)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private val onVideoDeviceSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            cameraCaptureSource.device = parent?.getItemAtPosition(position) as MediaDevice

            videoPreview.mirror = cameraCaptureSource.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private val onVideoFormatSelected = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            cameraCaptureSource.format = parent?.getItemAtPosition(position) as VideoCaptureFormat
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private fun populateAudioDeviceList(freshAudioDeviceList: List<MediaDevice>) {
        audioDevices.clear()
        audioDevices.addAll(
            freshAudioDeviceList.filter {
                it.type != MediaDeviceType.OTHER
            }.sortedBy { it.order }
        )
        audioDeviceArrayAdapter.notifyDataSetChanged()
        if (audioDevices.isNotEmpty()) {
            audioVideo.chooseAudioDevice(audioDevices[0])
        }
    }

    private fun populateVideoDeviceList(freshVideoDeviceList: List<MediaDevice>) {
        videoDevices.clear()
        videoDevices.addAll(
            freshVideoDeviceList.filter {
                it.type != MediaDeviceType.OTHER
            }.sortedBy { it.order }
        )
        videoDeviceArrayAdapter.notifyDataSetChanged()
        if (videoDevices.isNotEmpty()) {
            cameraCaptureSource.device = videoDevices[0]
        }
    }

    private fun populateVideoFormatList(freshVideoCaptureFormatList: List<VideoCaptureFormat>) {
        videoFormats.clear()

        val filteredFormats = freshVideoCaptureFormatList.filter { it.height <= 800 }

        for (format in filteredFormats) {
            videoFormats.add(format)
            // Add additional 15 FPS option
            videoFormats.add(VideoCaptureFormat(format.width, format.height, 15))
        }
        videoCaptureFormatArrayAdapter.notifyDataSetChanged()
        if (videoFormats.isNotEmpty()) {
            cameraCaptureSource.format = videoFormats[0]
        }
    }


    private suspend fun listAudioDevices(): List<MediaDevice> {
        return withContext(Dispatchers.Default) {
            audioVideo.listAudioDevices()
        }
    }

    private suspend fun listVideoDevices(): List<MediaDevice> {
        return withContext(Dispatchers.Default) {
            audioVideo.listVideoDevices()
        }
    }

    private suspend fun listVideoFormats(): List<VideoCaptureFormat> {
        return withContext(Dispatchers.Default) {
            val device = cameraCaptureSource.device ?: return@withContext emptyList<VideoCaptureFormat>()
            MediaDevice.getSupportedVideoCaptureFormats(cameraManager, device)
        }
    }

    private fun createMediaDeviceSpinnerAdapter(
        context: Context,
        list: List<MediaDevice>
    ): ArrayAdapter<MediaDevice> {
        return ArrayAdapter(context, android.R.layout.simple_spinner_item, list)
    }

    private fun createVideoCaptureFormatSpinnerAdapter(
            context: Context,
            list: List<VideoCaptureFormat>
    ): ArrayAdapter<VideoCaptureFormat> {
        return ArrayAdapter(context, android.R.layout.simple_spinner_item, list)
    }

    override fun onAudioDeviceChanged(freshAudioDeviceList: List<MediaDevice>) {
        populateAudioDeviceList(freshAudioDeviceList)
    }

    override fun onVideoDeviceChanged(freshVideoDeviceList: List<MediaDevice>) {
        populateVideoDeviceList(freshVideoDeviceList)
    }
}
