package com.devsrimanth.mlkit.mediapipeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Draws the 478 face landmarks as a wireframe mesh on top of the camera feed.
 *
 * Unlike hand connections (which we wrote manually - 23 pairs),
 * face has hundreds of connections - MediaPipe provides this list
 * built-in via FaceLandmarker.FACE_LANDMARKS_TESSELATION
 */
@Composable
fun FaceMeshOverlay(
    result: FaceLandmarkerResult,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        /** Loop through each detected face */
        for (faceLandmarks in result.faceLandmarks()) {

            /** Draw mesh lines using MediaPipe's built-in tesselation connections */
            for (connection in FaceLandmarker.FACE_LANDMARKS_TESSELATION) {
                val start = faceLandmarks[connection.start()]
                val end = faceLandmarks[connection.end()]

                drawLine(
                    color = Color.Green.copy(alpha = 0.5f),
                    start = Offset(start.x() * canvasWidth, start.y() * canvasHeight),
                    end = Offset(end.x() * canvasWidth, end.y() * canvasHeight),
                    strokeWidth = 1f
                )
            }

            /** Optional: draw small dots for each of the 478 points */
            for (landmark in faceLandmarks) {
                drawCircle(
                    color = Color.Yellow,
                    radius = 1.5f,
                    center = Offset(landmark.x() * canvasWidth, landmark.y() * canvasHeight)
                )
            }
        }
    }
}