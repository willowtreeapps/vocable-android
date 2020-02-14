package com.willowtree.vocable.keyboard

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import kotlinx.android.synthetic.main.fragment_keyboard.*

class KeyboardFragment : BaseFragment() {

    private val allViews = mutableListOf<View>()

    override fun getLayout(): Int = R.layout.fragment_keyboard

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(keyboard_parent)
        }
        return allViews
    }

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