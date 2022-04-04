package com.willowtree.vocable.keyboard

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentKeyboardBinding
import com.willowtree.vocable.keyboard.adapter.KeyboardAdapter
import com.willowtree.vocable.utils.ItemOffsetDecoration
import com.willowtree.vocable.utils.VocableTextToSpeech
import java.util.*

class KeyboardFragment : BaseFragment<FragmentKeyboardBinding>() {

    override val bindingInflater: BindingInflater<FragmentKeyboardBinding> =
        FragmentKeyboardBinding::inflate
    private lateinit var viewModel: KeyboardViewModel
    private lateinit var keys: Array<String>

    private val keyAction = { keyText: String ->
        val currentText = binding.keyboardInput.text?.toString() ?: ""
        if (isDefaultTextVisible()) {
            binding.keyboardInput.text = null
            binding.keyboardInput.append(keyText)
        } else if (currentText.endsWith(". ") || currentText.endsWith("? ")) {
            binding.keyboardInput.append(keyText)
        } else {
            binding.keyboardInput.append(
                keyText.toLowerCase(Locale.getDefault())
            )
        }

        viewModel.currentText = binding.keyboardInput.text.toString()
        binding.keyboardInput.setSelection(binding.keyboardInput.text.length)
    }

    private fun isDefaultTextVisible(): Boolean {
        return binding.keyboardInput.text.toString() == getString(R.string.keyboard_select_letters)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        keys = resources.getStringArray(R.array.keyboard_keys)

        val numColumns = resources.getInteger(R.integer.keyboard_columns)
        val numRows = resources.getInteger(R.integer.keyboard_rows)

        with(binding.keyboardKeyHolder) {
            layoutManager = GridLayoutManager(requireContext(), numColumns)
            addItemDecoration(
                ItemOffsetDecoration(
                    requireContext(),
                    R.dimen.keyboard_key_margin,
                    keys.size
                )
            )
            adapter = KeyboardAdapter(keys, keyAction, numRows)
        }

        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            binding.speakerIcon.isVisible = it
        })

        binding.actionButtonContainer.presetsButton.action = {
            if (findNavController().currentDestination?.id == R.id.keyboardFragment) {
                findNavController().navigate(R.id.action_keyboardFragment_to_presetsFragment)
            }
        }

        binding.actionButtonContainer.settingsButton.action = {
            if (findNavController().currentDestination?.id == R.id.keyboardFragment) {
                findNavController().navigate(R.id.action_keyboardFragment_to_settingsFragment)
            }
        }

        binding.actionButtonContainer.saveButton.action = {
            if (!isDefaultTextVisible()) {
                binding.keyboardInput.text?.let { text ->
                    if (text.isNotBlank()) {
                        binding.actionButtonContainer.saveButton.apply {
                            isActivated = true
                            isEnabled = false
                        }

                        val action = KeyboardFragmentDirections.actionKeyboardFragmentToAddToCategoryPickerFragment(text.toString())
                        if (findNavController().currentDestination?.id == R.id.keyboardFragment) {
                            findNavController().navigate(action)
                        }
                    }
                }
            }
        }

        binding.keyboardClearButton.action = {
            binding.keyboardInput.setText(R.string.keyboard_select_letters)
            viewModel.currentText = ""
        }

        binding.keyboardSpaceButton.action = {
            if (!isDefaultTextVisible() && binding.keyboardInput.text?.endsWith(' ') == false) {
                binding.keyboardInput.append(" ")
                viewModel.currentText = binding.keyboardInput.text.toString()
            }
        }

        binding.keyboardBackspaceButton.action = {
            if (!isDefaultTextVisible()) {
                binding.keyboardInput.apply {
                    setText(text.toString().dropLast(1))
                    setSelection(text.length)
                    if (text.isNullOrEmpty()) {
                        setText(R.string.keyboard_select_letters)
                    }
                    viewModel.currentText = this.text.toString()
                }
            }
        }

        binding.keyboardSpeakButton.action = {
            if (!isDefaultTextVisible()) {
                VocableTextToSpeech.speak(
                    Locale.getDefault(),
                    binding.keyboardInput.text?.toString() ?: ""
                )
            }
        }

        (binding.phraseSavedView.root as TextView).setText(R.string.saved_successfully)

        viewModel = ViewModelProviders.of(
            this,
            BaseViewModelFactory()
        ).get(KeyboardViewModel::class.java)
        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
            binding.phraseSavedView.root.isVisible = it
        })

        viewModel.isPhraseSaved.observe(viewLifecycleOwner, Observer {
            binding.actionButtonContainer.saveButton.apply {
                isActivated = it
                isEnabled = !it
            }
        })
    }

    private val allViews = mutableListOf<View>()

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.keyboardParent)
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
}
