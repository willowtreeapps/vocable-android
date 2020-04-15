package com.willowtree.vocable.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.databinding.FragmentEditKeyboardBinding
import com.willowtree.vocable.databinding.KeyboardKeyLayoutBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import java.util.*

class EditKeyboardFragment : BaseFragment() {

    companion object {
        private const val KEY_PHRASE = "KEY_PHRASE"
        private const val KEY_IS_EDITING = "KEY_IS_EDITING"
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_IS_CATEGORY = "KEY_IS_CATEGORY"

        fun newInstance(phrase: Phrase): EditKeyboardFragment {
            return EditKeyboardFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_PHRASE, phrase)
                    putBoolean(KEY_IS_EDITING, true)
                    putBoolean(KEY_IS_CATEGORY, false)
                }
            }
        }

        fun newInstance(isEditing: Boolean) = EditKeyboardFragment().apply {
            arguments = bundleOf(KEY_IS_EDITING to isEditing)
        }

        fun newInstance(category: Category) = EditKeyboardFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_CATEGORY, category)
                putBoolean(KEY_IS_EDITING, true)
                putBoolean(KEY_IS_CATEGORY, true)
            }
        }

        fun newCreateCategoryInstance(): EditKeyboardFragment = EditKeyboardFragment().apply {
            arguments = bundleOf(KEY_IS_CATEGORY to true)
        }
    }

    private lateinit var viewModel: EditPhrasesViewModel
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel
    private var binding: FragmentEditKeyboardBinding? = null
    private lateinit var keys: Array<String>
    private var phrase: Phrase? = null
    private var category: Category? = null
    private var isCategory = false

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
        arguments?.getParcelable<Category>(KEY_CATEGORY)?.let {
            category = it
        }
        isCategory = arguments?.getBoolean(KEY_IS_CATEGORY) ?: false

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
                        binding?.keyboardInput?.append(
                            text?.toString()?.toLowerCase(Locale.getDefault())
                        )
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
            val isEditing = arguments?.getBoolean(KEY_IS_EDITING) ?: false
            val textChanged = binding?.keyboardInput?.text.toString() != phrase?.getLocalizedText()
            val categoryTextChanged = binding?.keyboardInput?.text.toString() != category?.getLocalizedText()
            if (isCategory && !categoryTextChanged || isDefaultTextVisible()) {
                parentFragmentManager.popBackStack()
            } else if (isEditing && !textChanged || isDefaultTextVisible()) {
                parentFragmentManager.popBackStack()
            } else {
                showConfirmationDialog()
            }
        }

        binding?.saveButton?.let {
            val isEditing = arguments?.getBoolean(KEY_IS_EDITING) ?: false
            it.action = {
                if (isCategory && !isEditing && !isDefaultTextVisible()) {
                    binding?.keyboardInput?.text?.let { text ->
                        if (text.isNotBlank()) {
                            editCategoriesViewModel.addNewCategory(text.toString())
                            parentFragmentManager.popBackStack()
                        }
                    }
                } else if (isCategory && isEditing && !isDefaultTextVisible()) {
                    binding?.keyboardInput?.text?.let { text ->
                        val categoryName = category?.localizedName?.toMutableMap()?.apply {
                            put(Locale.getDefault().toString(), text.toString())
                        }
                        category?.localizedName = categoryName ?: mapOf()
                        category?.let { updatedCategory ->
                            editCategoriesViewModel.updateCategory(updatedCategory)
                        } ?: editCategoriesViewModel.addNewCategory(text.toString())
                    }
                } else if (!isDefaultTextVisible()) {
                    binding?.keyboardInput?.text?.let { text ->
                        if (text.isNotBlank()) {
                            val phraseUtterance =
                                phrase?.localizedUtterance?.toMutableMap()?.apply {
                                    put(Locale.getDefault().toString(), text.toString())
                                }
                            phrase?.localizedUtterance = phraseUtterance ?: mapOf()
                            phrase?.let { updatedPhrase ->
                                viewModel.updatePhrase(updatedPhrase)
                            } ?: viewModel.addNewPhrase(text.toString())
                        }
                    }
                }
            }
        }

        binding?.keyboardInput?.setText(
            if ( !isCategory && phrase?.getLocalizedText().isNullOrEmpty()) {
                getString(R.string.keyboard_select_letters)
            } else if (isCategory && category?.getLocalizedText().isNullOrEmpty() ) {
                getString(R.string.keyboard_select_letters)
            }  else if (isCategory) {
                category?.getLocalizedText()
            } else {
                phrase?.getLocalizedText()
            }
        )

        binding?.keyboardInput?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // no-op
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no-op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding?.saveButton?.isEnabled = !isDefaultTextVisible()
            }

        })

        binding?.saveButton?.isEnabled = false

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

        if (isCategory) {
            editCategoriesViewModel = ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory(
                    getString(R.string.category_123_id),
                    getString(R.string.category_my_sayings_id)
                )
            ).get(EditCategoriesViewModel::class.java)
        } else {
            viewModel = ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory(
                    getString(R.string.category_123_id),
                    getString(R.string.category_my_sayings_id)
                )
            ).get(EditPhrasesViewModel::class.java)
        }
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
                if (isCategory) {
                    parentFragmentManager.popBackStack()
                } else {
                    parentFragmentManager.popBackStack()
                }
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
        if (!isCategory) {
            viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
                binding?.phraseSavedView?.root?.isVisible = it ?: false
            })
        }
    }

    override fun getAllViews(): List<View> = emptyList()

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}