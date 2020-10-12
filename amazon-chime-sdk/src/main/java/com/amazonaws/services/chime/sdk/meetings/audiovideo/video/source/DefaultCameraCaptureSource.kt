package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.hardware.camera2.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.*
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.min

/**
 * [DefaultCameraCaptureSource] will configure a reasonably standard capture stream which will
 * use the [Surface] provided by the capture source provided by a [SurfaceTextureCaptureSourceFactory]
 */
class DefaultCameraCaptureSource(
    private val context: Context,
    private val logger: Logger,
    private val surfaceTextureCaptureSourceFactory: SurfaceTextureCaptureSourceFactory,
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
) : CameraCaptureSource, VideoSink {

    private val handler: Handler

    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCharacteristics: CameraCharacteristics? = null

    private var cameraOrientation = 0
    private var isCameraFrontFacing = false

    private var surfaceTextureSource: SurfaceTextureCaptureSource? = null

    private val observers = mutableSetOf<CaptureSourceObserver>()
    private val sinks = mutableSetOf<VideoSink>()

    override val contentHint = ContentHint.Motion

    private val DESIRED_CAPTURE_FORMAT = VideoCaptureFormat(960, 720, 15)

    private val TAG = "DefaultCameraCaptureSource"

    init {
        val thread = HandlerThread("DefaultCameraCaptureSource")
        thread.start()
        handler = Handler(thread.looper)
    }

    override var device: MediaDevice? = MediaDevice.listVideoDevices(cameraManager)
        .firstOrNull { it.type == MediaDeviceType.VIDEO_FRONT_CAMERA }
        set(value) {
            logger.info(TAG, "Setting capture device: $value")
            if (field == value) {
                logger.info(TAG, "Already using device: $value; ignoring")
                return
            }

            field = value

            // Restart capture if already running (i.e. we have a valid surface texture source)
            surfaceTextureSource?.let {
                stop()
                start()
            }
        }

    override fun switchCamera() {
        val desiredDeviceType = if (device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA) {
            MediaDeviceType.VIDEO_BACK_CAMERA
        } else {
            MediaDeviceType.VIDEO_FRONT_CAMERA
        }
        device =
            MediaDevice.listVideoDevices(cameraManager).firstOrNull { it.type == desiredDeviceType }
    }

    override var flashlightEnabled: Boolean = false
        @RequiresApi(Build.VERSION_CODES.M)
        set(value) {
            if (cameraCharacteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == false) {
                logger.warn(
                    TAG,
                    "Torch not supported on current camera, setting value and returning"
                )
                return
            }

            field = value
            if (cameraDevice == null) {
                // If not in a session, use the CameraManager API
                device?.id?.let { cameraManager.setTorchMode(it, field) }
            } else {
                // Otherwise trigger a new request which will pick up the new value
                createCaptureRequest()
            }
        }
    override var format: VideoCaptureFormat = DESIRED_CAPTURE_FORMAT
        set(value) {
            logger.info(TAG, "Setting capture format: $value")
            if (field == value) {
                logger.info(TAG, "Already using format: $value; ignoring")
                return
            }

            if (value.maxFps > 15) {
                logger.info(TAG, "Limiting capture to 15 FPS to avoid frame drops")
            }
            field = VideoCaptureFormat(value.width, value.height, min(value.maxFps, 15))

            // Restart capture if already running (i.e. we have a valid surface texture source)
            surfaceTextureSource?.let {
                stop()
                start()
            }
        }

    override fun start() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Missing necessary camera permissions")
        }


        logger.info(TAG, "Starting camera capture with device: $device")
        val device = device ?: return

        cameraCharacteristics = cameraManager.getCameraCharacteristics(device.id).also {
            cameraOrientation = it.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            isCameraFrontFacing =
                it.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT
        }

        val chosenCaptureFormat: VideoCaptureFormat? =
            MediaDevice.getSupportedVideoCaptureFormats(cameraManager, device).minBy { format ->
                abs(format.width - this.format.width) + abs(format.height - this.format.height)
            }
        val surfaceTextureFormat: VideoCaptureFormat = chosenCaptureFormat ?: return
        surfaceTextureSource =
            surfaceTextureCaptureSourceFactory.createSurfaceTextureCaptureSource(
                surfaceTextureFormat.width,
                surfaceTextureFormat.height,
                contentHint
            )
        surfaceTextureSource?.start()
        surfaceTextureSource?.addVideoSink(this)

        cameraManager.openCamera(device.id, cameraDeviceStateCallback, handler)
    }

    override fun stop() {
        logger.info(TAG, "Stopping camera capture source")
        val sink: VideoSink = this
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            // Close camera capture session
            cameraCaptureSession?.close()
            cameraCaptureSession = null

            // Close camera device
            cameraDevice?.close()
            cameraDevice = null

            // Stop Surface capture source
            surfaceTextureSource?.removeVideoSink(sink)
            surfaceTextureSource?.stop()
            surfaceTextureSource?.release()
            surfaceTextureSource = null
        }
    }

    override fun onVideoFrameReceived(frame: VideoFrame) {
        val processedBuffer: VideoFrameBuffer = updateBufferForCameraOrientation(
            frame.buffer as VideoFrameTextureBuffer,
            isCameraFrontFacing, -cameraOrientation
        )

        val processedFrame =
            VideoFrame(frame.timestampNs, processedBuffer, getCapturedFrameRotation())
        sinks.forEach { it.onVideoFrameReceived(processedFrame) }
        processedBuffer.release()
    }

    override fun addVideoSink(sink: VideoSink) {
        handler.post { sinks.add(sink) }
    }

    override fun removeVideoSink(sink: VideoSink) {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            sinks.remove(sink)
        }
    }

    override fun addCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.add(observer)
    }

    override fun removeCaptureSourceObserver(observer: CaptureSourceObserver) {
        observers.remove(observer)
    }

    fun release() {
        runBlocking(handler.asCoroutineDispatcher().immediate) {
            logger.info(TAG, "Stopping handler looper")
            handler.removeCallbacksAndMessages(null)
            handler.looper.quit()
        }
    }


    // Implement and store callbacks as private constants since we can't inherit from all of them

    private val cameraCaptureSessionCaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure
            ) {
                logger.error(TAG, "Camera capture session failed: $failure")
                ObserverUtils.notifyObserverOnMainThread(observers) {
                    it.onCaptureFailed(CaptureSourceError.SystemFailure)
                }
            }
        }

    private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            logger.info(
                TAG,
                "Camera capture session configured for session with device ID: ${session.device.id}"
            )
            cameraCaptureSession = session
            createCaptureRequest()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            logger.error(
                TAG, "Camera session configuration failed with device ID: ${session.device.id}"
            )
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.ConfigurationFailure)
            }
            session.close()
        }
    }


    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            logger.info(TAG, "Camera device opened for ID ${device.id}")
            cameraDevice = device
            try {
                cameraDevice?.createCaptureSession(
                    listOf(surfaceTextureSource?.surface),
                    cameraCaptureSessionStateCallback,
                    handler
                )
            } catch (e: CameraAccessException) {
                logger.info(TAG, "Exception encountered creating capture session: ${e.reason}")
                ObserverUtils.notifyObserverOnMainThread(observers) {
                    it.onCaptureFailed(
                        CaptureSourceError.SystemFailure
                    )
                }
                return
            }
        }

        override fun onClosed(device: CameraDevice) {
            logger.info(TAG, "Camera device closed for ID ${device.id}")
            ObserverUtils.notifyObserverOnMainThread(observers) { it.onCaptureStopped() }
        }

        override fun onDisconnected(device: CameraDevice) {
            logger.info(TAG, "Camera device disconnected for ID ${device.id}")
            ObserverUtils.notifyObserverOnMainThread(observers) { it.onCaptureStopped() }
        }

        override fun onError(device: CameraDevice, error: Int) {
            logger.info(TAG, "Camera device encountered error: $error for ID ${device.id}")
            ObserverUtils.notifyObserverOnMainThread(observers) {
                it.onCaptureFailed(CaptureSourceError.SystemFailure)
            }
        }
    }

    private fun createCaptureRequest() {
        val cameraDevice = cameraDevice ?: return
        try {
            val captureRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(0, this.format.maxFps)
            )

            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON
            )
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false)

            if (flashlightEnabled) {
                captureRequestBuilder.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH
                )
            } else {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }

            chooseStabilizationMode(captureRequestBuilder)
            chooseFocusMode(captureRequestBuilder)

            captureRequestBuilder.addTarget(surfaceTextureSource?.surface ?: return)
            cameraCaptureSession?.setRepeatingRequest(
                captureRequestBuilder.build(), cameraCaptureSessionCaptureCallback, handler
            )
            logger.info(
                TAG,
                "Capture request completed with device ID: ${cameraCaptureSession?.device?.id}"
            )
        } catch (e: CameraAccessException) {
            logger.error(
                TAG,
                "Failed to start capture request with device ID: ${cameraCaptureSession?.device?.id}"
            )
            return
        }
    }

    private fun chooseStabilizationMode(captureRequestBuilder: CaptureRequest.Builder) {
        if (cameraCharacteristics?.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
            )?.any { it == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON } == true
        ) {
            captureRequestBuilder[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE] =
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
            captureRequestBuilder[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE] =
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            logger.info(TAG, "Using optical stabilization.")
            return
        }

        // If no optical mode is available, try software.
        if (cameraCharacteristics?.get(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
            )?.any { it == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON } == true
        ) {
            captureRequestBuilder[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE] =
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
            captureRequestBuilder[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE] =
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
            logger.info(TAG, "Using video stabilization.")
            return
        }

        logger.info(TAG, "Stabilization not available.")
    }

    private fun chooseFocusMode(captureRequestBuilder: CaptureRequest.Builder) {
        if (cameraCharacteristics?.get(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
            )?.any { it == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO } == true
        ) {
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            logger.info(TAG, "Using optical stabilization.")
            return
        }

        logger.info(TAG, "Auto-focus is not available.")
    }

    private fun getCapturedFrameRotation(): VideoRotation {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var rotation = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_0 -> 0
            else -> 0
        }
        if (!isCameraFrontFacing) {
            // Account for mirror
            rotation = 360 - rotation
        }
        return VideoRotation.from((cameraOrientation + rotation) % 360) ?: VideoRotation.Rotation0
    }

    private fun updateBufferForCameraOrientation(
        buffer: VideoFrameTextureBuffer, mirror: Boolean, rotation: Int
    ): VideoFrameTextureBuffer {
        val transformMatrix = Matrix()
        // Perform mirror and rotation around (0.5, 0.5) since that is the center of the texture.
        transformMatrix.preTranslate(0.5f, 0.5f)
        if (mirror) {
            transformMatrix.preScale(-1f, 1f)
        }
        transformMatrix.preRotate(rotation.toFloat())
        transformMatrix.preTranslate(-0.5f, -0.5f)

        // The width and height are not affected by rotation since Camera2Session has set them to the
        // value they should be after undoing the rotation.
        val newMatrix = Matrix(buffer.transformMatrix)
        newMatrix.preConcat(transformMatrix)
        buffer.retain()
        return DefaultVideoFrameTextureBuffer(buffer.width, buffer.height,
            buffer.textureId, newMatrix, buffer.type, Runnable { buffer.release() })
    }
}