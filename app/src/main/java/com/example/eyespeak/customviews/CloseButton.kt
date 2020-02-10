package com.example.eyespeak.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity

/**
 * A subclass of VocableButton that will close the current activity (if the button's context is an
 * AppCompatActivity)
 */
class CloseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableButton(context, attrs, defStyle) {

    override fun onPointerEnter() {
        text = "Close"
        super.onPointerEnter()
    }

    override fun performAction() {
        if (context is AppCompatActivity) {
            (context as AppCompatActivity).finish()
        }
    }

    override fun onPointerExit() {
        text = null
        super.onPointerExit()
    }
}