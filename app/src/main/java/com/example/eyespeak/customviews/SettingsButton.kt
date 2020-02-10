package com.example.eyespeak.customviews

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.example.eyespeak.settings.SettingsActivity

/**
 * A subclass of VocableButton that will open SettingsActivity when interacted with
 */
class SettingsButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableButton(context, attrs, defStyle) {

    override fun performAction() {
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }
}