package com.example.eyespeak

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

class PointerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint: Paint = Paint().apply { color = 0xFFFFFFFF.toInt() }
    private val radius: Float = 25f

    private var xAdjusted: Float = 0f
    private var yAdjusted: Float = 0f

    private var xPercent: Int = 0
    private var yPercent: Int = 0

    fun updatePointerPositionPercent(xPercent: Int, yPercent: Int) {
        this.xPercent = xPercent
        this.yPercent = yPercent
        invalidate()
    }

    fun updatePointerPosition(x: Float, y: Float) {
        this.xAdjusted = x
        this.yAdjusted = y
        val params = layoutParams as ConstraintLayout.LayoutParams
        layoutParams = params.apply {
            marginStart = x.toInt()
            topMargin = y.toInt()
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val displayMetrics = DisplayMetrics()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calcAdjustedCoords(w, h)
    }

    private fun calcAdjustedCoords(width: Int, height: Int) {
        xAdjusted = (width / 100F * xPercent)
        yAdjusted = (height / 100F * yPercent)
    }
}