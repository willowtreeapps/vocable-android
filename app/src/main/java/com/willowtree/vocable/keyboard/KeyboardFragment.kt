package com.willowtree.vocable.keyboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentKeyboardBinding
import com.willowtree.vocable.databinding.KeyboardKeyLayoutBinding
import com.willowtree.vocable.presets.PresetsFragment
import com.willowtree.vocable.settings.SettingsActivity
import com.willowtree.vocable.utils.VocableTextToSpeech
import java.util.*


class KeyboardFragment : BaseFragment() {

    private lateinit var viewModel: KeyboardViewModel
    private var binding: FragmentKeyboardBinding? = null
    private lateinit var keys: Array<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentKeyboardBinding.inflate(inflater, container, false)
        keys = resources.getStringArray(R.array.keyboard_keys)
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

        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            binding?.speakerIcon?.isVisible = it ?: false
        })

        binding?.actionButtonContainer?.presetsButton?.let {
            it.action = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, PresetsFragment())
                    .commit()
            }
        }

        binding?.actionButtonContainer?.settingsButton?.let {
            it.action = {
                val intent = Intent(activity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        binding?.actionButtonContainer?.saveButton?.let {
            it.action = {
                if (!isDefaultTextVisible()) {
                    binding?.keyboardInput?.text?.let { text ->
                        if (text.isNotBlank()) {
                            viewModel.addNewPhrase(text.toString())
                        }
                    }
                }
            }
        }

        binding?.keyboardClearButton?.let {
            it.action = {
                binding?.keyboardInput?.setText(R.string.keyboard_select_letters)
            }
        }

        binding?.keyboardSpaceButton?.let {
            it.action = {
                if (!isDefaultTextVisible() && binding?.keyboardInput?.text?.endsWith(' ') == false) {
                    binding?.keyboardInput?.append(" ")
                }
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

        binding?.keyboardSpeakButton?.let {
            it.action = {
                if (!isDefaultTextVisible()) {
                    VocableTextToSpeech.speak(
                        Locale.getDefault(),
                        binding?.keyboardInput?.text?.toString() ?: ""
                    )
                }
            }
        }

        binding?.phraseSavedView?.root?.let {
            buildTextWithIcon(
                getString(R.string.saved_successfully),
                getString(R.string.category_saved_to),
                iconCharStart = 9,
                iconCharEnd = 10,
                view = it as TextView,
                icon = R.drawable.ic_heart_solid_blue
            )
        }

        viewModel = ViewModelProviders.of(this).get(KeyboardViewModel::class.java)
        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.showPhraseAdded.observe(viewLifecycleOwner, Observer {
            binding?.phraseSavedView?.root?.isVisible = it ?: false
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private val allViews = mutableListOf<View>()

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding?.keyboardParent)
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