package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.CategoryEditButtonBinding
import com.willowtree.vocable.databinding.FragmentEditCategoriesListBinding
import com.willowtree.vocable.room.Category
import kotlin.math.min

class EditCategoriesListFragment : BaseFragment() {

    companion object {
        private const val KEY_START_POSITION = "KEY_START_POSITION"
        private const val KEY_END_POSITION = "KEY_END_POSITION"
        private const val KEY_CATEGORIES_SUBLIST = "KEY_CATEGORIES_SUBLIST"

        fun newInstance(
            startPosition: Int,
            endPosition: Int
        ): EditCategoriesListFragment {
            return EditCategoriesListFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_START_POSITION, startPosition)
                    putInt(KEY_END_POSITION, endPosition)
                }
            }
        }
    }

    private var binding: FragmentEditCategoriesListBinding? = null
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel
    private var maxEditCategories = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentEditCategoriesListBinding.inflate(inflater, container, false)
        maxEditCategories = resources.getInteger(R.integer.max_edit_categories)

        val categories =
            arguments?.getParcelableArrayList<Category>(KEY_CATEGORIES_SUBLIST)
        categories?.forEachIndexed { _, category ->
            val categoryView =
                CategoryEditButtonBinding.inflate(
                    inflater,
                    binding?.categoryEditButtonContainer,
                    false
                )
            categoryView.categoryName.text = category.getLocalizedText()
            binding?.categoryEditButtonContainer?.addView(categoryView.root)

            categoryView.editCategorySelectButton.action = {
                requireActivity()
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.settings_fragment_container,
                        EditCategoryOptionsFragment.newInstance(category)
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }

        categories?.let {
            // Add invisible views to fill out the rest of the space
            for (i in 0 until maxEditCategories - it.size) {
                val hiddenButton =
                    CategoryEditButtonBinding.inflate(
                        inflater,
                        binding?.categoryEditButtonContainer,
                        false
                    )
                binding?.categoryEditButtonContainer?.addView(hiddenButton.root.apply {
                    isEnabled = false
                    visibility = View.INVISIBLE
                })
            }
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editCategoriesViewModel =
            ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory(
                    getString(R.string.category_123_id),
                    getString(R.string.category_my_sayings_id)
                )
            ).get(EditCategoriesViewModel::class.java)
        subscribeToViewModel()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.orderCategoryList.observe(viewLifecycleOwner, Observer {

        })
    }
}