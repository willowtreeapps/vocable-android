package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet
import com.willowtree.vocable.keyboard.CurrentKeyboardText

class KeyboardButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableButton(context, attrs, defStyle),
    PointerListener {

    override fun sayText(text: CharSequence?) {
        //no-op
    }

    override fun performAction() {
        CurrentKeyboardText.addCharacterToText(text?.toString() ?: "")
    }
}