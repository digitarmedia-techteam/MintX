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
    }

    fun setData(easy: Int, medium: Int, hard: Int, totalAll: Int) {
        this.easyCount = easy
        this.mediumCount = medium
        this.hardCount = hard
        this.totalSolved = easy + medium + hard
        this.totalQuestions = totalAll
        invalidate()
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

        // Draw Track
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        // Draw Center Text
        val cx = width / 2f
        val cy = height / 2f
        
        if (totalQuestions > 0) {
            canvas.drawText("$totalSolved / $totalQuestions", cx, cy + (textPaint.textSize / 3), textPaint)
        } else {
            canvas.drawText("$totalSolved", cx, cy + (textPaint.textSize / 3), textPaint)
        }
        canvas.drawText("Solved", cx, cy + (textPaint.textSize) + 10, labelPaint)

        if (totalSolved == 0) return
        
        // Calculate Angles
        // We generally use "Total Questions" as 100% or "Total Solved" as 100%?
        // LeetCode uses "Total Solved" / "Total Questions" ratio?
        // Actually LeetCode's ring shows breakdown of SOLVED.
        // So 100% of the ring = Total Solved (if all adjacent) OR 100% ring = Total Questions?
        // LeetCode: The ring represents the *Solved* questions.
        // Wait, LeetCode's ring is often incomplete if you haven't solved all.
        // No, LeetCode's profile ring fills up based on Total Solved / Total Questions.
        // But for this simplified version, let's make the ring represent the breakdown of the solved ones (100% filled if we only care about proportions) OR
        // better: Represent Solved / Total Available.
        
        // FOR NOW: Let's assume we want to show PROPORTION of Solved vs Total Available.
        // But I don't have "Total Available" easily.
        // So let's make the ring strictly "Breakdown of Solved". i.e. Full circle = 100% of solved.
        // Wait, if I have solved 1 easy, 1 medium, 1 hard. 3 total. Each 33%.
        // The circle will be full.
        // This looks good.
        
        val total = totalSolved.toFloat()
        val easyAngle = (easyCount / total) * 360f
        val mediumAngle = (mediumCount / total) * 360f
        val hardAngle = (hardCount / total) * 360f

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
