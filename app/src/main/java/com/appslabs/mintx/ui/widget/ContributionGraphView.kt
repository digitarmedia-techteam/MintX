package com.appslabs.mintx.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.util.AttributeSet
import com.appslabs.mintx.utils.StreakUtils
import java.util.Calendar
import java.util.Locale

class ContributionGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Tooltip Paints
    private val paintTooltipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155") // Slate 700
        style = Paint.Style.FILL
    }
    private val paintTooltipText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    
    // Data
    private var activityLevels: List<Int> = emptyList() // 0-4
    private var dailyStatsMap: Map<String, Int> = emptyMap()
    private var selectedCell: Pair<Int, Int>? = null // (Col, Row)
    
    // Config
    private val cellSize = 12f // dp
    private val gap = 3f // dp
    private val cornerRadius = 2f // dp
    private val rows = 7
    private val cols = 53
    
    // Dimensions in PX
    private var cellSizePx = 0f
    private var gapPx = 0f
    private var radiusPx = 0f
    private var textHeightPx = 0f
    
    // Colors
    private val colors = IntArray(5)

    init {
        val density = context.resources.displayMetrics.density
        cellSizePx = cellSize * density
        gapPx = gap * density
        radiusPx = cornerRadius * density
        textHeightPx = 14f * density // Space for text

        // Text Paint
        paintText.color = Color.parseColor("#94A3B8") // Slate 400
        paintText.textSize = 10f * density
        paintText.textAlign = Paint.Align.LEFT
        
        paintTooltipText.textSize = 12f * density

        // Level Colors
        colors[0] = Color.parseColor("#2D333B") // Dark Gray
        colors[1] = Color.parseColor("#0E4429") // L1
        colors[2] = Color.parseColor("#006D32") // L2
        colors[3] = Color.parseColor("#26A641") // L3
        colors[4] = Color.parseColor("#39D353") // L4
    }

    fun setActivityData(dailyStats: Map<String, Int>) {
        this.dailyStatsMap = dailyStats
        activityLevels = StreakUtils.getYearActivityLevels(dailyStats)
        requestLayout()
        invalidate()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            
            // Check grid bounds
            val gridTop = textHeightPx
            if (y < gridTop) {
                selectedCell = null
                invalidate()
                return true
            }

            val col = ((x - paddingLeft) / (cellSizePx + gapPx)).toInt()
            val row = ((y - gridTop) / (cellSizePx + gapPx)).toInt()
            
            if (col in 0 until cols && row in 0 until rows) {
                // Toggle or Select
                if (selectedCell?.first == col && selectedCell?.second == row) {
                    selectedCell = null // Deselect
                } else {
                    selectedCell = col to row
                }
                invalidate()
            } else {
                selectedCell = null
                invalidate()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val levelsToDraw = if (activityLevels.isEmpty()) List(365) { 0 } else activityLevels
        
        // 1. Draw Month Labels
        val today = Calendar.getInstance()
        var lastMonth = -1
        
        val dateCursor = Calendar.getInstance()
        dateCursor.add(Calendar.WEEK_OF_YEAR, -52)
        
        for (col in 0 until cols) {
            val month = dateCursor.get(Calendar.MONTH)
            if (month != lastMonth) {
                lastMonth = month
                val monthName = dateCursor.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
                val x = col * (cellSizePx + gapPx)
                if (col < cols - 2) { 
                    canvas.drawText(monthName ?: "", x, textHeightPx - (4 * resources.displayMetrics.density), paintText)
                }
            }
            dateCursor.add(Calendar.WEEK_OF_YEAR, 1)
        }

        // 2. Draw Grid
        val gridTopOffset = textHeightPx 
        val totalCells = cols * rows 
        val dataSize = levelsToDraw.size // 365
        val offset = totalCells - dataSize // 6
        
        for (col in 0 until cols) {
            for (row in 0 until rows) {
                val gridIndex = col * 7 + row
                val dataIndex = gridIndex - offset
                
                var level = 0
                if (dataIndex >= 0 && dataIndex < dataSize) {
                    level = levelsToDraw[dataIndex]
                }
                
                val safeLevel = level.coerceIn(0, 4)
                paint.color = colors[safeLevel]
                
                // Highlight selected cell
                if (selectedCell?.first == col && selectedCell?.second == row) {
                    paint.color = Color.WHITE // Or a highlight stroke
                }
                
                val left = col * (cellSizePx + gapPx)
                val top = gridTopOffset + row * (cellSizePx + gapPx)
                val right = left + cellSizePx
                val bottom = top + cellSizePx
                
                canvas.drawRoundRect(left, top, right, bottom, radiusPx, radiusPx, paint)
            }
        }
        
        // 3. Draw Tooltip
        selectedCell?.let { (col, row) ->
             drawTooltip(canvas, col, row, offset) // Pass offset
        }
    }
    
    private fun drawTooltip(canvas: Canvas, col: Int, row: Int, offset: Int) {
        val gridIndex = col * 7 + row
        val dataIndex = gridIndex - offset
        
        // If padding cell, don't show
        if (dataIndex < 0 || dataIndex >= 365) return

        // Calculate Date
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(364 - dataIndex)) // 364 = Today
        
        val dateString = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.time)
        val key = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val count = dailyStatsMap[key] ?: 0
        
        val text = "$count ${if (count == 1) "submission" else "submissions"} on $dateString"
        
        // Measure Text
        val textWidth = paintTooltipText.measureText(text)
        val density = resources.displayMetrics.density
        val padding = 8f * density
        val tooltipWidth = textWidth + (padding * 2)
        val tooltipHeight = paintTooltipText.textSize + (padding * 2)
        
        // Position
        val cellCenterX = (col * (cellSizePx + gapPx)) + (cellSizePx / 2)
        val cellTopY = (textHeightPx) + (row * (cellSizePx + gapPx))
        
        var tooltipX = cellCenterX
        var tooltipY = cellTopY - tooltipHeight - (6f * density)
        
        // Bounds check (Y)
        if (tooltipY < 0) {
            tooltipY = cellTopY + cellSizePx + (6f * density)
        }
        
        // Bounds check (X)
        if (tooltipX - (tooltipWidth/2) < 0) {
            tooltipX = tooltipWidth/2 + padding
        } else if (tooltipX + (tooltipWidth/2) > width) {
            tooltipX = width - (tooltipWidth/2) - padding
        }

        // Draw Bubble
        val r = 6f * density
        val rect = RectF(
            tooltipX - (tooltipWidth/2),
            tooltipY,
            tooltipX + (tooltipWidth/2),
            tooltipY + tooltipHeight
        )
        
        // Shadow/Stroke could be nice, but keep simple
        canvas.drawRoundRect(rect, r, r, paintTooltipBg)
        
        // Draw Text
        val textY = rect.centerY() - ((paintTooltipText.descent() + paintTooltipText.ascent()) / 2)
        canvas.drawText(text, rect.centerX(), textY, paintTooltipText)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (cols * (cellSizePx + gapPx) - gapPx).toInt() + paddingLeft + paddingRight
        val height = (textHeightPx + rows * (cellSizePx + gapPx) - gapPx).toInt() + paddingTop + paddingBottom
        
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }
}

