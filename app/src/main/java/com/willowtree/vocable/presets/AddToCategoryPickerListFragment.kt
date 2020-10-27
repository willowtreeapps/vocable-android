package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentAddToCategoryListBinding
import com.willowtree.vocable.presets.adapter.AddToCategoryPickerViewModel
import com.willowtree.vocable.presets.adapter.CustomCategoryAdapter
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.ItemOffsetDecoration
import org.koin.android.ext.android.inject

class AddToCategoryPickerListFragment : BaseFragment<FragmentAddToCategoryListBinding>() {

    companion object {
        private const val KEY_PHRASE_STRING = "KEY_PHRASE_STRING"
        private const val KEY_CATEGORIES = "KEY_CATEGORIES"

        fun newInstance(
            phraseString: String,
            categories: List<Category>
        ): AddToCategoryPickerListFragment {
            return AddToCategoryPickerListFragment().apply {
                arguments = bundleOf(
                    KEY_PHRASE_STRING to phraseString,
                    KEY_CATEGORIES to ArrayList(categories)
                )
            }
        }
    }

    private val viewModel: AddToCategoryPickerViewModel by inject()
    private lateinit var phraseString: String

    override val bindingInflater: BindingInflater<FragmentAddToCategoryListBinding> =
        FragmentAddToCategoryListBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(KEY_PHRASE_STRING)?.let {
            phraseString = it
        }

        val categories = arguments?.getParcelableArrayList<Category>(KEY_CATEGORIES)

        categories?.let {
            val numColumns = resources.getInteger(R.integer.custom_category_columns)
            val numRows = resources.getInteger(R.integer.custom_category_rows)

            with(binding.categoryList) {
                layoutManager = GridLayoutManager(requireContext(), numColumns)
                addItemDecoration(
                    ItemOffsetDecoration(
                        requireContext(),
                        R.dimen.edit_category_phrase_button_margin,
                        it.size
                    )
                )
                setHasFixedSize(true)

                adapter = CustomCategoryAdapter(
                    mapOf(),
                    numRows,
                    onCategoryToggle
                )
            }

            viewModel.buildCategoryList(phraseString, it)
        }

        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
            binding.phraseSavedView.root.isVisible = it
        })

        viewModel.showPhraseDeleted.observe(viewLifecycleOwner, Observer {
            binding.phraseSavedView.root.isVisible = it
        })

        viewModel.categoryMap.observe(viewLifecycleOwner, Observer {
            (binding.categoryList.adapter as CustomCategoryAdapter).setMap(it)
        })
    }

    private val onCategoryToggle = { category: Category, isChecked: Boolean ->
        if (isChecked) {
            binding.phraseSavedView.root.text =
                getString(R.string.saved_successfully, viewModel.getUpdatedCategoryName(category))
        } else {
            binding.phraseSavedView.root.text =
                getString(R.string.removed_successfully, viewModel.getUpdatedCategoryName(category))
        }

        viewModel.handleCategoryToggled(phraseString, category, isChecked)
    }

    override fun getAllViews(): List<View> = emptyList()
}