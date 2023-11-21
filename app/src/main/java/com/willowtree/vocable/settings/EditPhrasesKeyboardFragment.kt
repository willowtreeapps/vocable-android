package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.willowtree.vocable.R
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.locale.LocalesWithText

class EditPhrasesKeyboardFragment : EditKeyboardFragment() {

    private lateinit var phrase: Phrase
    private var addNewPhrase = false
    private val viewModel: EditPhrasesViewModel by viewModels({ requireActivity() })
    private val args by navArgs<EditPhrasesKeyboardFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        phrase = args.phrase

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.action = {
            val textChanged =
                binding.keyboardInput.text.toString() != localizedResourceUtility.getTextFromPhrase(
                    phrase
                )
            if (!textChanged || isDefaultTextVisible() || addNewPhrase) {
                findNavController().popBackStack()
            } else {
                showConfirmationDialog()
            }
        }

        binding.saveButton.action = {
            if (!isDefaultTextVisible()) {
                binding.keyboardInput.text.let { text ->
                    if (text.isNotBlank()) {
                        val languageWithText = phrase.localizedUtterance ?: LocalesWithText(emptyMap())
                        viewModel.updatePhrase(phrase.phraseId, languageWithText)
                        addNewPhrase = false
                    }
                }
            }
        }

        val phraseText = localizedResourceUtility.getTextFromPhrase(phrase)
        val inputText = phraseText.ifEmpty { getString(R.string.keyboard_select_letters) }

        binding.keyboardInput.setText(inputText)

        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner) {
            if (it) {
                if (phrase == null) {
                    Toast.makeText(context, R.string.new_phrase_saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.changes_saved, Toast.LENGTH_SHORT).show()
                }
                viewModel.phraseToFalse()
                findNavController().popBackStack()
            }
        }
    }
}
