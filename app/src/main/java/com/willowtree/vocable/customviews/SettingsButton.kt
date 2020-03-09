package com.willowtree.vocable.customviews

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.willowtree.vocable.settings.SettingsActivity

/**
 * A subclass of VocableButton that will open SettingsActivity when interacted with
 */
class SettingsButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ActionButton(context, attrs, defStyle) {

    override fun sayText(text: CharSequence?) {
        //no-op
    }
}