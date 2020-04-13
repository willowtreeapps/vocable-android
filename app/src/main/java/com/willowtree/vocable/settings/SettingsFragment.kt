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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentSettingsBinding

class SettingsFragment : BaseFragment() {

    companion object {
        private const val PRIVACY_POLICY = "https://vocable.app/privacy.html"
        private const val MAIL_TO = "mailto:vocable@willowtreeapps.com"
        private const val SETTINGS_OPTION_COUNT = 5
    }

    private lateinit var viewModel: SettingsViewModel
    private var binding: FragmentSettingsBinding? = null
    private var numColumns = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        numColumns = resources.getInteger(R.integer.settings_options_columns)

        (binding?.settingsOptionsContainer?.root as? GridLayout)?.children?.forEachIndexed { index, child ->
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

        return binding?.root
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.version?.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        binding?.privacyPolicyButton?.action = {
            showLeavingAppDialog {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)))
            }
        }

        binding?.contactDevsButton?.action = {
            showLeavingAppDialog {
                val sendEmail = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(MAIL_TO)
                }
                startActivity(sendEmail)
            }
        }

        binding?.settingsCloseButton?.let {
            it.action = {
                requireActivity().finish()
            }
        }

        binding?.settingsOptionsContainer?.editSayingsButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, EditPresetsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding?.settingsOptionsContainer?.timingSensitivityButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SensitivityFragment())
                .addToBackStack(null)
                .commit()
        }

        binding?.settingsOptionsContainer?.selectionModeButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SelectionModeFragment())
                .addToBackStack(null)
                .commit()
        }

        binding?.settingsOptionsContainer?.editCategoriesButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, EditCategoriesFragment())
                .addToBackStack(null)
                .commit()
        }

        viewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory(
                getString(R.string.category_123_id),
                getString(R.string.category_my_sayings_id)
            )
        ).get(SettingsViewModel::class.java)
    }


    private fun showLeavingAppDialog(positiveAction: (() -> Unit)) {
        setSettingsButtonsEnabled(false)
        binding?.settingsConfirmation?.dialogTitle?.setText(R.string.settings_dialog_title)
        binding?.settingsConfirmation?.dialogMessage?.setText(R.string.settings_dialog_message)
        binding?.settingsConfirmation?.dialogPositiveButton?.let {
            it.setText(R.string.settings_dialog_continue)
            it.action = {
                positiveAction.invoke()
                toggleDialogVisibility(false)

                setSettingsButtonsEnabled(true)
            }
        }
        binding?.settingsConfirmation?.dialogNegativeButton?.let {
            it.setText(R.string.settings_dialog_cancel)
            it.action = {
                toggleDialogVisibility(false)
                setSettingsButtonsEnabled(true)
            }
        }
        toggleDialogVisibility(true)
    }

    private fun setSettingsButtonsEnabled(enable: Boolean) {
        binding?.let {
            it.settingsCloseButton.isEnabled = enable
            it.privacyPolicyButton.isEnabled = enable
            it.contactDevsButton.isEnabled = enable
            it.settingsOptionsContainer.editCategoriesButton.isEnabled = enable
            it.settingsOptionsContainer.editSayingsButton.isEnabled = enable
            it.settingsOptionsContainer.resetAppButton.isEnabled = enable
            it.settingsOptionsContainer.selectionModeButton.isEnabled = enable
            it.settingsOptionsContainer.timingSensitivityButton.isEnabled = enable
        }
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding?.settingsConfirmation?.root?.let {
            it.isVisible = visible
        }
    }
}