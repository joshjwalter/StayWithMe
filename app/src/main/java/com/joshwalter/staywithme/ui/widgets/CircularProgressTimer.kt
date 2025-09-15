package com.joshwalter.staywithme.ui.widgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.joshwalter.staywithme.R

class CircularProgressTimer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var progress = 0f
    private var maxProgress = 100f
    private var displayText = "0:00"
    
    private val greenColor = ContextCompat.getColor(context, android.R.color.holo_green_dark)
    private val grayColor = Color.LTGRAY
    private val textColor = Color.BLACK
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        // Background circle paint
        backgroundPaint.apply {
            color = grayColor
            style = Paint.Style.STROKE
            strokeWidth = 20f
            strokeCap = Paint.Cap.ROUND
        }
        
        // Progress circle paint
        paint.apply {
            color = greenColor
            style = Paint.Style.STROKE
            strokeWidth = 20f
            strokeCap = Paint.Cap.ROUND
        }
        
        // Text paint
        textPaint.apply {
            color = textColor
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) / 2f) - 30f
        
        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        
        // Draw progress arc
        val sweepAngle = (progress / maxProgress) * 360f
        val rectF = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        canvas.drawArc(
            rectF,
            -90f, // Start from top
            sweepAngle,
            false,
            paint
        )
        
        // Draw text
        val textY = centerY + (textPaint.textSize / 3)
        canvas.drawText(displayText, centerX, textY, textPaint)
    }
    
    fun setProgress(current: Float, max: Float) {
        progress = current
        maxProgress = max
        invalidate()
    }
    
    fun setDisplayText(text: String) {
        displayText = text
        invalidate()
    }
    
    fun setColors(progressColor: Int, backgroundColor: Int) {
        paint.color = progressColor
        backgroundPaint.color = backgroundColor
        invalidate()
    }
}
