package com.devsrimanth.mlkit.faceDetection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import androidx.camera.core.Preview as CameraXPreview

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

sealed class DetectionState {
    object Idle : DetectionState()
    object Loading : DetectionState()
    data class Success(val faces: List<Face>) : DetectionState()
    data class Error(val message: String) : DetectionState()
}

sealed class InputMode {
    object None : InputMode()
    object Image : InputMode()
    object Camera : InputMode()
}

@Composable
fun FaceDetectionScreen() {
    FaceDetectionData()
}

@Composable
fun FaceDetectionData() {

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    var detectionState by remember { mutableStateOf<DetectionState>(DetectionState.Idle) }
    var inputMode by remember { mutableStateOf<InputMode>(InputMode.None) }
    var liveFaces by remember { mutableStateOf<List<Face>>(emptyList()) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            imageUri = null
            inputMode = InputMode.Camera
            detectionState = DetectionState.Idle
            liveFaces = emptyList()
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { image ->
        image?.let {
            imageUri = it
            inputMode = InputMode.Image
            detectionState = DetectionState.Idle
            liveFaces = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray)
                .clickable {
                    if (inputMode != InputMode.Camera) imageLauncher.launch("image/*")
                }
        ) {
            when (inputMode) {
                is InputMode.Camera -> {
                    CameraPreviewWithOverlay(
                        faces = liveFaces,
                        onFacesDetected = { liveFaces = it }
                    )
                }
                else -> {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .height(220.dp)
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .background(Color.DarkGray, shape = RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    imageUri = null
                    inputMode = InputMode.Image
                    detectionState = DetectionState.Idle
                    liveFaces = emptyList()
                    imageLauncher.launch("image/*")
                }
            ) {
                Text(
                    "Select Image",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.Black
                )
            }

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .background(Color.DarkGray, shape = RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        imageUri = null
                        inputMode = InputMode.Camera
                        detectionState = DetectionState.Idle
                        liveFaces = emptyList()
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }
            ) {
                Text(
                    "Launch Camera",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .background(Color.Blue, shape = RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            onClick = {
                when (inputMode) {
                    is InputMode.Camera -> {
                        if (liveFaces.isEmpty()) {
                            detectionState = DetectionState.Error("No faces detected in camera yet")
                        } else {
                            detectionState = DetectionState.Success(liveFaces)
                        }
                    }
                    is InputMode.Image -> {
                        imageUri?.let { uri ->
                            detectionState = DetectionState.Loading
                            try {
                                val userInputImage = InputImage.fromFilePath(context, uri)
                                runFaceDetection(userInputImage) { state ->
                                    detectionState = state
                                }
                            } catch (e: Exception) {
                                detectionState = DetectionState.Error(
                                    e.message ?: "Failed to load image"
                                )
                            }
                        } ?: run {
                            detectionState = DetectionState.Error("Please select an image first")
                        }
                    }
                    is InputMode.None -> {
                        detectionState = DetectionState.Error("Please select an image or launch camera first")
                    }
                }
            }
        ) {
            Text(
                "Detect Faces",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = detectionState) {
            is DetectionState.Idle -> { }
            is DetectionState.Loading -> {
                LoadingCard()
            }
            is DetectionState.Success -> {
                if (state.faces.isEmpty()) {
                    ErrorCard("No faces detected in the selected image.")
                } else {
                    state.faces.forEachIndexed { index, face ->
                        FaceDataItem(faceIndex = index + 1, face = face)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            is DetectionState.Error -> {
                ErrorCard(state.message)
            }
        }
    }
}

@Composable
fun CameraPreviewWithOverlay(
    faces: List<Face>,
    onFacesDetected: (List<Face>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {

        AndroidView(
            factory = { PreviewView(it) },
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()

                val preview = CameraXPreview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                    val mediaImage = proxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        runFaceDetection(image) { state ->
                            if (state is DetectionState.Success) onFacesDetected(state.faces)
                        }
                    }
                    proxy.close()
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(context))
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            faces.forEach { face ->
                val b = face.boundingBox
                drawRect(
                    color = Color(0xFF00E676),
                    topLeft = Offset(b.left.toFloat(), b.top.toFloat()),
                    size = Size(b.width().toFloat(), b.height().toFloat()),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

fun runFaceDetection(image: InputImage, onResult: (DetectionState) -> Unit) {
    val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .enableTracking()
        .build()

    val detector = FaceDetection.getClient(highAccuracyOpts)

    detector.process(image)
        .addOnSuccessListener { faces ->
            onResult(DetectionState.Success(faces))
        }
        .addOnFailureListener { e ->
            onResult(DetectionState.Error(e.message ?: "Face detection failed"))
        }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = "⏳ Detecting faces...",
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp,
            color = Color(0xFF555555)
        )
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "❌ Error",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFFB71C1C)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFFC62828)
            )
        }
    }
}

@Composable
fun FaceDataItem(faceIndex: Int, face: Face) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Face #$faceIndex",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1A1A1A)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))

            val bounds = face.boundingBox
            FaceDataRow("Bounding Box", "left=${bounds.left}, top=${bounds.top}, right=${bounds.right}, bottom=${bounds.bottom}")
            FaceDataRow("Head Rotation Y (left/right)", "%.2f°".format(face.headEulerAngleY))
            FaceDataRow("Head Rotation Z (tilt)", "%.2f°".format(face.headEulerAngleZ))
            FaceDataRow("Head Rotation X (up/down)", "%.2f°".format(face.headEulerAngleX))

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Landmarks", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(4.dp))

            val landmarks = listOf(
                FaceLandmark.LEFT_EAR to "Left Ear",
                FaceLandmark.RIGHT_EAR to "Right Ear",
                FaceLandmark.LEFT_EYE to "Left Eye",
                FaceLandmark.RIGHT_EYE to "Right Eye",
                FaceLandmark.LEFT_CHEEK to "Left Cheek",
                FaceLandmark.RIGHT_CHEEK to "Right Cheek",
                FaceLandmark.MOUTH_LEFT to "Mouth Left",
                FaceLandmark.MOUTH_RIGHT to "Mouth Right",
                FaceLandmark.MOUTH_BOTTOM to "Mouth Bottom",
                FaceLandmark.NOSE_BASE to "Nose Base",
            )
            landmarks.forEach { (type, label) ->
                val landmark = face.getLandmark(type)
                val value = landmark?.position?.let { "(%.1f, %.1f)".format(it.x, it.y) } ?: "Not detected"
                FaceDataRow(label, value)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Contours (point count)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(4.dp))

            val contours = listOf(
                FaceContour.FACE to "Face",
                FaceContour.LEFT_EYE to "Left Eye",
                FaceContour.RIGHT_EYE to "Right Eye",
                FaceContour.LEFT_EYEBROW_TOP to "Left Eyebrow Top",
                FaceContour.LEFT_EYEBROW_BOTTOM to "Left Eyebrow Bottom",
                FaceContour.RIGHT_EYEBROW_TOP to "Right Eyebrow Top",
                FaceContour.RIGHT_EYEBROW_BOTTOM to "Right Eyebrow Bottom",
                FaceContour.UPPER_LIP_TOP to "Upper Lip Top",
                FaceContour.UPPER_LIP_BOTTOM to "Upper Lip Bottom",
                FaceContour.LOWER_LIP_TOP to "Lower Lip Top",
                FaceContour.LOWER_LIP_BOTTOM to "Lower Lip Bottom",
                FaceContour.NOSE_BRIDGE to "Nose Bridge",
                FaceContour.NOSE_BOTTOM to "Nose Bottom",
                FaceContour.LEFT_CHEEK to "Left Cheek",
                FaceContour.RIGHT_CHEEK to "Right Cheek",
            )
            contours.forEach { (type, label) ->
                val count = face.getContour(type)?.points?.size ?: 0
                FaceDataRow(label, "$count points")
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Classifications", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(4.dp))

            face.smilingProbability?.let {
                FaceDataRow("Smiling Probability", "%.1f%%".format(it * 100))
            } ?: FaceDataRow("Smiling Probability", "Not available")

            face.leftEyeOpenProbability?.let {
                FaceDataRow("Left Eye Open Probability", "%.1f%%".format(it * 100))
            } ?: FaceDataRow("Left Eye Open Probability", "Not available")

            face.rightEyeOpenProbability?.let {
                FaceDataRow("Right Eye Open Probability", "%.1f%%".format(it * 100))
            } ?: FaceDataRow("Right Eye Open Probability", "Not available")

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(8.dp))

            face.trackingId?.let {
                FaceDataRow("Tracking ID", "$it")
            } ?: FaceDataRow("Tracking ID", "Not available")
        }
    }
}

@Composable
fun FaceDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Preview
@Composable
fun FaceDetectionPreview() {
    FaceDetectionScreen()
}