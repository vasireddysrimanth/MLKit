package com.devsrimanth.mlkit.tflite

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tensorflow.lite.Interpreter
import androidx.core.graphics.get
import androidx.core.graphics.scale
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@Composable
fun ImageClassificationV2Screen() {
    val context = LocalContext.current

    var userPickedBitMap by remember { mutableStateOf<Bitmap?>(null) }
    var predictions by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val interpreter = remember {
        val fileDescriptor = context.assets.openFd("1.tflite")
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

    val labels = remember {
        context.assets.open("labels.txt")
            .bufferedReader()
            .readLines()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(stream)
            userPickedBitMap = bitmap
            isLoading = true

            // Preprocess into model input buffer
            val inputBuffer = preprocessBitmap(bitmap!!)

            // Run inference with ByteBuffer input
            val scores = runInference(interpreter, inputBuffer)

            predictions = getTop3(scores, labels)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Title
        Text(
            text = "TFLite V2 Classifier",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        // Subtitle
        Text(
            text = "Powered by TensorFlow Lite",
            fontSize = 12.sp,
            color = Color.Gray
        )

        // Image Box
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            if (userPickedBitMap != null) {
                Image(
                    bitmap = userPickedBitMap!!.asImageBitmap(),
                    contentDescription = "Selected Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🖼️", fontSize = 40.sp)
                    Text(
                        text = "No Image Selected",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Pick Button
        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6C63FF)
            )
        ) {
            Text(
                text = "Pick Image from Gallery",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Loading
        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF6C63FF))
        }

        // Predictions
        if (predictions.isNotEmpty()) {
            Text(
                text = "Top 3 Predictions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
            predictions.forEachIndexed { index, (label, confidence) ->
                V2PredictionCard(
                    rank = index + 1,
                    label = label,
                    confidence = confidence
                )
            }
        }
    }
}

@Composable
fun V2PredictionCard(rank: Int, label: String, confidence: Int) {
    val rankColor = when (rank) {
        1 -> Color(0xFF6C63FF)
        2 -> Color(0xFF3ABEF9)
        else -> Color(0xFF5CB85C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(rankColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = label.replaceFirstChar { it.uppercase() },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A2E)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(rankColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$confidence%",
                    color = rankColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Step 3: Resize + normalize image into Float32 ByteBuffer (NHWC)
private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
    val resized = bitmap.scale(128, 128)
    val inputBuffer = ByteBuffer.allocateDirect(1 * 128 * 128 * 3 * 4)
    inputBuffer.order(ByteOrder.nativeOrder())

    for (y in 0 until 128) {
        for (x in 0 until 128) {
            val pixel = resized[x, y]
            inputBuffer.putFloat((pixel shr 16 and 0xFF) / 255f) // R
            inputBuffer.putFloat((pixel shr 8 and 0xFF) / 255f)  // G
            inputBuffer.putFloat((pixel and 0xFF) / 255f)         // B
        }
    }

    inputBuffer.rewind()
    return inputBuffer
}

// Step 4: Inference
private fun runInference(interpreter: Interpreter, inputBuffer: ByteBuffer): FloatArray {
    val output = Array(1) { FloatArray(1001) }
    interpreter.run(inputBuffer, output)
    return output[0]
}

// Step 5: Post Process — same గా ఉంది
private fun getTop3(scores: FloatArray, labels: List<String>): List<Pair<String, Int>> {
    return scores
        .mapIndexed { index, score -> index to score }
        .sortedByDescending { it.second }
        .take(3)
        .map { (index, score) ->
            val label = if (index < labels.size) labels[index] else "Unknown"
            label to (score * 100).toInt()
        }
}