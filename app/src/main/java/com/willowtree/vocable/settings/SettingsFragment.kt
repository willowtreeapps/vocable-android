package com.willowtree.vocable.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentSettingsBinding

class SettingsFragment : BaseFragment() {

    companion object {
        private const val PRIVACY_POLICY = "https://vocable.app/privacy.html"
        private const val MAIL_TO =
            "mailto:vocable@willowtreeapps.com?subject=Feedback for Android Vocable "
    }

    private lateinit var viewModel: SettingsViewModel
    private var binding: FragmentSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
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
                    data =
                        Uri.parse("$MAIL_TO${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}")
                }
                startActivity(sendEmail)
            }
        }

        binding?.settingsCloseButton?.let {
            it.action = {
                requireActivity().finish()
            }
        }

        binding?.headTrackingContainer?.action = {
            binding?.headTrackingSwitch?.let {
                it.isChecked = !it.isChecked
            }
        }

        binding?.headTrackingSwitch?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onHeadTrackingChecked(isChecked)
        }

        binding?.editSayingsButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, EditPresetsFragment())
                .addToBackStack(null)
                .commit()
        }

        viewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel::class.java)
        subscribeToViewModel()
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
            it.headTrackingContainer.isEnabled = enable
            it.privacyPolicyButton.isEnabled = enable
            it.contactDevsButton.isEnabled = enable
        }
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding?.settingsConfirmation?.root?.let {
            it.isVisible = visible
            it.post {
                //allViews.clear()
            }
        }
    }

    private fun subscribeToViewModel() {
        viewModel.headTrackingEnabled.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding?.headTrackingSwitch?.isChecked = it
            }
        })

        viewModel.mySayingsIsEmpty.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding?.editSayingsButton?.isEnabled = !it
            }
        })
    }
}