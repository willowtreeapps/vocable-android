package com.willowtree.vocable.settings.customcategories

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentCustomCategoryPhraseListBinding
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.settings.EditCategoryPhrasesFragmentDirections
import com.willowtree.vocable.settings.customcategories.adapter.CustomCategoryPhraseAdapter
import com.willowtree.vocable.utils.ItemOffsetDecoration
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomCategoryPhraseListFragment : BaseFragment<FragmentCustomCategoryPhraseListBinding>() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"
        private const val KEY_CATEGORY = "KEY_CATEGORY"

        fun newInstance(
            phrases: List<Phrase>,
            category: Category
        ): CustomCategoryPhraseListFragment {
            return CustomCategoryPhraseListFragment().apply {
                arguments = bundleOf(KEY_PHRASES to ArrayList(phrases), KEY_CATEGORY to category)
            }
        }
    }

    private val viewModel: CustomCategoryPhraseViewModel by viewModel()
    private lateinit var category: Category

    private val onPhraseEdit = { phrase: Phrase ->
        val action = EditCategoryPhrasesFragmentDirections.actionEditCategoryPhrasesFragmentToEditPhrasesKeyboardFragment(phrase)
        if (findNavController().currentDestination?.id == R.id.editCategoryPhrasesFragment) {
            findNavController().navigate(action)
        }
    }

    private val onPhraseDelete = { phrase: Phrase ->
        showDeletePhraseDialog(phrase)
    }

    override val bindingInflater: BindingInflater<FragmentCustomCategoryPhraseListBinding> =
        FragmentCustomCategoryPhraseListBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getParcelable<Category>(KEY_CATEGORY)?.let {
            category = it
        }

        val numColumns = resources.getInteger(R.integer.custom_category_phrase_columns)

        val phrases = arguments?.getParcelableArrayList<Phrase>(KEY_PHRASES)

        phrases?.let {
            with(binding.customCategoryPhraseHolder) {
                layoutManager = GridLayoutManager(requireContext(), numColumns)
                addItemDecoration(
                    ItemOffsetDecoration(
                        requireContext(),
                        R.dimen.edit_category_phrase_button_margin,
                        it.size
                    )
                )
                setHasFixedSize(true)
                adapter = CustomCategoryPhraseAdapter(it, onPhraseEdit, onPhraseDelete)
            }
        }
    }

    private fun showDeletePhraseDialog(phrase: Phrase) {

        with(binding.deleteConfirmation) {
            dialogTitle.setText(R.string.are_you_sure)

            dialogMessage.setText(R.string.delete_warning)

            with(dialogPositiveButton) {
                setText(R.string.delete)
                action = {
                    viewModel.deletePhraseFromCategory(phrase)
                    toggleDialogVisibility(false)
                }
            }

            with(dialogNegativeButton) {
                setText(R.string.settings_dialog_cancel)
                action = {
                    toggleDialogVisibility(false)
                }
            }
        }

        toggleDialogVisibility(true)
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.deleteConfirmation.root.isVisible = visible
    }

    override fun getAllViews(): List<View> = emptyList()
}
