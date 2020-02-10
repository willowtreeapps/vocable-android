package com.example.eyespeak.customviews

import android.content.Context
import android.util.AttributeSet

/**
 * A subclass of VocableButton that allows a caller to define a custom action
 */
class ActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableButton(context, attrs, defStyle) {

    var action: (() -> Unit)? = null

    override fun performAction() {
        action?.invoke()
    }
}