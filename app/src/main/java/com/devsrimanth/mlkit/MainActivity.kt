package com.devsrimanth.mlkit

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.devsrimanth.mlkit.faceDetection.FaceDetectionScreen
import com.devsrimanth.mlkit.tflite.ImageClassificationScreen
import com.devsrimanth.mlkit.tflite.ObjectDetectionScreen
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model = loadModelFile("1.tflite")
            val interpreter = Interpreter(model)

            Log.d("TFLite", "Model loaded successfully! $interpreter")

            // Input shape చూడు
            val inputShape = interpreter.getInputTensor(0).shape()
            Log.d("TFLite", "Input shape: ${inputShape.contentToString()}")

            // Output shape చూడు
            val outputShape = interpreter.getOutputTensor(0).shape()
            Log.d("TFLite", "Output shape: ${outputShape.contentToString()}")

            val labels = this.assets.open("labels.txt")
                .bufferedReader()
                .readLines()

            Log.d("TFLite", "Total labels: ${labels.size}")

            ObjectDetectionScreen()
        }
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(fileName)
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }
}