package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet

class KeyboardButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ActionButton(context, attrs, defStyle) {

    override fun sayText(text: CharSequence?) {
        //no-op
    }
}