package com.devsrimanth.mlkit.mediapipeline

import android.Manifest
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.*
import androidx.compose.ui.unit.dp
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.Executors

/**
 * Main screen of the app.
 *
 * Flow:
 * 1. Request camera permission
 * 2. Once granted -> show CameraX preview
 * 3. For every camera frame -> run HandLandmarker inference
 * 4. Draw the 21 landmarks + skeleton on top of the camera feed
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    /** Ask for permission once when the screen first appears */
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            HandTrackingCameraView()
        }
        cameraPermissionState.status.shouldShowRationale -> {
            PermissionDeniedScreen(onRequestAgain = {
                cameraPermissionState.launchPermissionRequest()
            })
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Requesting camera permission...")
            }
        }
    }
}

/**
 * Shown when the user has denied permission. Provides a button to retry.
 */
@Composable
private fun PermissionDeniedScreen(onRequestAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Camera permission is required to detect hand landmarks.")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRequestAgain) {
            Text("Grant Permission")
        }
    }
}

/**
 * Main camera + hand tracking view.
 * Combines CameraX preview with a Canvas overlay showing hand landmarks.
 */
@Composable
private fun HandTrackingCameraView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    /** Holds the latest detection result. UI redraws automatically when this changes. */
    var handResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var gestureResult by remember { mutableStateOf<GestureRecognizerResult?>(null) }
    var faceResult by remember { mutableStateOf<FaceLandmarkerResult?>(null) }



    /** Created once and reused for the lifetime of this screen */
    val handLandmarkerHelper = remember {
        HandLandmarkerHelper(
            context = context,
            onResult = { result, _, _ ->
                handResult = result
            },
            onError = { errorMsg ->
                Log.e("HandTracker", errorMsg)
            }
        )
    }

    val gestureRecognizerHelper = remember {
        GestureRecognizerHelper(
            context = context,
            onResult = { result, _, _ -> gestureResult = result },
            onError = { errorMsg -> Log.e("GestureRecognizer", errorMsg) }
        )
    }

    /** Holds the latest face mesh result */
    /** Face landmarker helper - created once */
    val faceLandmarkerHelper = remember {
        FaceLandmarkerHelper(
            context = context,
            onResult = { result, _, _ ->
                faceResult = result
            },
            onError = { errorMsg ->
                Log.e("FaceLandmarker", errorMsg)
            }
        )
    }

    /** Release the model when this screen is removed from composition */
    DisposableEffect(Unit) {
        onDispose {
            handLandmarkerHelper.clear()
            gestureRecognizerHelper.clear()
            faceLandmarkerHelper.clear()
        }
    }

    /** Start the camera once */
    LaunchedEffect(Unit) {
        startCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            handLandmarkerHelper = handLandmarkerHelper,
            gestureRecognizerHelper = gestureRecognizerHelper ,
            faceLandmarkerHelper = faceLandmarkerHelper
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        /** Camera preview (background layer) */
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        /** Hand landmark overlay (foreground layer) */
        handResult?.let { result ->
            if (result.landmarks().isNotEmpty()) {
                HandLandmarkOverlay(
                    result = result,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        /** Simple status label */
        handResult?.let { result ->
            val handLabels = result.handednesses().joinToString(", ") { handednessList ->
                val category = handednessList[0]
                "${category.categoryName()} (${(category.score() * 100).toInt()}%)"
            }

            Text(
                text = if (result.landmarks().isNotEmpty())
                    "Detected: $handLabels"
                else
                    "No hand detected",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
        /** Gesture label (Thumbs Up / Peace / Fist / etc.) */
        gestureResult?.let { result ->
            val gestureText = if (result.gestures().isNotEmpty()) {
                result.gestures().joinToString(", ") { gestureList ->
                    val gesture = gestureList[0] // top prediction
                    val name = when (gesture.categoryName()) {
                        "Thumb_Up" -> "👍 Thumbs Up"
                        "Victory" -> "✌️ Peace Sign"
                        "Closed_Fist" -> "✊ Fist"
                        "Open_Palm" -> "✋ Open Palm"
                        "Thumb_Down" -> "👎 Thumbs Down"
                        "Pointing_Up" -> "☝️ Pointing Up"
                        "ILoveYou" -> "🤟 I Love You"
                        else -> gesture.categoryName()
                    }
                    "$name (${(gesture.score() * 100).toInt()}%)"
                }
            } else {
                "No gesture detected"
            }

            Text(
                text = gestureText,
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp)
            )
        }

        /** Face mesh overlay - 478 points wireframe */
        faceResult?.let { result ->
            if (result.faceLandmarks().isNotEmpty()) {
                FaceMeshOverlay(
                    result = result,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

    }


}

/**
 * Configures and starts the CameraX pipeline:
 * - Preview use case -> shows live feed
 * - ImageAnalysis use case -> sends each frame to HandLandmarkerHelper
 */
private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    handLandmarkerHelper: HandLandmarkerHelper,
    gestureRecognizerHelper: GestureRecognizerHelper ,
    faceLandmarkerHelper: FaceLandmarkerHelper
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        /** Preview use case: shows live camera feed in PreviewView */
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        /** ImageAnalysis use case: receives each frame for processing */
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->

                    /** Convert camera frame (YUV) to Bitmap, then to MPImage */
                    val bitmap = imageProxyToBitmap(imageProxy)
                    val mpImage = BitmapImageBuilder(bitmap).build()

                    /** Timestamp must be strictly increasing for LIVE_STREAM mode */
                    val frameTime = SystemClock.uptimeMillis()

                    handLandmarkerHelper.detectAsync(mpImage, frameTime)
                    gestureRecognizerHelper.detectAsync(mpImage, frameTime)
                    faceLandmarkerHelper.detectAsync(mpImage, frameTime)

                    /** Must close the frame to free the buffer for the next one */
                    imageProxy.close()
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("CameraX", "Camera binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(context))
}