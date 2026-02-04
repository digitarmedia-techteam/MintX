package com.digitar.mintx.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.digitar.mintx.R
import kotlin.math.min

class AnalogTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Background Ring Paint (The empty track)
    private val bgPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.gray_200) // Visible gray track
        isAntiAlias = true
    }

    // Progress Arc Paint (The remaining time)
    private val progressPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.mint_primary) // Bold primary color
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Timer Text Paint
    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.text_headline)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
        // Optional: Monospaced numbers if font supports it, otherwise standard
    }

    private val rectF = RectF()
    private var progress: Float = 100f
    private var timerText: String = ""

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
        
        // Dynamic Stroke Width: 10% of the smaller dimension (e.g., 50dp -> 5dp stroke)
        val size = min(w, h)
        val strokeThickness = size * 0.1f
        
        bgPaint.strokeWidth = strokeThickness
        progressPaint.strokeWidth = strokeThickness
        
        // Text Size: 30% of size (e.g., 50dp -> 15dp text)
        textPaint.textSize = size * 0.3f
        
        // Padding: Half stroke width to ensure draw is within bounds + tiny buffer
        val padding = strokeThickness / 2f + 2f
        rectF.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw Background Ring
        canvas.drawOval(rectF, bgPaint)
        
        // Draw Progress Arc (Starts from top -90 degrees, sweeps counter-clockwise or clockwise depending on logic)
        // Usually timers go clockwise. sweepAngle is positive.
        // If we want it to decrease: 
        val sweepAngle = 360f * (progress / 100f)
        
        // Draw starting from top (270 degrees)
        // If progress is 100% -> Full circle
        // If progress decreases, we want the arc to shrink.
        // 'false' for useCenter means draw it as an arc/stroke, not a wedge.
        canvas.drawArc(rectF, 270f, -sweepAngle, false, progressPaint)
        
        // Draw Text
        if (timerText.isNotEmpty()) {
            val xPos = width / 2f
            // Center text vertically
            val yPos = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(timerText, xPos, yPos, textPaint)
        }
    }
}
