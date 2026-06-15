package com.devsrimanth.mlkit.mediapipeline

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Helper class that wraps MediaPipe's HandLandmarker task.
 *
 * Responsibilities:
 * - Load the hand_landmarker.task model from assets
 * - Configure detection settings (number of hands, confidence thresholds)
 * - Run inference asynchronously on camera frames (LIVE_STREAM mode)
 * - Return results via callback (onResult) since LIVE_STREAM mode is async
 */
class HandLandmarkerHelper(
    context: Context,
    private val onResult: (HandLandmarkerResult, Int, Int) -> Unit,
    private val onError: (String) -> Unit
) {

    private var handLandmarker: HandLandmarker? = null

    init {
        setupHandLandmarker(context)
    }

    /**
     * Initializes the HandLandmarker with model path and configuration options.
     */
    private fun setupHandLandmarker(context: Context) {
        try {
            /** Step 1: Point to the model file inside assets/ folder */
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            /** Step 2: Configure task-specific options */
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM) // Required for camera feed
                .setNumHands(2)                          // Track up to 2 hands
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                /**
                 * In LIVE_STREAM mode, detectAsync() does not return results directly.
                 * Results arrive here once processing is complete.
                 */
                .setResultListener { result, inputImage ->
                    onResult(result, inputImage.width, inputImage.height)
                }
                .setErrorListener { error ->
                    onError(error.message ?: "Unknown MediaPipe error")
                }
                .build()

            /** Step 3: Create the HandLandmarker instance */
            handLandmarker = HandLandmarker.createFromOptions(context, options)

        } catch (e: Exception) {
            onError("HandLandmarker setup failed: ${e.message}")
            Log.e("HandLandmarkerHelper", "Setup error", e)
        }
    }

    /**
     * Runs hand landmark detection on a single frame.
     * Must be called with a strictly increasing timestamp (frameTime).
     */
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    /**
     * Releases the model from memory. Must be called when no longer needed
     * to avoid memory leaks.
     */
    fun clear() {
        handLandmarker?.close()
        handLandmarker = null
    }
}