package com.devsrimanth.mlkit.textRecognizationNew

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

/**
 * @author : Srimanth Chowdary Vasireddy
 * @Date : 2026-05-18
 * @Project : ML Kit
 */

private val Purple = Color(0xFF534AB7)
private val PurpleLight = Color(0xFFEEEDFE)
private val Teal = Color(0xFF0F6E56)
private val TealLight = Color(0xFFE1F5EE)
private val Amber = Color(0xFF854F0B)
private val AmberLight = Color(0xFFFAEEDA)

@Composable
fun TextRecognizationScreen() {
    TextRecognizationScreenContent()
}

@Composable
private fun TextRecognizationScreenContent() {
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedResult  by remember { mutableStateOf<Text?>(null) }
    var errorMessage      by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedImageUri = uri
        errorMessage = null
        try {
            recognizer.process(InputImage.fromFilePath(context, uri))
                .addOnSuccessListener { recognizedResult = it }
                .addOnFailureListener { errorMessage = it.message }
        } catch (e: IOException) {
            errorMessage = e.message
        }
    }

    Scaffold(
        topBar = { TopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ImagePreviewCard(uri = selectedImageUri)

            PickImageButton(onClick = { launcher.launch("image/*") })

            recognizedResult?.let { result ->
                StatsRow(result)
                ResultCard(result)
            }

            errorMessage?.let {
                ErrorCard(message = it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Text recognition",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = "ML Kit · Latin script",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun ImagePreviewCard(uri: Uri?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No image selected",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PickImageButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Purple)
    ) {
        Icon(
            imageVector = Icons.Outlined.Upload,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Select image from gallery",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StatsRow(result: Text) {
    val blockCount   = result.textBlocks.size
    val lineCount    = result.textBlocks.sumOf { it.lines.size }
    val elementCount = result.textBlocks.sumOf { b -> b.lines.sumOf { it.elements.size } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(value = "$blockCount",   label = "Blocks", tint = Purple,      bg = PurpleLight, modifier = Modifier.weight(1f))
        StatCard(value = "$lineCount",    label = "Lines",  tint = Teal,        bg = TealLight,   modifier = Modifier.weight(1f))
        StatCard(value = "$elementCount", label = "Words",  tint = Amber,       bg = AmberLight,  modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = tint)
        Text(text = label, fontSize = 11.sp, color = tint.copy(alpha = 0.7f))
    }
}

@Composable
private fun ResultCard(result: Text) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recognized text",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PurpleLight
            ) {
                Text(
                    text = "${result.textBlocks.size} blocks",
                    fontSize = 11.sp,
                    color = Purple,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp)

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            result.textBlocks.forEachIndexed { blockIndex, block ->
                BlockRow(blockIndex = blockIndex, block = block)
            }
        }
    }
}

@Composable
private fun BlockRow(blockIndex: Int, block: Text.TextBlock) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(IntrinsicSize.Min)
                .background(Purple, RoundedCornerShape(1.dp))
        )
        Column(
            modifier = Modifier.padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Block $blockIndex — \"${block.text.take(40)}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${block.boundingBox}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )

            block.lines.forEachIndexed { lineIndex, line ->
                LineRow(lineIndex = lineIndex, line = line)
            }
        }
    }
}

@Composable
private fun LineRow(lineIndex: Int, line: Text.Line) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .background(Color(0xFF9FE1CB), RoundedCornerShape(1.dp))
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = "Line $lineIndex — \"${line.text}\"",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            line.elements.forEachIndexed { elementIndex, element ->
                Text(
                    text = "  Word $elementIndex: \"${element.text}\"",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "Error: $message",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TextRecognizationScreenPreview() {
    TextRecognizationScreen()
}