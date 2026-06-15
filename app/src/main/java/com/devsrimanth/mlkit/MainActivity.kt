package com.devsrimanth.mlkit

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.devsrimanth.mlkit.faceDetection.FaceDetectionScreen
import com.devsrimanth.mlkit.mediapipeline.CameraScreen
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
            CameraScreen()
        }
    }
}