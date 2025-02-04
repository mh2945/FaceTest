package com.example.testsample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import android.net.Uri
import android.os.Build

class CameraActivity : AppCompatActivity() {
    private var camera: Camera? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private var isCountingDown = false
    private val COUNTDOWN_TIME = 3 // 3초
    private var isFrontCamera = false  // 기본값은 후면 카메라
    private lateinit var outputDirectory: File
    private val handler = Handler(Looper.getMainLooper())
    private var countDownValue = COUNTDOWN_TIME
    
    // 카메라 콜백 인터페이스
    private val pictureCallback = Camera.PictureCallback { data, _ ->
        if (data != null) {
            try {
                // 이미지 회전 처리
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                val rotatedBitmap = rotateBitmap(bitmap, if (isFrontCamera) 270f else 90f)
                
                // 이미지를 Download 폴더에 저장
                saveImageToDownloads(rotatedBitmap)
                
                // 메모리 해제
                bitmap.recycle()
                rotatedBitmap.recycle()
                
                // 앱 종료
                finishAffinity()
            } catch (e: Exception) {
                Toast.makeText(this, "사진 저장 실패", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                finish()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)
        surfaceView = findViewById(R.id.surfaceView)
        surfaceHolder = surfaceView.holder
        outputDirectory = getOutputDirectory()
        
        if (checkCameraPermission()) {
            initializeCamera()
        } else {
            requestCameraPermission()
        }

        startAutoCapture()  // 자동 촬영 시작
    }

    private fun initializeCamera() {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    val cameraId = getCameraId(isFrontCamera)
                    if (cameraId >= 0) {
                        camera = Camera.open(cameraId)
                        camera?.let { 
                            it.setDisplayOrientation(90)
                            it.setPreviewDisplay(holder)
                            setCameraParameters(it, surfaceView.width, surfaceView.height)
                            it.startPreview()
                        }
                    } else {
                        showCameraError(if (isFrontCamera) "전면 카메라를 찾을 수 없습니다" else "후면 카메라를 찾을 수 없습니다")
                    }
                } catch (e: Exception) {
                    showCameraError("카메라를 열 수 없습니다: ${e.message}")
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                camera?.let { 
                    try {
                        it.stopPreview()
                        setCameraParameters(it, width, height)
                        it.setPreviewDisplay(holder)
                        it.startPreview()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }
        })
    }

    private fun showCameraError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun releaseCamera() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (checkCameraPermission()) {
                initializeCamera()
            } else {
                showCameraError("카메라 권한이 필요합니다")
            }
        }
    }

    private fun showResult(imagePath: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("imagePath", imagePath)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)  // 핸들러 메시지 제거
        releaseCamera()
    }

    private fun setCameraParameters(camera: Camera, width: Int, height: Int) {
        try {
            val parameters = camera.parameters
            val sizes = parameters.supportedPreviewSizes
            
            // Fit_Center를 위한 최적의 프리뷰 크기 찾기
            var optimalSize = sizes[0]
            val targetRatio = width.toFloat() / height
            var minDiff = Float.MAX_VALUE

            // 화면 비율을 유지하면서 가장 적절한 크기 찾기
            for (size in sizes) {
                val ratio = size.width.toFloat() / size.height
                val diff = Math.abs(ratio - targetRatio)
                if (diff < minDiff) {
                    optimalSize = size
                    minDiff = diff
                }
            }

            // 프리뷰 크기 설정
            parameters.setPreviewSize(optimalSize.width, optimalSize.height)
            
            // 자동 초점 설정
            if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }

            camera.parameters = parameters

            // SurfaceView 크기 조정
            adjustSurfaceViewSize(optimalSize.width, optimalSize.height)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun adjustSurfaceViewSize(previewWidth: Int, previewHeight: Int) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // 프리뷰 비율 계산 (90도 회전 고려)
        val previewRatio = previewHeight.toFloat() / previewWidth

        // 화면에 맞는 크기 계산
        var surfaceWidth = screenWidth
        var surfaceHeight = (screenWidth * previewRatio).toInt()

        // 높이가 화면을 벗어나는 경우
        if (surfaceHeight > screenHeight) {
            surfaceHeight = screenHeight
            surfaceWidth = (screenHeight / previewRatio).toInt()
        }

        // SurfaceView 크기 및 위치 조정
        val params = surfaceView.layoutParams
        params.width = surfaceWidth
        params.height = surfaceHeight
        
        // 중앙 정렬
        if (surfaceView.parent is android.widget.RelativeLayout) {
            val layoutParams = params as android.widget.RelativeLayout.LayoutParams
            layoutParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT)
        }
        
        surfaceView.layoutParams = params
    }

    private fun startAutoCapture() {
        isCountingDown = true
        countDownValue = COUNTDOWN_TIME
        updateProgress()
        
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (countDownValue > 0) {
                    countDownValue--
                    updateProgress()
                    handler.postDelayed(this, 1000)
                } else {
                    takePicture()
                }
            }
        }, 1000)
    }

    private fun updateProgress() {
        val progress = 1f - (countDownValue.toFloat() / COUNTDOWN_TIME)
        findViewById<CircleView>(R.id.circleView).setProgress(progress)
    }

    private fun takePicture() {
        camera?.let {
            try {
                it.takePicture(null, null, pictureCallback)
            } catch (e: Exception) {
                Toast.makeText(this, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // 이미지 회전 함수 추가
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
            // 전면 카메라인 경우 좌우 반전
            if (isFrontCamera) {
                postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun saveImageToDownloads(bitmap: Bitmap) {
        val filename = "IMG_${SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA)
            .format(System.currentTimeMillis())}.jpg"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 이상
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    Toast.makeText(this, "이미지가 다운로드 폴더에 저장되었습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 이하
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val imageFile = File(downloadsDir, filename)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                Toast.makeText(this, "이미지가 다운로드 폴더에 저장되었습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10

        private fun getCameraId(isFront: Boolean): Int {
            val numberOfCameras = Camera.getNumberOfCameras()
            val cameraInfo = Camera.CameraInfo()
            
            for (i in 0 until numberOfCameras) {
                Camera.getCameraInfo(i, cameraInfo)
                if (isFront) {
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        return i
                    }
                } else {
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        return i
                    }
                }
            }
            return -1
        }
    }
}