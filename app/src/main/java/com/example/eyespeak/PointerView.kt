package com.example.eyespeak

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PointerView(context: Context, attrs: AttributeSet) : View(context, attrs){

    private val paint: Paint = Paint()
    val rectF: RectF = RectF(100.0F, 100.0F, 200.0F, 200.0F)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawOval(rectF, paint)
    }

}