package com.devsrimanth.mlkit.mediapipeline

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Helper class that wraps MediaPipe's FaceLandmarker task.
 *
 * Detects 478 3D landmarks across the face (eyes, lips, eyebrows,
 * jawline, nose, cheeks, forehead) and returns them per detected face.
 */
class FaceLandmarkerHelper(
    context: Context,
    private val onResult: (FaceLandmarkerResult, Int, Int) -> Unit,
    private val onError: (String) -> Unit
) {

    private var faceLandmarker: FaceLandmarker? = null

    init {
        setupFaceLandmarker(context)
    }

    private fun setupFaceLandmarker(context: Context) {
        try {
            /** Step 1: Point to the face model file in assets/ */
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            /** Step 2: Configure options */
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)                       // detect 1 face
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                /** LIVE_STREAM mode -> results arrive asynchronously here */
                .setResultListener { result, inputImage ->
                    onResult(result, inputImage.width, inputImage.height)
                }
                .setErrorListener { error ->
                    onError(error.message ?: "Unknown error")
                }
                .build()

            /** Step 3: Create the landmarker instance */
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)

        } catch (e: Exception) {
            onError("FaceLandmarker setup failed: ${e.message}")
            Log.e("FaceLandmarkerHelper", "Setup error", e)
        }
    }

    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        faceLandmarker?.detectAsync(mpImage, frameTime)
    }

    fun clear() {
        faceLandmarker?.close()
        faceLandmarker = null
    }
}