package com.example.eyespeak

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity() {
    private val allViews = mutableListOf<View>()

    override fun getPointerView(): PointerView = pointer_view

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(parent_layout)
        }
        return allViews
    }

    override fun getLayout(): Int = R.layout.activity_settings

    override fun getPauseButton(): PauseButton? = settings_pause_button

    private fun getAllChildViews(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }
}