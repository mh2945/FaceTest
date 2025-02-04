package com.example.testsample

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.hardware.Camera

class CameraPreview(
    context: Context,
    private val camera: Camera
) : SurfaceView(context), SurfaceHolder.Callback {
    
    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera.setPreviewDisplay(holder)
            camera.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // 빈 구현. Activity에서 카메라 리소스 해제를 처리합니다.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (holder.surface == null) return

        try {
            camera.stopPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            camera.setPreviewDisplay(holder)
            camera.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 