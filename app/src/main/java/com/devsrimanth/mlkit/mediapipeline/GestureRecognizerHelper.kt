package com.devsrimanth.mlkit.mediapipeline

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

/**
 * Helper class that wraps MediaPipe's GestureRecognizer task.
 *
 * GestureRecognizer internally does everything HandLandmarker does
 * (21 landmarks + handedness) PLUS classifies the hand pose into
 * a gesture label like "Thumb_Up", "Victory", "Closed_Fist", etc.
 */
class GestureRecognizerHelper(
    context: Context,
    private val onResult: (GestureRecognizerResult, Int, Int) -> Unit,
    private val onError: (String) -> Unit
) {

    private var gestureRecognizer: GestureRecognizer? = null

    init {
        setupGestureRecognizer(context)
    }

    private fun setupGestureRecognizer(context: Context) {
        try {
            /** Step 1: Point to the gesture model file in assets/ */
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("gesture_recognizer.task")
                .build()

            /** Step 2: Configure options */
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                /** LIVE_STREAM mode -> results arrive asynchronously here */
                .setResultListener { result, inputImage ->
                    onResult(result, inputImage.width, inputImage.height)
                }
                .setErrorListener { error ->
                    onError(error.message ?: "Unknown error")
                }
                .build()

            /** Step 3: Create the recognizer instance */
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)

        } catch (e: Exception) {
            onError("GestureRecognizer setup failed: ${e.message}")
            Log.e("GestureRecognizerHelper", "Setup error", e)
        }
    }

    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    fun clear() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }
}