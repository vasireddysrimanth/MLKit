package com.devsrimanth.mlkit.mediapipeline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Converts a CameraX ImageProxy (YUV_420_888 format) into an Android Bitmap.
 *
 * Camera frames don't come as Bitmaps directly - they come in YUV format
 * (raw sensor data). This function:
 *  1. Combines the Y, U, V planes into NV21 format
 *  2. Compresses NV21 -> JPEG -> Bitmap
 *  3. Rotates the bitmap to correct orientation (front camera is rotated by default)
 */
fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {

    val yBuffer = imageProxy.planes[0].buffer
    val uBuffer = imageProxy.planes[1].buffer
    val vBuffer = imageProxy.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21, ImageFormat.NV21,
        imageProxy.width, imageProxy.height, null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
    val imageBytes = out.toByteArray()

    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    /** Correct rotation - front camera sensor is usually rotated 90/270 degrees */
    val matrix = Matrix()
    matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    return bitmap
}