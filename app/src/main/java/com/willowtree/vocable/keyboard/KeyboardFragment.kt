package com.willowtree.vocable.keyboard

import android.content.Context
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
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentKeyboardBinding
import com.willowtree.vocable.databinding.KeyboardKeyLayoutBinding
import com.willowtree.vocable.presets.PresetsFragment
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.settings.SettingsActivity
import com.willowtree.vocable.utils.VocableTextToSpeech
import org.koin.android.ext.android.get
import java.util.*

class KeyboardFragment : BaseFragment<FragmentKeyboardBinding>() {

    override val bindingInflater: BindingInflater<FragmentKeyboardBinding> = FragmentKeyboardBinding::inflate
    private lateinit var viewModel: KeyboardViewModel
    private lateinit var keys: Array<String>
    private var mySayingsPhrases: List<Phrase>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        keys =  resources.getStringArray(R.array.keyboard_keys)
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
                    var currentText = binding.keyboardInput.text?.toString() ?: ""
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

                    viewModel.currentText = binding.keyboardInput.text.toString()
                }
            }
        }
    }

    private fun isDefaultTextVisible(): Boolean {
        return binding.keyboardInput.text.toString() == getString(R.string.keyboard_select_letters)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            binding.speakerIcon.isVisible = it
        })

        binding.actionButtonContainer.presetsButton.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, PresetsFragment())
                .commit()
        }

        binding.actionButtonContainer.settingsButton.action = {
            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.actionButtonContainer.saveButton.action = {
            if (!isDefaultTextVisible()) {
                binding.keyboardInput.text?.let { text ->
                    if (text.isNotBlank()) {
                        viewModel.addNewPhrase(text.toString())
                        binding.actionButtonContainer.saveButton.apply {
                            isActivated = true
                            isEnabled = false
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
            BaseViewModelFactory(
                getString(R.string.category_123_id),
                getString(R.string.category_my_sayings_id)
            )
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