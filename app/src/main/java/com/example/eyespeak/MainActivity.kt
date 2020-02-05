package com.example.eyespeak

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity() {
    private val allViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        basic_needs.isChecked = true
    }

    override fun getPointerView(): PointerView = pointer_view
    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(parent_layout)
        }
        return allViews
    }

    override fun getLayout(): Int = R.layout.activity_main

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