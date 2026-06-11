package com.devsrimanth.mlkit.tflite

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.get
import androidx.core.graphics.scale

@Composable
fun ObjectDetectionScreen() {
    val context = LocalContext.current

    var userPickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    /**
     * Interpreter — Loads the SSD MobileNet model
     * Reads the ssd.tflite file from assets folder
     * and prepares the TFLite engine
     * Returns: Interpreter (ready to run the model)
     */
    val interpreter = remember {
        val fileDescriptor = context.assets.openFd("ssd.tflite")
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val model: MappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
            Interpreter(model)
        }
    }

    /**
     * Labels — Loads the labels.txt file
     * e.g. ["background", "person", "dog", "cat"...]
     * Returns: List<String> (1000+ object names)
     */
    val labels = remember {
        context.assets.open("labels.txt")
            .bufferedReader()
            .readLines()
    }

    /**
     * ImagePicker — Picks an image from the Gallery
     * Converts URI to Bitmap, then
     * runs: preprocess → inference → parse → draw
     */
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(stream)
            userPickedBitmap = bitmap
            isLoading = true

            val byteBuffer = preprocessBitmap(bitmap!!)
            val rawDetections = runDetection(interpreter, byteBuffer)
            detections = parseDetections(rawDetections, labels, bitmap)
            resultBitmap = drawBoundingBoxes(bitmap, detections)

            isLoading = false
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Object Detection",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
        )
        Text(
            text = "Powered by SSD MobileNet",
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color.Gray
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(androidx.compose.ui.graphics.Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            when {
                resultBitmap != null -> Image(
                    bitmap = resultBitmap!!.asImageBitmap(),
                    contentDescription = "Result",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                userPickedBitmap != null -> Image(
                    bitmap = userPickedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🔍", fontSize = 40.sp)
                    Text(
                        text = "No Image Selected",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF6C63FF)
            )
        ) {
            Text("Pick Image from Gallery", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (isLoading) {
            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF6C63FF))
        }

        if (detections.isNotEmpty()) {
            Text(
                text = "Detected Objects",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
            )
            detections.forEachIndexed { index, detection ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${index + 1}. ${detection.label}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(detection.confidence * 100).toInt()}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFF6C63FF)
                        )
                    }
                }
            }
        }
    }
}

/**
 * preprocessBitmap — Converts Bitmap to ByteBuffer
 * SSD MobileNet requires a 300x300 input size
 * Reads RGB pixels and stores them as bytes (0–255 range)
 * Returns: ByteBuffer (input ready for the model)
 */
private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
    val resized = bitmap.scale(300, 300)
    val byteBuffer = ByteBuffer.allocateDirect(1 * 300 * 300 * 3)
    byteBuffer.order(ByteOrder.nativeOrder())
    for (y in 0 until 300) {
        for (x in 0 until 300) {
            val pixel = resized[x, y]
            byteBuffer.put((pixel shr 16 and 0xFF).toByte()) // R
            byteBuffer.put((pixel shr 8 and 0xFF).toByte())  // G
            byteBuffer.put((pixel and 0xFF).toByte())         // B
        }
    }
    return byteBuffer
}

/**
 * runDetection — Runs the Interpreter
 * Takes ByteBuffer as input and produces 4 output tensors:
 *   [0] → Bounding Boxes   [1,10,4] — 10 objects × 4 coordinates
 *   [1] → Class Indices    [1,10]   — 10 objects × class number
 *   [2] → Confidence       [1,10]   — 10 objects × score
 *   [3] → Total Detections [1]      — number of objects detected
 * Returns: Map<Int, Any> (4 output tensors)
 */
private fun runDetection(interpreter: Interpreter, byteBuffer: ByteBuffer): Map<Int, Any> {
    val boundingBoxes = Array(1) { Array(10) { FloatArray(4) } }
    val classIndices = Array(1) { FloatArray(10) }
    val confidences = Array(1) { FloatArray(10) }
    val numDetections = FloatArray(1)

    val outputs = mapOf(
        0 to boundingBoxes,
        1 to classIndices,
        2 to confidences,
        3 to numDetections
    )

    interpreter.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputs)
    return outputs
}

/**
 * parseDetections — Converts raw output into a Detection list
 * Only keeps objects with confidence > 0.5
 * Converts class index to label name
 * Converts normalized coordinates to actual pixel coordinates
 * Returns: List<Detection> (label + confidence + boundingBox)
 */
private fun parseDetections(
    outputs: Map<Int, Any>,
    labels: List<String>,
    bitmap: Bitmap
): List<Detection> {
    val boundingBoxes = outputs[0] as Array<Array<FloatArray>>
    val classIndices = outputs[1] as Array<FloatArray>
    val confidences = outputs[2] as Array<FloatArray>
    val numDetections = (outputs[3] as FloatArray)[0].toInt()

    val detectionList = mutableListOf<Detection>()

    for (i in 0 until numDetections) {
        val confidence = confidences[0][i]

        // Skip detections with confidence below 50%
        if (confidence < 0.5f) continue

        val classIndex = classIndices[0][i].toInt()
        val label = if (classIndex < labels.size) labels[classIndex] else "Unknown"

        // Convert normalized coordinates (0–1) to actual pixel values
        val box = boundingBoxes[0][i]
        val rect = RectF(
            box[1] * bitmap.width,   // left
            box[0] * bitmap.height,  // top
            box[3] * bitmap.width,   // right
            box[2] * bitmap.height   // bottom
        )

        detectionList.add(Detection(label, confidence, rect))
    }

    return detectionList
}

/**
 * drawBoundingBoxes — Draws detection boxes onto the image
 * Draws a rectangle around each detected object
 * Also displays the label and confidence score as text
 * Returns: Bitmap (image with bounding boxes drawn)
 */
private fun drawBoundingBoxes(bitmap: Bitmap, detections: List<Detection>): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    val textPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
        style = Paint.Style.FILL
    }

    detections.forEach { detection ->
        // Draw bounding box
        canvas.drawRect(detection.boundingBox, boxPaint)

        // Draw label text
        val text = "${detection.label} ${(detection.confidence * 100).toInt()}%"
        canvas.drawText(
            text,
            detection.boundingBox.left,
            detection.boundingBox.top - 10f,
            textPaint
        )
    }

    return result
}

/**
 * Detection — Data class representing a single detected object
 * label       → object name (e.g. "Dog", "Person")
 * confidence  → how confident the model is (e.g. 0.92 = 92%)
 * boundingBox → where the object is located (left, top, right, bottom)
 */
data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)