package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentEditKeyboardBinding
import com.willowtree.vocable.databinding.KeyboardKeyLayoutBinding
import com.willowtree.vocable.room.Phrase
import java.util.*

class EditKeyboardFragment : BaseFragment() {

    companion object {
        private const val KEY_PHRASE = "KEY_PHRASE"
        private const val KEY_IS_EDITING = "KEY_IS_EDITING"

        fun newInstance(phrase: Phrase): EditKeyboardFragment {
            return EditKeyboardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_PHRASE, phrase)
                    putBoolean(KEY_IS_EDITING, true)
                }
            }
        }

        fun newInstance(isEditing: Boolean) = EditKeyboardFragment(). apply {
            arguments = bundleOf(KEY_IS_EDITING to isEditing)
        }
    }

    private lateinit var viewModel: EditPhrasesViewModel
    private var binding: FragmentEditKeyboardBinding? = null
    private lateinit var keys: Array<String>
    private var phrase: Phrase? = null

    private val allViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditKeyboardBinding.inflate(inflater, container, false)
        keys = resources.getStringArray(R.array.keyboard_keys)
        arguments?.getParcelable<Phrase>(KEY_PHRASE)?.let {
            phrase = it
        }

        populateKeys()

        return binding?.root
    }

    private fun populateKeys() {
        keys.withIndex().forEach {
            with(
                KeyboardKeyLayoutBinding.inflate(
                    layoutInflater,
                    binding?.keyboardKeyHolder,
                    true
                ).root as ActionButton
            ) {
                text = it.value
                action = {
                    //This action mimics sentence capitalization
                    //Example: "This is what's going on in here. Do you get it? Some letters are capitalized."
                    val currentText = binding?.keyboardInput?.text?.toString() ?: ""
                    if (isDefaultTextVisible()) {
                        binding?.keyboardInput?.text = null
                        binding?.keyboardInput?.append(text?.toString())
                    } else if (currentText.endsWith(". ") || currentText.endsWith("? ")) {
                        binding?.keyboardInput?.append(text?.toString())
                    } else {
                        binding?.keyboardInput?.append(text?.toString()?.toLowerCase(Locale.getDefault()))
                    }
                }
            }
        }
    }

    private fun isDefaultTextVisible(): Boolean {
        return binding?.keyboardInput?.text.toString() == getString(R.string.keyboard_select_letters)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.backButton?.action = {
            if (binding?.keyboardInput?.text.toString() == phrase?.utterance || arguments?.getBoolean(
                    KEY_IS_EDITING) == false) {
                parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, EditPresetsFragment())
                .addToBackStack(null)
                .commit()
            } else {
                showConfirmationDialog()
            }
        }

        binding?.saveButton?.let {
            it.action = {
                if (!isDefaultTextVisible()) {
                    binding?.keyboardInput?.text?.let { text ->
                        if (text.isNotBlank()) {
                            phrase?.utterance = text.toString()
                            phrase?.let {
                                viewModel.updatePhrase(it)
                            } ?: viewModel.addNewPhrase(text.toString())
                        }
                    }
                }
            }
        }

        binding?.keyboardInput?.setText(phrase?.utterance ?: getString(R.string.keyboard_select_letters))

        binding?.keyboardClearButton?.action = {
            binding?.keyboardInput?.setText(R.string.keyboard_select_letters)
        }

        binding?.keyboardSpaceButton?.action = {
            if (!isDefaultTextVisible() && binding?.keyboardInput?.text?.endsWith(' ') == false) {
                binding?.keyboardInput?.append(" ")
            }
        }

        binding?.keyboardBackspaceButton?.let {
            it.action = {
                if (!isDefaultTextVisible()) {
                    binding?.keyboardInput?.let { keyboardInput ->
                        keyboardInput.setText(keyboardInput.text.toString().dropLast(1))
                        keyboardInput.setSelection(
                            keyboardInput.text?.length ?: 0
                        )
                        if (keyboardInput.text.isNullOrEmpty()) {
                            keyboardInput.setText(R.string.keyboard_select_letters)
                        }
                    }
                }
            }
        }

        binding?.phraseSavedView?.root?.let {
            if (arguments?.getBoolean(KEY_IS_EDITING) == true) {
                (it as TextView).setText(R.string.changes_saved)
            } else {
                (it as TextView).setText(R.string.new_phrase_saved)
            }
        }

        viewModel = ViewModelProviders.of(requireActivity()).get(EditPhrasesViewModel::class.java)
        subscribeToViewModel()
    }

    private fun showConfirmationDialog() {
        setSettingsButtonsEnabled(false)
        binding?.editConfirmation?.dialogTitle?.text = getString(R.string.are_you_sure)
        binding?.editConfirmation?.dialogMessage?.text = getString(R.string.back_warning)
        binding?.editConfirmation?.dialogPositiveButton?.let {
            it.text = getString(R.string.contiue_editing)
            it.action = {
                toggleDialogVisibility(false)
                setSettingsButtonsEnabled(true)
            }
        }
        binding?.editConfirmation?.dialogNegativeButton?.let {
            it.text = getString(R.string.discard)
            it.action = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings_fragment_container, EditPresetsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        toggleDialogVisibility(true)
    }

    private fun setSettingsButtonsEnabled(enable: Boolean) {
        binding?.let {
            it.backButton.isEnabled = enable
            it.saveButton.isEnabled = enable
            it.keyboardBackspaceButton.isEnabled = enable
            it.keyboardSpaceButton.isEnabled = enable
            it.keyboardClearButton.isEnabled = enable
            it.keyboardInput.isEnabled = enable
            it.keyboardKeyHolder.children.forEach {
                it.isEnabled = enable
            }
        }
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding?.editConfirmation?.root?.let {
            it.isVisible = visible
        }
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
            binding?.phraseSavedView?.root?.isVisible = it ?: false
        })
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding?.editKeyboardParent)
        }
        return allViews
    }

    private fun getAllChildViews(viewGroup: ViewGroup?) {
        viewGroup?.children?.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}