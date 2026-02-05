package com.appslabs.mintx.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.appslabs.mintx.R

class SolvedStatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var easyCount = 0
    private var mediumCount = 0
    private var hardCount = 0
    private var totalSolved = 0
    private var totalQuestions = 0
    
    // Loading state
    private var isLoading = true
    private var animationProgress = 0f
    private var shimmerOffset = 0f

    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E0E0E0") // Light Grey
    }

    private val easyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.mint_green)
    }

    private val mediumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.mint_gold)
    }

    private val hardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.accent_red)
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.text_headline)
        isFakeBoldText = true
    }
    
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.text_subtext)
        textSize = 30f // Will be scaled
    }

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E8E8E8")
    }
    
    private val rect = RectF()
    private var strokeWidth = 20f

    init {
        // Optional: Load attributes if defined in attrs.xml
        val density = resources.displayMetrics.density
        strokeWidth = 8 * density
        
        trackPaint.strokeWidth = strokeWidth
        easyPaint.strokeWidth = strokeWidth
        mediumPaint.strokeWidth = strokeWidth
        hardPaint.strokeWidth = strokeWidth
        
        textPaint.textSize = 24 * density
        labelPaint.textSize = 12 * density
        shimmerPaint.strokeWidth = strokeWidth
    }

    fun setData(easy: Int, medium: Int, hard: Int, totalAll: Int) {
        this.easyCount = easy
        this.mediumCount = medium
        this.hardCount = hard
        this.totalSolved = easy + medium + hard
        this.totalQuestions = totalAll
        isLoading = false
        animationProgress = 0f
        animateToFull()
    }
    
    fun startLoading() {
        isLoading = true
        animationProgress = 0f
        shimmerOffset = 0f
        invalidate()
        startShimmerAnimation()
    }
    
    fun stopLoading() {
        isLoading = false
        invalidate()
    }
    
    private fun startShimmerAnimation() {
        if (!isLoading) return
        shimmerOffset += 10f
        if (shimmerOffset > 360f) shimmerOffset = 0f
        invalidate()
        postDelayed({ startShimmerAnimation() }, 16)
    }
    
    private fun animateToFull() {
        if (animationProgress >= 1f) {
            animationProgress = 1f
            invalidate()
            return
        }
        animationProgress += 0.05f
        invalidate()
        postDelayed({ animateToFull() }, 16)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = minOf(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = strokeWidth / 2
        rect.set(padding, padding, width - padding, height - padding)

        val cx = width / 2f
        val cy = height / 2f
        
        if (isLoading) {
            // Draw shimmer loading state
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)
            
            // Draw animated shimmer arc
            val shimmerLength = 90f
            canvas.drawArc(rect, shimmerOffset, shimmerLength, false, shimmerPaint)
            
            // Draw loading text
            canvas.drawText("Loading...", cx, cy + (textPaint.textSize / 3), labelPaint)
            return
        }

        // Draw Track
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        // Draw Center Text
        if (totalQuestions > 0) {
            canvas.drawText("$totalSolved / $totalQuestions", cx, cy + (textPaint.textSize / 3), textPaint)
        } else {
            canvas.drawText("$totalSolved", cx, cy + (textPaint.textSize / 3), textPaint)
        }
        canvas.drawText("Solved", cx, cy + (textPaint.textSize) + 10, labelPaint)

        if (totalSolved == 0) return
        
        val total = totalSolved.toFloat()
        val easyAngle = (easyCount / total) * 360f * animationProgress
        val mediumAngle = (mediumCount / total) * 360f * animationProgress
        val hardAngle = (hardCount / total) * 360f * animationProgress

        var startAngle = -90f // Start from top

        // Draw Easy
        if (easyCount > 0) {
            canvas.drawArc(rect, startAngle, easyAngle, false, easyPaint)
            startAngle += easyAngle
        }

        // Draw Medium
        if (mediumCount > 0) {
            canvas.drawArc(rect, startAngle, mediumAngle, false, mediumPaint)
            startAngle += mediumAngle
        }

        // Draw Hard
        if (hardCount > 0) {
            canvas.drawArc(rect, startAngle, hardAngle, false, hardPaint)
        }
    }
}

