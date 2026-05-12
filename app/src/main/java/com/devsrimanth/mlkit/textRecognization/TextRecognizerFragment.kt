package com.devsrimanth.mlkit.textRecognization

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import com.devsrimanth.mlkit.R
import com.devsrimanth.mlkit.databinding.FragmentTextRecognizerBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author : Srimanth Chowdary Vasireddy
 * @Date : 2026-05-11
 * @Project : ML Kit
 */

class TextRecognizerFragment : Fragment() {

    private var _binding: FragmentTextRecognizerBinding? = null
    private val binding get() = _binding!!

    /**
     * step : 1 create a text recognizer for from ml kit
     */
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * step 2 : create a camera executor to run the text recognition in background thread
     */
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * add image picker to select an image from the gallery and get its uri
     */
    private var selectedImageUri : Uri? = null

    /**
     * add a throttle mechanism to prevent overload when analyzing camera frames (process 1 frame/sec)
     */
    private var lastAnalysisTime = 0L
    private val THROTTLE_MS = 1000L

    /**
     * register an activity result launcher to pick an image from the gallery and get its uri
     */
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.previewView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.imageView.setImageURI(it)
            binding.analyzeButton.isEnabled = true
        }
    }

    /**
     *  register an activity result launcher to request camera permission and start the camera if granted
     */
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else binding.resultText.text = "Camera permission denied"
    }




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextRecognizerBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.analyzeButton.isEnabled = selectedImageUri != null

        /**
         * user click pick button launch gallery to select an image
         */
        binding.pickButton.setOnClickListener {
            stopCamera()
            imagePickerLauncher.launch("image/*")
            binding.previewView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.resultText.text = ""
        }

        /**
         * user click analyze button analyze the selected image and display the result
         */
        binding.analyzeButton.setOnClickListener {
            selectedImageUri?.let {
                analyzeImage(it)
            } ?: run {
                binding.resultText.text = getString(R.string.please_select_an_image_first)
            }
        }


        /**
         * initialize the camera executor to run the text recognition in background thread
         */
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.cameraButton.setOnClickListener {
            startCamera()
            binding.resultText.text = ""
            binding.imageView.setImageDrawable(null)
            binding.previewView.visibility = View.VISIBLE
            binding.imageView.visibility = View.GONE
            selectedImageUri = null
        }

        binding.clearButton.setOnClickListener {
            stopCamera()
            binding.resultText.text = ""
            binding.imageView.setImageDrawable(null)
            binding.previewView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            selectedImageUri = null
            binding.analyzeButton.isEnabled = false
        }

    }

    /**
     * step 3 : analyze the image using the text recognizer and display the result
     */
    private fun analyzeImage(uri: Uri) {
        val image = InputImage.fromFilePath(requireContext(), uri)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                binding.resultText.text = visionText.text
            }
            .addOnFailureListener {
                binding.resultText.text = getString(R.string.error, it.message)
            }
    }


    /**
     * step 4 : analyze the camera frames using the text recognizer and display the result
     */
    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(android.Manifest.permission.CAMERA)
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()  // ✅ correct place

            val cameraProvider = cameraProvider!!

            binding.imageView.visibility = View.GONE
            binding.previewView.visibility = View.VISIBLE

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                )
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeFrame(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                binding.resultText.text = getString(R.string.camera_start_failed)
            }

        }, androidx.core.content.ContextCompat.getMainExecutor(requireContext()))
    }


    /**
     * step 5 : analyze the camera frames using the text recognizer and display the result
     */
    @ExperimentalGetImage
    private fun analyzeFrame(imageProxy: androidx.camera.core.ImageProxy) {

        val now = System.currentTimeMillis()

        if (now - lastAnalysisTime < THROTTLE_MS) {
            imageProxy.close()
            return
        }
        lastAnalysisTime = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text

                activity?.runOnUiThread {
                    binding.resultText.text =
                        if (text.isBlank()) "No text detected"
                        else text.take(200)
                }
            }
            .addOnFailureListener {
                binding.resultText.text = getString(R.string.error, it.message)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        recognizer.close()
        _binding = null
    }

}