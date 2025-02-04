package com.example.testsample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePath = Path()
    private val paint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#272727")  // 딤 처리 색상
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }

    private val circleRadius = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        150f,  // 원의 반지름이 150dp
        resources.displayMetrics
    )

    private var circleCenterX: Float = 0f
    private var circleCenterY: Float = 0f
    private var progress: Float = 0f

    fun setProgress(value: Float) {
        progress = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        
        // 전체 화면을 딤 처리
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // 중심 좌표 계산
        circleCenterX = width / 2f
        circleCenterY = height / 2f
        
        // 원 그리기
        circlePath.reset()
        circlePath.addCircle(
            circleCenterX,
            circleCenterY,
            circleRadius,
            Path.Direction.CCW
        )
        
        canvas.drawPath(circlePath, paint)

        // 프로그레스 원호 그리기
        val sweepAngle = 360 * progress
        canvas.drawArc(
            circleCenterX - circleRadius,
            circleCenterY - circleRadius,
            circleCenterX + circleRadius,
            circleCenterY + circleRadius,
            -90f,
            sweepAngle,
            false,
            progressPaint
        )
    }
}

// Extension function for dp to px conversion
fun Int.dpToPx(context: Context): Float {
    return this * context.resources.displayMetrics.density
}
