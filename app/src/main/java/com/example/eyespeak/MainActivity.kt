package com.example.eyespeak

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.example.eyespeak.customviews.PauseButton
import com.example.eyespeak.customviews.PointerListener
import com.example.eyespeak.customviews.PointerView
import com.example.eyespeak.utils.VocableTextToSpeech
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity() {
    private val allViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VocableTextToSpeech.initialize(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        VocableTextToSpeech.shutdown()
    }

    override fun getPointerView(): PointerView = pointer_view

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(parent_layout)
        }
        return allViews
    }

    override fun getLayout(): Int = R.layout.activity_main

    override fun getPauseButton(): PauseButton? = pause_button

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