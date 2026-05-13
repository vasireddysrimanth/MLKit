package com.devsrimanth.mlkit.faceDetection

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.devsrimanth.mlkit.databinding.FragmentFaceDetectionBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

/**
 * @author : Srimanth Chowdary Vasireddy
 * @Date : 2026-05-13
 * @Project : ML Kit
 *
 */
class FaceDetectionFragment : Fragment() {

    private lateinit var binding: FragmentFaceDetectionBinding

    private var inputImage: InputImage? = null

    /**
     * Step 1 : pick an image from the gallery and get its uri
     */
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            inputImage = InputImage.fromFilePath(requireContext(), uri)
            binding.imageView.setImageURI(uri)
        }
    }

    /**
     * Step 2 : face detector options
     * 1. PERFORMANCE_MODE_FAST — quick detection
     * 2. LANDMARK_MODE_ALL — detect eyes, nose, mouth etc
     * 3. CLASSIFICATION_MODE_ALL — smiling, eyes open etc
     */
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    /**
     * Step 3 : create detector — lazy so created only when first used
     */
    private val faceDetector: FaceDetector by lazy {
        FaceDetection.getClient(options)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFaceDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pickButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.analyzeButton.setOnClickListener {
            val image = inputImage ?: run {
                binding.resultText.text = "Please pick an image first"
                return@setOnClickListener
            }
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        binding.resultText.text = "No face detected"
                    } else {
                        processFaces(faces)
                    }
                }
                .addOnFailureListener { e ->
                    binding.resultText.text = "Failed to detect faces: ${e.message}"
                }
        }

        binding.clearButton.setOnClickListener {
            inputImage = null
            binding.imageView.setImageDrawable(null)
            binding.resultText.text = ""
        }
    }

    // Bug 3 Fix — correct ML Kit Face import
    private fun processFaces(faces: List<Face>) {
        val result = StringBuilder()

        for ((index, face) in faces.withIndex()) {

            result.appendLine("Face ${index + 1}")

            // 1. Bounding Box
            val bounds = face.boundingBox
            result.appendLine("Bounds: $bounds")

            // 2. Head Rotation
            result.appendLine("Rotation Y (left/right): ${face.headEulerAngleY}")
            result.appendLine("Rotation Z (tilt): ${face.headEulerAngleZ}")

            // 3. Landmarks
            val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
            result.appendLine("Left Ear Position: ${leftEar?.position}")

            val rightsEar = face.getLandmark(FaceLandmark.RIGHT_EAR)
            result.appendLine("Right Ear Position: ${rightsEar?.position}")


            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
            result.appendLine("Left EYE Position: ${leftEye?.position}")

            val rightsEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
            result.appendLine("Right EYE Position: ${rightsEye?.position}")

            val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
            result.appendLine("nose Position: ${nose?.position}")

            val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)
            result.appendLine("left check Position: ${leftCheek?.position}")

            // 4. Classification
            face.smilingProbability?.let {
                result.appendLine("Smiling: ${"%.0f".format(it * 100)}%")
            }
            face.rightEyeOpenProbability?.let {
                result.appendLine("Right Eye Open: ${"%.0f".format(it * 100)}%")
            }

            result.appendLine()
        }

        binding.resultText.text = result.toString()
    }
}