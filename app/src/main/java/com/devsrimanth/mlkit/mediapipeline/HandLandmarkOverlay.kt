package com.devsrimanth.mlkit.mediapipeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Draws the 21 hand landmarks as dots and connects them with lines
 * to visualize the hand skeleton, on top of the camera preview.
 *
 * Landmark coordinates from MediaPipe are normalized (0.0 to 1.0),
 * so we multiply by the actual canvas width/height to get pixel positions.
 */

/** Pairs of landmark indices that should be connected by a line (hand skeleton) */
private val HAND_CONNECTIONS = listOf(
    // Thumb
    0 to 1, 1 to 2, 2 to 3, 3 to 4,
    // Index finger
    0 to 5, 5 to 6, 6 to 7, 7 to 8,
    // Middle finger
    0 to 9, 9 to 10, 10 to 11, 11 to 12,
    // Ring finger
    0 to 13, 13 to 14, 14 to 15, 15 to 16,
    // Pinky
    0 to 17, 17 to 18, 18 to 19, 19 to 20,
    // Palm base
    5 to 9, 9 to 13, 13 to 17
)

@Composable
fun HandLandmarkOverlay(
    result: HandLandmarkerResult,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        /** Loop through each detected hand (max 2 as configured) */
        for (landmarks in result.landmarks()) {

            /** Draw skeleton connections first (so dots appear on top) */
            for ((startIdx, endIdx) in HAND_CONNECTIONS) {
                val start = landmarks[startIdx]
                val end = landmarks[endIdx]

                drawLine(
                    color = Color.Green,
                    start = Offset(start.x() * canvasWidth, start.y() * canvasHeight),
                    end = Offset(end.x() * canvasWidth, end.y() * canvasHeight),
                    strokeWidth = 4f
                )
            }

            /** Draw a dot for each of the 21 landmark points */
            for (landmark in landmarks) {
                drawCircle(
                    color = Color.Red,
                    radius = 8f,
                    center = Offset(landmark.x() * canvasWidth, landmark.y() * canvasHeight)
                )
            }
        }
    }
}