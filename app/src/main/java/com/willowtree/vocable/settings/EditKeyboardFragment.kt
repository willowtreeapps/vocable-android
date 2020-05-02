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
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.databinding.FragmentEditKeyboardBinding
import com.willowtree.vocable.databinding.KeyboardKeyLayoutBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.LocaleUtils
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.android.ext.android.inject
import java.util.*

class EditKeyboardFragment : BaseFragment<FragmentEditKeyboardBinding>() {

    companion object {
        private const val KEY_PHRASE = "KEY_PHRASE"
        private const val KEY_IS_EDITING = "KEY_IS_EDITING"
        private const val KEY_CATEGORY = "KEY_CATEGORY"
        private const val KEY_IS_CATEGORY = "KEY_IS_CATEGORY"
        private const val KEY_USER_INPUT = "KEY_USER_INPUT"

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

    override val bindingInflater: BindingInflater<FragmentEditKeyboardBinding> = FragmentEditKeyboardBinding::inflate
    private lateinit var viewModel: EditPhrasesViewModel
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel
    private lateinit var keys: Array<String>
    private var phrase: Phrase? = null
    private var category: Category? = null
    private var isCategory = false
    private var addNewPhrase = false
    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        keys = resources.getStringArray(R.array.keyboard_keys)
        arguments?.getParcelable<Phrase>(KEY_PHRASE)?.let {
            phrase = it
        }
        arguments?.getParcelable<Category>(KEY_CATEGORY)?.let {
            category = it
        }

        isCategory = arguments?.getBoolean(KEY_IS_CATEGORY) ?: false

        keys = resources.getStringArray(R.array.keyboard_keys)

        populateKeys()

        return binding.root
    }

    private fun populateKeys() {
        keys.withIndex().forEach {
            with(
                KeyboardKeyLayoutBinding.inflate(
                    layoutInflater,
                    binding.keyboardKeyHolder,
                    true
                ).root as ActionButton
            ) {
                text = it.value
                action = {
                    //This action mimics sentence capitalization
                    //Example: "This is what's going on in here. Do you get it? Some letters are capitalized."
                    val currentText = binding.keyboardInput.text?.toString() ?: ""
                    if (isDefaultTextVisible()) {
                        binding.keyboardInput.text = null
                        binding.keyboardInput.append(text?.toString())
                    } else if (currentText.endsWith(". ") || currentText.endsWith("? ")) {
                        binding.keyboardInput.append(text?.toString())
                    } else {
                        binding.keyboardInput.append(
                            text?.toString()?.toLowerCase(Locale.getDefault())
                        )
                    }
                }
            }
        }
    }

    private fun isDefaultTextVisible(): Boolean {
        return binding.keyboardInput.text.toString() == getString(R.string.keyboard_select_letters)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.action = {
            val isEditing = arguments?.getBoolean(KEY_IS_EDITING) ?: false
            val textChanged = binding.keyboardInput.text.toString() != phrase?.let { localizedResourceUtility.getTextFromPhrase(it) }
            val categoryTextChanged =
                binding.keyboardInput.text.toString() != category?.let { localizedResourceUtility.getTextFromCategory(it) }
            if (isCategory && !categoryTextChanged || isDefaultTextVisible()) {
                parentFragmentManager.popBackStack()
            } else if (!textChanged || isDefaultTextVisible() || addNewPhrase) {
                parentFragmentManager.popBackStack()
            } else {
                showConfirmationDialog()
            }
        }

        val isEditing = arguments?.getBoolean(KEY_IS_EDITING) ?: false

        binding.saveButton.action = {
            if (isCategory && !isEditing && !isDefaultTextVisible()) {
                binding.keyboardInput.text?.let { text ->
                    if (text.isNotBlank()) {
                        editCategoriesViewModel.addNewCategory(text.toString())
                        parentFragmentManager.popBackStack()
                    }
                }
            } else if (isCategory && isEditing && !isDefaultTextVisible()) {
                binding.keyboardInput.text.let { text ->
                    val categoryName = category?.localizedName?.toMutableMap()?.apply {
                        put(Locale.getDefault().toString(), text.toString())
                    }
                    category?.localizedName = categoryName ?: mapOf()
                    category?.let { updatedCategory ->
                        editCategoriesViewModel.updateCategory(updatedCategory)
                    } ?: editCategoriesViewModel.addNewCategory(text.toString())
                }
            } else if (!isDefaultTextVisible()) {
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
        val phraseText = phrase?.let { localizedResourceUtility.getTextFromPhrase(it) }
        val categoryText = category?.let { localizedResourceUtility.getTextFromCategory(it) }
        val inputText = if (isCategory) {
            categoryText ?: getString(R.string.keyboard_select_letters)
        } else {
            phraseText ?: getString(R.string.keyboard_select_letters)
        }

        binding.keyboardInput.setText(inputText)

        binding.keyboardInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // no-op
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no-op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.saveButton.isEnabled = !isDefaultTextVisible()
            }

        })

        binding.saveButton.isEnabled = false

        binding.keyboardClearButton.action = {
            binding.keyboardInput.setText(R.string.keyboard_select_letters)
        }

        binding.keyboardSpaceButton.action = {
            if (!isDefaultTextVisible() && !binding.keyboardInput.text.endsWith(' ')) {
                binding.keyboardInput.append(" ")
            }
        }

        binding.keyboardBackspaceButton.action = {
            if (!isDefaultTextVisible()) {
                binding.keyboardInput.let { keyboardInput ->
                    keyboardInput.setText(keyboardInput.text.toString().dropLast(1))
                    if (keyboardInput.text.isNullOrEmpty()) {
                        keyboardInput.setText(R.string.keyboard_select_letters)
                    }
                }
            }
        }

        (binding.phraseSavedView.root as TextView).apply {
            if (arguments?.getBoolean(KEY_IS_EDITING) == true) {
                setText(R.string.changes_saved)
            } else {
                setText(R.string.new_phrase_saved)
            }
        }

        // Restore user input on config change
        savedInstanceState?.apply { binding.keyboardInput.setText(getString(KEY_USER_INPUT)) }

        if (isCategory) {
            editCategoriesViewModel = ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory()
            ).get(EditCategoriesViewModel::class.java)
        } else {
            viewModel = ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory()
            ).get(EditPhrasesViewModel::class.java)
        }
        subscribeToViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_USER_INPUT, binding.keyboardInput.text.toString())
    }

    private fun showConfirmationDialog() {
        setSettingsButtonsEnabled(false)
        binding.editConfirmation.dialogTitle.text = getString(R.string.are_you_sure)
        binding.editConfirmation.dialogMessage.text = getString(R.string.back_warning)
        binding.editConfirmation.dialogPositiveButton.apply {
            text = getString(R.string.contiue_editing)
            action = {
                toggleDialogVisibility(false)
                setSettingsButtonsEnabled(true)
            }
        }
        binding.editConfirmation.dialogNegativeButton.apply {
            text = getString(R.string.discard)
            action = {
                parentFragmentManager.popBackStack()
            }
        }
        toggleDialogVisibility(true)
    }

    private fun setSettingsButtonsEnabled(enable: Boolean) {
        binding.apply {
            backButton.isEnabled = enable
            saveButton.isEnabled = enable
            keyboardBackspaceButton.isEnabled = enable
            keyboardSpaceButton.isEnabled = enable
            keyboardClearButton.isEnabled = enable
            keyboardInput.isEnabled = enable
            keyboardKeyHolder.children.forEach {
                it.isEnabled = enable
            }
        }
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.editConfirmation.root.isVisible = visible
    }

    private fun subscribeToViewModel() {
        if (!isCategory) {
            viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
                binding.phraseSavedView.root.isVisible = it ?: false
            })
        }
    }

    override fun getAllViews(): List<View> = emptyList()
}