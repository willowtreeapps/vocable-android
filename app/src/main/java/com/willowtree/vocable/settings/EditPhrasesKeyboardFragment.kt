package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditKeyboardBinding
import com.willowtree.vocable.room.Phrase
import java.util.*

class EditPhrasesKeyboardFragment : EditKeyboardFragment() {

    companion object {
        private const val KEY_PHRASE = "KEY_PHRASE"

        fun newInstance(phrase: Phrase?): EditKeyboardFragment {
            return EditKeyboardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_PHRASE, phrase)
                }
            }
        }
    }

    override val bindingInflater: BindingInflater<FragmentEditKeyboardBinding> =
        FragmentEditKeyboardBinding::inflate
    private var phrase: Phrase? = null
    private var addNewPhrase = false
    private lateinit var viewModel: EditPhrasesViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getParcelable<Phrase>(KEY_PHRASE)?.let {
            phrase = it
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.action = {
            val textChanged = binding.keyboardInput.text.toString() != phrase?.getLocalizedText()
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

        val inputText = if (phrase?.getLocalizedText().isNullOrEmpty()) {
            getString(R.string.keyboard_select_letters)
        } else {
            phrase?.getLocalizedText()
        }

        binding.keyboardInput.setText(inputText)

        viewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory(
                getString(R.string.category_123_id),
                getString(R.string.category_my_sayings_id)
            )
        ).get(EditPhrasesViewModel::class.java)

        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
            binding.phraseSavedView.root.isVisible = it ?: false
        })
    }
}