package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.CategoryEditButtonBinding
import com.willowtree.vocable.databinding.FragmentEditCategoriesListBinding
import com.willowtree.vocable.room.Category

class EditCategoriesListFragment : BaseFragment() {

    companion object {
        private const val KEY_CATEGORIES = "KEY_CATEGORIES"

        fun newInstance(categories: List<Category>): EditCategoriesListFragment {
            return EditCategoriesListFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(KEY_CATEGORIES, ArrayList(categories))
                }
            }
        }
    }

    private var binding: FragmentEditCategoriesListBinding? = null
    private var maxEditCategories = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentEditCategoriesListBinding.inflate(inflater, container, false)
        maxEditCategories = resources.getInteger(R.integer.max_edit_categories)

        val categories =
            arguments?.getParcelableArrayList<Category>(KEY_CATEGORIES)
        categories?.forEachIndexed { _, category ->
            val categoryView =
                CategoryEditButtonBinding.inflate(
                    inflater,
                    binding?.categoryEditButtonContainer,
                    false
                )
            categoryView.categoryName.text = category.name
            binding?.categoryEditButtonContainer?.addView(categoryView.root)

            categoryView.editCategorySelectButton.action = {

            }
        }

        categories?.let {
            // Add invisible views to fill out the rest of the space
            for (i in 0 until maxEditCategories - it.size) {
                val hiddenButton =
                    CategoryEditButtonBinding.inflate(inflater, binding?.categoryEditButtonContainer, false)
                binding?.categoryEditButtonContainer?.addView(hiddenButton.root.apply {
                    isEnabled = false
                    visibility = View.INVISIBLE
                })
            }
        }
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }
}