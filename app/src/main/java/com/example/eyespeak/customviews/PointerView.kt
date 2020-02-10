package com.example.eyespeak.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

class PointerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var xAdjusted: Float = 0f
    private var yAdjusted: Float = 0f

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
}