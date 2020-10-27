package com.willowtree.vocable.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.*
import com.willowtree.vocable.databinding.FragmentSettingsBinding
import org.koin.android.ext.android.inject

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    companion object {
        private const val PRIVACY_POLICY = "https://vocable.app/privacy.html"
        private const val MAIL_TO =
            "mailto:vocable@willowtreeapps.com?subject=Feedback for Android Vocable "
        private const val SETTINGS_OPTION_COUNT = 5
    }

    override val bindingInflater: BindingInflater<FragmentSettingsBinding> = FragmentSettingsBinding::inflate
    private val viewModel: SettingsViewModel by inject()
    private var numColumns = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        numColumns = resources.getInteger(R.integer.settings_options_columns)

        (binding.settingsOptionsContainer.root as GridLayout).children.forEachIndexed { index, child ->
            if (index % numColumns == numColumns - 1) {
                child.layoutParams = (child.layoutParams as GridLayout.LayoutParams).apply {
                    marginEnd = 0
                }
            }
            if (index > SETTINGS_OPTION_COUNT - numColumns) {
                child.layoutParams = (child.layoutParams as GridLayout.LayoutParams).apply {
                    updateMargins(bottom = 0)
                }
            }
        }

        return binding.root
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.version.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        binding.privacyPolicyButton.action = {
            showLeavingAppDialog {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)))
            }
        }

        binding.contactDevsButton.action = {
            showLeavingAppDialog {
                val sendEmail = Intent(Intent.ACTION_SENDTO).apply {
                    data =
                        Uri.parse("$MAIL_TO${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}")
                }
                startActivity(sendEmail)
            }
        }

        binding.settingsCloseButton.action = {
            findNavController().popBackStack()
        }

        binding.settingsOptionsContainer.timingSensitivityButton.action = {
            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                findNavController().navigate(R.id.action_settingsFragment_to_sensitivityFragment)
            }
        }

        binding.settingsOptionsContainer.selectionModeButton.action = {
            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                findNavController().navigate(R.id.action_settingsFragment_to_selectionModeFragment)
            }
        }

        binding.settingsOptionsContainer.editCategoriesButton.action = {
            if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                findNavController().navigate(R.id.action_settingsFragment_to_editCategoriesFragment)
            }
        }
    }


    private fun showLeavingAppDialog(positiveAction: (() -> Unit)) {
        setSettingsButtonsEnabled(false)
        binding.settingsConfirmation.apply {
            dialogTitle.setText(R.string.settings_dialog_title)
            dialogMessage.setText(R.string.settings_dialog_message)
            dialogPositiveButton.apply {
                setText(R.string.settings_dialog_continue)
                action = {
                    positiveAction.invoke()
                    toggleDialogVisibility(false)

                    setSettingsButtonsEnabled(true)
                }
            }
            dialogNegativeButton.apply {
                setText(R.string.settings_dialog_cancel)
                action = {
                    toggleDialogVisibility(false)
                    setSettingsButtonsEnabled(true)
                }
            }
        }
        toggleDialogVisibility(true)
    }

    private fun setSettingsButtonsEnabled(enable: Boolean) {
        binding.apply {
            settingsCloseButton.isEnabled = enable
            privacyPolicyButton.isEnabled = enable
            contactDevsButton.isEnabled = enable
            settingsOptionsContainer.editCategoriesButton.isEnabled = enable
            settingsOptionsContainer.resetAppButton.isEnabled = enable
            settingsOptionsContainer.selectionModeButton.isEnabled = enable
            settingsOptionsContainer.timingSensitivityButton.isEnabled = enable
        }
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.settingsConfirmation.root.isVisible = visible
    }
}