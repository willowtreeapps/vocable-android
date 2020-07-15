package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.EditPhrasesViewModelFactory
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditKeyboardBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import java.util.*

class EditPhrasesKeyboardFragment : EditKeyboardFragment() {

    companion object {
        private const val KEY_PHRASE = "KEY_PHRASE"
        private const val KEY_CATEGORY = "KEY_CATEGORY"

        fun newInstance(phrase: Phrase?, category: Category): EditPhrasesKeyboardFragment {
            return EditPhrasesKeyboardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_PHRASE, phrase)
                    putParcelable(KEY_CATEGORY,category)
                }
            }
        }
    }

    private var phrase: Phrase? = null
    private var addNewPhrase = false
    private lateinit var category: Category
    private lateinit var viewModel: EditPhrasesViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getParcelable<Phrase>(KEY_PHRASE)?.let {
            phrase = it
        }

        arguments?.getParcelable<Category>(KEY_CATEGORY)?.let {
            category = it
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.action = {
            val textChanged = binding.keyboardInput.text.toString() != localizedResourceUtility.getTextFromPhrase(phrase)
            if (!textChanged || isDefaultTextVisible() || addNewPhrase) {
                parentFragmentManager.popBackStack()
            } else {
                showConfirmationDialog()
            }
        }

        binding.saveButton.action = {
            if (!isDefaultTextVisible()) {
                binding.keyboardInput.text.let { text ->
                    if (text.isNotBlank()) {
                        val phraseUtterance =
                            phrase?.localizedUtterance?.toMutableMap()?.apply {
                                put(Locale.getDefault().toString(), text.toString())
                            }
                        phrase?.localizedUtterance = phraseUtterance ?: mapOf()
                        if (phrase == null) {
                            viewModel.addNewPhrase(text.toString())
                            addNewPhrase = true
                        } else {
                            phrase?.let { updatedPhrase ->
                                viewModel.updatePhrase(updatedPhrase)
                                addNewPhrase = false
                            }
                        }

                    }
                }
            }
        }

        val phraseText = localizedResourceUtility.getTextFromPhrase(phrase)
        val inputText = phraseText.ifEmpty { getString(R.string.keyboard_select_letters) }

        binding.keyboardInput.setText(inputText)

        (binding.phraseSavedView.root as TextView).apply {
            if (phrase == null) {
                setText(R.string.new_phrase_saved)
            } else {
                setText(R.string.changes_saved)
            }
        }


        viewModel = ViewModelProviders.of(
            requireActivity(),
            EditPhrasesViewModelFactory(category)
        ).get(EditPhrasesViewModel::class.java)

        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
            binding.phraseSavedView.root.isVisible = it ?: false
        })
    }
}