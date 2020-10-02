package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet

/**
 * A class that allows you to pass a custom action to a VocableButton
 */
class VocablePhraseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableButton(context, attrs, defStyle) {

    var action: (() -> Unit)? = null

    override fun performAction() {
        action?.invoke()
    }
}