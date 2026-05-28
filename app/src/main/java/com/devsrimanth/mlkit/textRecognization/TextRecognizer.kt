package com.devsrimanth.mlkit.textRecognization

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

/**
 * Created by Srimanth on 26 May 2026.
 * Text Recognition using ML Kit + Live Camera Preview (inline fixed-height)
 * Supports: Latin, Chinese, Devanagari, Japanese, Korean
 */
fun buildRecognizer(language: RecognitionLanguage): TextRecognizer =
    when (language) {
        RecognitionLanguage.LATIN ->
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        RecognitionLanguage.CHINESE ->
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

        RecognitionLanguage.DEVANAGARI ->
            TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

        RecognitionLanguage.JAPANESE ->
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

        RecognitionLanguage.KOREAN ->
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }


@Composable
fun TextRecognizerScreen() {

    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }

    // Language selection state
    var selectedLanguage by remember { mutableStateOf(RecognitionLanguage.LATIN) }

    // Rebuild the recognizer whenever the language changes
    val textRecognizer by remember(selectedLanguage) { mutableStateOf(buildRecognizer(selectedLanguage)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
        recognizedText = ""
        showCamera = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LanguageSelectorRow(
            selected = selectedLanguage,
            onLanguageSelected = { lang ->
                selectedLanguage = lang
                recognizedText = ""
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE0E0E0))
                .border(
                    width = 1.dp,
                    color = Color(0xFFBDBDBD),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            if (showCamera) {
                InlineCameraPreview(
                    textRecognizer = textRecognizer,
                    onLiveTextUpdate = { text -> recognizedText = text }
                )
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            recognizedText = ""
                            launcher.launch("image/*")
                        }
                )

                if (imageUri == null) {
                    Text(
                        text = "Tap to pick an image",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            OutlinedButton(
                onClick = {
                    showCamera = false
                    recognizedText = ""
                    launcher.launch("image/*")
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Pick Image") }

            Button(
                onClick = {
                    if (showCamera) {
                        showCamera = false
                    } else {
                        imageUri = null
                        recognizedText = ""
                        showCamera = true
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) { Text(if (showCamera) "Close Camera" else "Open Camera") }

            Button(
                onClick = {
                    imageUri?.let { uri ->
                        val inputImage = InputImage.fromFilePath(context, uri)
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { result ->
                                recognizedText = result.text.ifBlank { "No text found." }
                            }
                            .addOnFailureListener {
                                recognizedText = "Failed to recognize text."
                            }
                    }
                },
                enabled = imageUri != null && !showCamera,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Recognize") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (recognizedText.isNotEmpty()) {
            RecognizedTextCard(
                text = recognizedText,
                language = selectedLanguage
            )
        }
    }
}


/**
 * Horizontal scrollable row of language chips.
 * Tapping a chip switches the active recognizer model.
 */
@Composable
fun LanguageSelectorRow(
    selected: RecognitionLanguage,
    onLanguageSelected: (RecognitionLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${selected.flag}  ${selected.label}  ▾")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RecognitionLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.flag}  ${lang.label}") },
                    onClick = {
                        onLanguageSelected(lang)
                        expanded = false
                    },
                    leadingIcon = {
                        if (lang == selected) {
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}


/**
 * Camera preview that fits inside the same 400 dp box as the image.
 * Uses [ImageAnalysis] to feed frames continuously into the text recognizer,
 * so results update in real time without any extra button press.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun InlineCameraPreview(
    textRecognizer: TextRecognizer,
    onLiveTextUpdate: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(textRecognizer) {

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        // 1. Preview use-case
        val preview = CameraXPreview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        // 2. ImageAnalysis use-case – live OCR on every frame
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { result ->
                                if (result.text.isNotBlank()) {
                                    onLiveTextUpdate(result.text)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()   // always close the proxy!
                            }
                    } else {
                        imageProxy.close()
                    }
                }
            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
        )
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}


/**
 * Displays the OCR output with a small language badge.
 */
@Composable
fun RecognizedTextCard(
    text: String,
    language: RecognitionLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Language badge
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "${language.flag} ${language.label}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Recognized text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Preview(showBackground = true)
@Composable
fun TextRecognizerScreenPreview() {
    TextRecognizerScreen()
}

@Preview(showBackground = true)
@Composable
fun RecognizedTextCardPreview() {
    RecognizedTextCard(
        text = "नमस्ते — यह एक नमूना पाठ है।",
        language = RecognitionLanguage.DEVANAGARI
    )
}