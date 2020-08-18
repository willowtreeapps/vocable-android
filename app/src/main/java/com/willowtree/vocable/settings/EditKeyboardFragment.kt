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
import androidx.navigation.findNavController
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

abstract class EditKeyboardFragment : BaseFragment<FragmentEditKeyboardBinding>() {

    companion object {
        private const val KEY_USER_INPUT = "KEY_USER_INPUT"
    }

    override val bindingInflater: BindingInflater<FragmentEditKeyboardBinding> = FragmentEditKeyboardBinding::inflate
    private lateinit var keys: Array<String>
    internal val localizedResourceUtility: LocalizedResourceUtility by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

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

    fun isDefaultTextVisible(): Boolean {
        return binding.keyboardInput.text.toString() == getString(R.string.keyboard_select_letters)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        // Restore user input on config change
        savedInstanceState?.apply { binding.keyboardInput.setText(getString(KEY_USER_INPUT)) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_USER_INPUT, binding.keyboardInput.text.toString())
    }

    fun showConfirmationDialog() {
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
                findNavController().popBackStack()
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

    override fun getAllViews(): List<View> = emptyList()
}