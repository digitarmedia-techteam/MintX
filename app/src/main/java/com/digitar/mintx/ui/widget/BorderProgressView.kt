package com.digitar.mintx.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BorderProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val progressPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        // Glow effect
        setShadowLayer(10f, 0f, 0f, Color.GREEN)
    }

    private val borderPath = Path()
    private val progressPath = Path()
    private val rectF = RectF()
    private val pathMeasure = PathMeasure()

    var cornerRadius: Float = 32f
    var strokeWidth: Float = 12f
    
    // Animation properties
    private var progress: Float = 100f // 0 to 100
    private var currentStrokeWidth: Float = 12f
    private var isPulsing = false
    private var pulseScale = 1f
    private var pulseDirection = 1
    
    init {
        // Density scaling
        val density = resources.displayMetrics.density
        cornerRadius = 32f * density
        strokeWidth = 4f * density // Base stroke width
        currentStrokeWidth = strokeWidth
        
        // Enable hardware acceleration for shadow layer if needed, or disable if buggy
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setProgress(value: Float, color: Int) {
        this.progress = value.coerceIn(0f, 100f)
        
        // Update color and glow
        progressPaint.color = color
        progressPaint.setShadowLayer(15f, 0f, 0f, color)
        
        // Pulse logic for low time (< 20%)
        if (progress < 20f && progress > 0f) {
            isPulsing = true
        } else {
            isPulsing = false
            currentStrokeWidth = strokeWidth
        }
        
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath()
    }

    private fun updatePath() {
        val halfStroke = strokeWidth * 1.5f // Ensure padding for glow
        rectF.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        
        borderPath.reset()
        borderPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        pathMeasure.setPath(borderPath, false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Pulse Animation logic
        if (isPulsing) {
            pulseScale += 0.05f * pulseDirection
            if (pulseScale >= 1.5f) pulseDirection = -1
            if (pulseScale <= 1.0f) pulseDirection = 1
            progressPaint.strokeWidth = strokeWidth * pulseScale
            // Trigger constant redraw for animation
            postInvalidateOnAnimation()
        } else {
            progressPaint.strokeWidth = strokeWidth
        }

        val length = pathMeasure.length
        val drawLength = length * (progress / 100f)

        progressPath.reset()
        // Draw segment from top-center (handled by path definition usually starting at corner)
        // To make it look "morphing", we can keep it simple or offset it.
        // Standard path segment:
        pathMeasure.getSegment(0f, drawLength, progressPath, true)
        
        canvas.drawPath(progressPath, progressPaint)
    }
}
