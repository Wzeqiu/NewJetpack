package com.common.camera

import android.Manifest.permission.CAMERA
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.common.kt.requestPermission
import java.io.File

class MainActivity : AppCompatActivity() {
    private val priView by lazy { findViewById<PreviewView>(R.id.preView) }
    private val picture by lazy { findViewById<Button>(R.id.picture) }
    private val change by lazy { findViewById<Button>(R.id.change) }
    private val image by lazy { findViewById<ImageView>(R.id.image) }
    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission(CAMERA) {
            startCamera()
        }

        picture.setOnClickListener {
            val file = File(cacheDir, "${System.currentTimeMillis()}.jpg")
            val metadata = ImageCapture.Metadata()
            metadata.isReversedHorizontal = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
            val outputOptions =
                ImageCapture.OutputFileOptions
                    .Builder(file)
                    .setMetadata(metadata)
                    .build()
            imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object :
                ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.e("TAG", "onImageSaved: ")
                    Glide.with(this@MainActivity).load(file).into(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("TAG", "onError: " + exception.message)
                }

            })
        }
        change.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = priView.surfaceProvider
            }
            imageCapture = ImageCapture.Builder().build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }
}