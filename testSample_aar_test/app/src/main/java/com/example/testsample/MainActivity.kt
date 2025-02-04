package com.example.testsample

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private var camera: Camera? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var circleView: CircleView
    private var progressAnimator: ValueAnimator? = null
    private var isFaceDetected = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val CAPTURE_DELAY = 3000L
        private const val FINISH_DELAY = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        checkAndRequestPermission()
    }

    private fun initializeViews() {
        surfaceView = findViewById(R.id.surfaceView)
        circleView = findViewById(R.id.circleView)
        surfaceHolder = surfaceView.holder.apply {
            addCallback(this@MainActivity)
        }
    }

    private fun checkAndRequestPermission() {
        if (hasCameraPermission()) {
            setupCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    private fun setupCamera() {
        try {
            initializeCamera()
            setupFaceDetection()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeCamera() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT).apply {
            setDisplayOrientation(90)
            parameters = parameters?.apply {
                setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                
                // FIT_CENTER를 위한 프리뷰 크기 설정
                val displayMetrics = resources.displayMetrics
                val screenRatio = displayMetrics.heightPixels.toFloat() / displayMetrics.widthPixels
                
                val sizes = supportedPreviewSizes
                var optimalSize = sizes[0]
                var minDiff = Float.MAX_VALUE
                
                // 화면 비율과 가장 비슷한 프리뷰 크기 선택
                for (size in sizes) {
                    val ratio = size.width.toFloat() / size.height
                    val diff = abs(ratio - screenRatio)
                    if (diff < minDiff) {
                        optimalSize = size
                        minDiff = diff
                    }
                }
                
                setPreviewSize(optimalSize.width, optimalSize.height)
            }
        }
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>, w: Int, h: Int): Camera.Size {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = h.toDouble() / w

        if (sizes.isEmpty()) return sizes[0]

        var optimalSize = sizes[0]
        var minDiff = Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - h).toDouble()
            }
        }

        if (minDiff == Double.MAX_VALUE) {
            for (size in sizes) {
                if (abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height - h).toDouble()
                }
            }
        }

        return optimalSize
    }

    private fun setupFaceDetection() {
        camera?.setFaceDetectionListener { faces, _ ->
            when {
                faces.isNotEmpty() && !isFaceDetected -> {
                    isFaceDetected = true
                    startCaptureTimer()
                }
                faces.isEmpty() && isFaceDetected -> {
                    isFaceDetected = false
                    cancelCaptureTimer()
                }
            }
        }
    }

    private fun startCaptureTimer() {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CAPTURE_DELAY
            addUpdateListener { animation ->
                circleView.setProgress(animation.animatedValue as Float)
            }
            addListener(createAnimatorListener())
            start()
        }
    }

    private fun createAnimatorListener() = object : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) {}
        override fun onAnimationEnd(animator: Animator) {
            if (isFaceDetected) takePicture()
        }
        override fun onAnimationCancel(animator: Animator) {}
        override fun onAnimationRepeat(animator: Animator) {}
    }

    private fun cancelCaptureTimer() {
        progressAnimator?.cancel()
        circleView.setProgress(0f)
    }

    private fun takePicture() {
        camera?.takePicture(null, null, Camera.PictureCallback { data, _ ->
            try {
                saveRotatedImage(data)
                restartPreviewAndFinish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
    }

    private fun saveRotatedImage(data: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        val rotatedBitmap = createRotatedBitmap(bitmap)
        val imageData = convertBitmapToByteArray(rotatedBitmap)
        saveImageToGallery(imageData)
        recycleBitmaps(bitmap, rotatedBitmap)
    }

    private fun createRotatedBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { 
            postRotate(-90f)
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(
            source, 0, 0,
            source.width, source.height,
            matrix, true
        )
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.toByteArray()
        }
    }

    private fun saveImageToGallery(imageData: ByteArray) {
        val values = createImageContentValues()
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            saveImageToUri(uri, imageData, values)
        }
    }

    private fun createImageContentValues(): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "CUBOX_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
    }

    private fun saveImageToUri(uri: android.net.Uri, imageData: ByteArray, values: ContentValues) {
        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(imageData)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        }
    }

    private fun recycleBitmaps(vararg bitmaps: Bitmap) {
        bitmaps.forEach { it.recycle() }
    }

    private fun restartPreviewAndFinish() {
        camera?.startPreview()
        isFaceDetected = false
        
        // 저장 완료 메시지 표시
        Toast.makeText(this, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
        
        // 약간의 딜레이 후 앱 종료 (Toast 메시지가 보이도록)
        Handler(Looper.getMainLooper()).postDelayed({ 
            finish() 
        }, FINISH_DELAY)
    }

    // SurfaceHolder.Callback implementations
    override fun surfaceCreated(holder: SurfaceHolder) {
        startCameraPreview(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (surfaceHolder.surface == null) return
        restartCameraPreview(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    private fun startCameraPreview(holder: SurfaceHolder) {
        try {
            camera?.apply {
                setPreviewDisplay(holder)
                startPreview()
                startFaceDetection()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restartCameraPreview(holder: SurfaceHolder) {
        try {
            camera?.stopPreview()
            startCameraPreview(holder)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseCamera() {
        camera?.apply {
            stopFaceDetection()
            stopPreview()
            release()
        }
        camera = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.isNotEmpty() 
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressAnimator?.cancel()
        cancelCaptureTimer()
        releaseCamera()
    }
}

