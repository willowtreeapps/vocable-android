package com.willowtree.vocable.settings.customcategories

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentCustomCategoryPhraseListBinding
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.settings.EditCategoriesViewModel
import com.willowtree.vocable.settings.EditCategoryOptionsFragmentDirections
import com.willowtree.vocable.settings.customcategories.adapter.CustomCategoryPhraseAdapter
import com.willowtree.vocable.utils.ItemOffsetDecoration

class CustomCategoryPhraseListFragment : BaseFragment<FragmentCustomCategoryPhraseListBinding>() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"

        fun newInstance(phrases: List<Phrase>): CustomCategoryPhraseListFragment {
            return CustomCategoryPhraseListFragment().apply {
                arguments = bundleOf(KEY_PHRASES to ArrayList(phrases))
            }
        }
    }

    private val onPhraseEdit = { phrase: Phrase ->
        val action = EditCategoryOptionsFragmentDirections.actionEditCategoryOptionsFragmentToEditPhrasesKeyboardFragment(phrase)
        findNavController().navigate(action)
    }

    private val onPhraseDelete = { phrase: Phrase ->
        // TODO: Handle deleting the phrase
    }

    override val bindingInflater: BindingInflater<FragmentCustomCategoryPhraseListBinding> =
        FragmentCustomCategoryPhraseListBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val numColumns = resources.getInteger(R.integer.custom_category_phrase_columns)
        val numRows = resources.getInteger(R.integer.custom_category_phrase_rows)

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
                adapter = CustomCategoryPhraseAdapter(it, numRows, onPhraseEdit, onPhraseDelete)
            }
        }
    }

    override fun getAllViews(): List<View> = emptyList()
}
