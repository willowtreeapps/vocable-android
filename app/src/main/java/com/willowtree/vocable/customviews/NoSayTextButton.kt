package com.willowtree.vocable.customviews

import android.content.Context
import android.content.Intent
import android.util.AttributeSet

/**
 * A subclass of VocableButton that will not say what is on the button when it is clicked.
 */
class NoSayTextButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ActionButton(context, attrs, defStyle) {

    override fun sayText(text: CharSequence?) {
        //no-op
    }
}