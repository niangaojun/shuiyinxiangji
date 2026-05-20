package com.example.watermarkcamera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.AnimatorSet
import androidx.core.content.ContextCompat

class FocusRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val accentColor = ContextCompat.getColor(context, R.color.accent_glow)
    
    private val paint = Paint().apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    private val glowPaint = Paint().apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
        alpha = 80
    }

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 60f
    private var isAnimating = false
    private var currentScale = 1.5f

    fun showAt(x: Float, y: Float) {
        centerX = x
        centerY = y
        visibility = VISIBLE
        
        // 如果正在动画，先取消
        if (isAnimating) {
            clearAnimation()
        }
        
        // 重置状态
        currentScale = 1.5f
        paint.alpha = 255
        glowPaint.alpha = 80
        
        // 开始动画
        startAnimation()
    }

    fun hide() {
        visibility = INVISIBLE
        isAnimating = false
    }

    private fun startAnimation() {
        isAnimating = true
        
        // 创建缩放动画 - 从大到小
        val scaleAnimator = ValueAnimator.ofFloat(1.5f, 1.0f).apply {
            duration = 300
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animation ->
                currentScale = animation.animatedValue as Float
                radius = 60f * currentScale
                invalidate()
            }
        }
        
        // 创建透明度渐变动画
        val alphaAnimator = ValueAnimator.ofInt(255, 0).apply {
            duration = 800
            startDelay = 200
            addUpdateListener { animation ->
                val alpha = animation.animatedValue as Int
                paint.alpha = alpha
                glowPaint.alpha = (alpha * 0.3).toInt()
                invalidate()
            }
        }
        
        // 组合动画
        AnimatorSet().apply {
            playSequentially(
                scaleAnimator,
                alphaAnimator
            )
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    hide()
                    // 重置透明度
                    paint.alpha = 255
                    glowPaint.alpha = 80
                    isAnimating = false
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cornerLength = 25f
        val gap = 8f
        
        // 绘制发光效果
        drawCornerGlow(canvas, centerX - radius, centerY - radius, cornerLength, gap, true, true)
        drawCornerGlow(canvas, centerX + radius, centerY - radius, cornerLength, gap, false, true)
        drawCornerGlow(canvas, centerX - radius, centerY + radius, cornerLength, gap, true, false)
        drawCornerGlow(canvas, centerX + radius, centerY + radius, cornerLength, gap, false, false)
        
        // 绘制对焦框线条
        drawCorner(canvas, centerX - radius, centerY - radius, cornerLength, gap, true, true)
        drawCorner(canvas, centerX + radius, centerY - radius, cornerLength, gap, false, true)
        drawCorner(canvas, centerX - radius, centerY + radius, cornerLength, gap, true, false)
        drawCorner(canvas, centerX + radius, centerY + radius, cornerLength, gap, false, false)
        
        // 绘制中心点
        canvas.drawCircle(centerX, centerY, 4f, paint)
    }
    
    private fun drawCornerGlow(
        canvas: Canvas, 
        x: Float, y: Float, 
        length: Float, gap: Float,
        isLeft: Boolean, isTop: Boolean
    ) {
        val dirX = if (isLeft) 1 else -1
        val dirY = if (isTop) 1 else -1
        
        // 水平线
        canvas.drawLine(
            x, y,
            x + length * dirX, y,
            glowPaint
        )
        // 垂直线
        canvas.drawLine(
            x, y,
            x, y + length * dirY,
            glowPaint
        )
    }
    
    private fun drawCorner(
        canvas: Canvas, 
        x: Float, y: Float, 
        length: Float, gap: Float,
        isLeft: Boolean, isTop: Boolean
    ) {
        val dirX = if (isLeft) 1 else -1
        val dirY = if (isTop) 1 else -1
        
        // 水平线
        canvas.drawLine(
            x + gap * if (isLeft) 1 else -1, y,
            x + length * dirX, y,
            paint
        )
        // 垂直线
        canvas.drawLine(
            x, y + gap * if (isTop) 1 else -1,
            x, y + length * dirY,
            paint
        )
    }
}
