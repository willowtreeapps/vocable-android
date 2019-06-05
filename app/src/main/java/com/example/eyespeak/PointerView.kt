package com.example.eyespeak

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

const val POINTER_COLOR: Int = 0xFFFFFFFF.toInt()
const val POINTER_RADIUS: Float = 100f

class PointerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint: Paint = Paint().apply { color = POINTER_COLOR }

    private val pointerPosition = PointF(0f, 0f)
    private val pointerPositionUnit = PointF(0f, 0f)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawCircle(pointerPosition.x, pointerPosition.y, POINTER_RADIUS, paint)
    }

    private fun updatePointerPosition(position: PointF) {
        this.pointerPosition.set(position)
        invalidate()
    }

    //Expect values satisfying [-1,1]
    fun updatePointerPositionUnitInternal(positionUnit: PointF) {
        pointerPositionUnit.set(positionUnit)

        val widthHalf: Float = width.toFloat()/2
        val heightHalf: Float = height.toFloat()/2

        val pointerXNew = (widthHalf * positionUnit.x) + widthHalf
        val pointerYNew = (heightHalf * pointerPositionUnit.y) + heightHalf

        updatePointerPosition(PointF(pointerXNew, pointerYNew))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePointerPositionUnitInternal(pointerPositionUnit)
        invalidate()
    }

}
