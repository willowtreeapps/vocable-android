package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.databinding.CategoryEditButtonBinding
import com.willowtree.vocable.databinding.FragmentEditCategoriesListBinding
import com.willowtree.vocable.room.Category

class EditCategoriesListFragment : BaseFragment() {

    companion object {
        private const val KEY_CATEGORIES = "KEY_CATEGORIES"

        fun newInstance(categories: List<Category>): EditCategoriesFragment {
            return EditCategoriesFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(KEY_CATEGORIES, ArrayList(categories))
                }
            }
        }
    }

    private var binding: FragmentEditCategoriesListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentEditCategoriesListBinding.inflate(inflater, container, false)

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