package com.example.dronepilot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class GestureRecognizerClass(val context: Context, val gestureRecognizerListener: GestureRecognizerListener? = null) {

    private var minHandDetectionConfidence: Float = DEFAULT_HAND_DETECTION_CONFIDENCE
    private var minHandTrackingConfidence: Float = DEFAULT_HAND_TRACKING_CONFIDENCE
    private var minHandPresenceConfidence: Float = DEFAULT_HAND_PRESENCE_CONFIDENCE
    private var gestureRecognizer: GestureRecognizer? = null

    data class ResultClass(
        val results: List<GestureRecognizerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    interface GestureRecognizerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultClass)
    }

    init{
        gestureRecognizer()
    }

    fun gestureRecognizer() {
        try {
            val baseOptionBuilder = BaseOptions.builder().setModelAssetPath(MP_RECOGNIZER_TASK)
            val baseOptions = baseOptionBuilder.build()

            val optionsBuilder =
                GestureRecognizer.GestureRecognizerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinHandDetectionConfidence(minHandDetectionConfidence)
                    .setMinTrackingConfidence(minHandTrackingConfidence)
                    .setMinHandPresenceConfidence(minHandPresenceConfidence)
                    .setResultListener(this::returnLiveStreamResult)
                    .setErrorListener(this::returnLivestreamError)
                    .setRunningMode(RunningMode.LIVE_STREAM)

            val options = optionsBuilder.build()
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            gestureRecognizerListener?.onError("Gesture recognizer failed to initialize")
            Log.e(TAG,"MP Task Vision failed to load the task with error: " + e.message)
        } catch (e: RuntimeException) {
            gestureRecognizerListener?.onError("Gesture recognizer failed to initialize")
            Log.e( TAG,"MP Task Vision failed to load the task with error: " + e.message)
        }
    }

    private fun returnLiveStreamResult(result: GestureRecognizerResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        gestureRecognizerListener?.onResults(ResultClass(listOf(result), inferenceTime, input.height, input.width))
    }

    private fun returnLivestreamError(error: RuntimeException) {
        gestureRecognizerListener?.onError(error.message ?: "An unknown error has occurred")
    }

    fun prepareData(imageProxy: ImageProxy){
        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        imageProxy.use {
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        }
        imageProxy.close()

        val matrix = Matrix().apply {
            // Rotate the frame received from the camera to be in the same direction as it'll be shown
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image since we only support front camera
            postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
        }

        // Rotate bitmap to match what our model expects
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer,0,0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            matrix,
            true
        )

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    fun clearGestureRecognizer() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }

    fun isClosed(): Boolean {
        return gestureRecognizer == null
    }

    companion object {
        val TAG = "GestureRecognizerHelper"
        private const val MP_RECOGNIZER_TASK = "D:/UNI/DronePilot/app/src/main/assets/gesture_recognizer.task"

        const val DEFAULT_HAND_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_HAND_PRESENCE_CONFIDENCE = 0.5F
        const val OTHER_ERROR = 0
    }
}