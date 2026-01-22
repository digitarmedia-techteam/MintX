package com.digitar.mintx.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.digitar.mintx.R

class AnalogTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#E0E0E0")
        isAntiAlias = true
    }

    private val progressPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.GREEN
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val rectF = RectF()
    private var progress: Float = 100f
    private var timerText: String = "00:00"

    fun setProgress(value: Float, color: Int) {
        this.progress = value.coerceIn(0f, 100f)
        this.progressPaint.color = color
        invalidate()
    }
    
    fun setText(text: String) {
        this.timerText = text
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 20f
        rectF.set(padding, padding, w - padding, h - padding)
        textPaint.textSize = w / 4f
        bgPaint.strokeWidth = w / 12f
        progressPaint.strokeWidth = w / 12f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw Background Ring
        canvas.drawOval(rectF, bgPaint)
        
        // Draw Progress Arc (Clockwise from top)
        // 360 * (progress / 100)
        val sweepAngle = 360f * (progress / 100f)
        canvas.drawArc(rectF, 270f, -sweepAngle, false, progressPaint)
        
        // Draw Text
        val xPos = width / 2f
        val yPos = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(timerText, xPos, yPos, textPaint)
    }
}
