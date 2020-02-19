package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.customviews.CategoryButton
import com.willowtree.vocable.customviews.PointerListener
import kotlinx.android.synthetic.main.categories_fragment.*

class CategoriesFragment : BaseFragment() {

    companion object {
        const val KEY_CATEGORIES = "KEY_CATEGORIES"

        fun newInstance(categories: List<String>): CategoriesFragment {
            return CategoriesFragment().apply {
                arguments = Bundle().apply {
                    putStringArray(KEY_CATEGORIES, categories.toTypedArray())
                }
            }
        }
    }

    private val allViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val buttonContainer = view?.findViewById<LinearLayout>(R.id.category_button_container)
        arguments?.getStringArray(KEY_CATEGORIES)?.forEach {
            val categoryButton = layoutInflater.inflate(
                R.layout.category_button,
                buttonContainer,
                false
            ) as ActionButton
            categoryButton.text = it
            buttonContainer?.addView(categoryButton)
        }
        return view
    }

    override fun getLayout(): Int = R.layout.categories_fragment

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(category_button_container)
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