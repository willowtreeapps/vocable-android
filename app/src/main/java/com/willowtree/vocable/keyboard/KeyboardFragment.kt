package com.willowtree.vocable.keyboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableButton
import kotlinx.android.synthetic.main.fragment_keyboard.*

class KeyboardFragment : BaseFragment() {

    companion object {
        private val KEYS = listOf(
            "Q",
            "W",
            "E",
            "R",
            "T",
            "Y",
            "U",
            "I",
            "O",
            "P",
            "A",
            "S",
            "D",
            "F",
            "G",
            "H",
            "J",
            "K",
            "L",
            "Z",
            "X",
            "C",
            "V",
            "B",
            "N",
            "M",
            "space",
            "speak"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val gridLayout = view?.findViewById<GridLayout>(R.id.keyboard_key_holder)

        KEYS.withIndex().forEach {
            layoutInflater.inflate(R.layout.keyboard_key_layout, gridLayout, true)
            val key = (gridLayout?.getChildAt(it.index) as VocableButton).apply {
                text = it.value
            }
            when (it.value) {
                "speak", "space" -> {
                    key.layoutParams = (key.layoutParams as GridLayout.LayoutParams).apply {
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2, 1.5f)
                    }
                }
            }
        }

        return view
    }

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