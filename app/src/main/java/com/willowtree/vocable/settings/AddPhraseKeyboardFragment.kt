package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.willowtree.vocable.R
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.PhraseDto

class AddPhraseKeyboardFragment : EditKeyboardFragment() {

    companion object {
        private const val KEY_PHRASE = "KEY_PHRASE"

        fun newInstance(phrase: PhraseDto?): EditPhrasesKeyboardFragment {
            return EditPhrasesKeyboardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_PHRASE, phrase)
                }
            }
        }
    }

    private val viewModel: AddPhraseViewModel by viewModels()
    private val args: AddPhraseKeyboardFragmentArgs by navArgs()
    private lateinit var category: Category
    private var savedPhrase = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        category = args.category
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.action = {
            if (isDefaultTextVisible() || binding.keyboardInput.text.toString() == savedPhrase) {
                findNavController().popBackStack()
            } else {
                showConfirmationDialog()
            }
        }

        binding.saveButton.action = {
            if (!isDefaultTextVisible()) {
                binding.keyboardInput.text.let { text ->
                    if (text.isNotBlank()) {
                        viewModel.addNewPhrase(text.toString(), category.categoryId)
                        savedPhrase = text.toString()
                    }
                }
            }
        }

        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
            if (it) {
                Toast.makeText(context, R.string.new_phrase_saved, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                with(binding.editConfirmation) {
                    root.isVisible = true
                    dialogTitle.isInvisible = true
                    dialogNegativeButton.isVisible = false
                    dialogMessage.setText(R.string.duplicate_phrase)
                    dialogPositiveButton.setText(android.R.string.ok)
                    dialogPositiveButton.action = {
                        root.isVisible = false
                    }
                }
            }
        })
    }
}
