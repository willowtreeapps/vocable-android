package com.willowtree.vocable.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseActivity
import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.databinding.ActivitySettingsBinding
import com.willowtree.vocable.facetracking.FaceTrackFragment

class SettingsActivity : BaseActivity() {

    companion object {
        private const val PRIVACY_POLICY = "https://vocable.app/privacy.html"
        private const val MAIL_TO = "mailto:vocable@willowtreeapps.com"
    }

    private lateinit var binding: ActivitySettingsBinding
    private val allViews = mutableListOf<View>()
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)

        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.version.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        binding.privacyPolicyButton.action = {
            showLeavingAppDialog {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)))
            }
        }

        binding.contactDevsButton.action = {
            showLeavingAppDialog {
                val sendEmail = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(MAIL_TO)
                }
                startActivity(sendEmail)
            }
        }

        with(binding.settingsCloseButton) {
            action = {
                finish()
            }
        }

        binding.headTrackingContainer.action = {
            binding.headTrackingSwitch.isChecked = !binding.headTrackingSwitch.isChecked
        }

        binding.headTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onHeadTrackingChecked(isChecked)
        }
    }

    private fun showLeavingAppDialog(positiveAction: (() -> Unit)) {
        binding.settingsConfirmation.dialogTitle.setText(R.string.settings_dialog_title)
        binding.settingsConfirmation.dialogMessage.setText(R.string.settings_dialog_message)
        with(binding.settingsConfirmation.dialogPositiveButton) {
            setText(R.string.settings_dialog_continue)
            action = {
                positiveAction.invoke()
                toggleDialogVisibility(false)
            }
        }
        with(binding.settingsConfirmation.dialogNegativeButton) {
            setText(R.string.settings_dialog_cancel)
            action = {
                toggleDialogVisibility(false)
            }
        }
        toggleDialogVisibility(true)
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        with(binding.settingsConfirmation.root) {
            isVisible = visible
            post {
                allViews.clear()
            }
        }
    }

    override fun subscribeToViewModel() {
        super.subscribeToViewModel()
        viewModel.headTrackingEnabled.observe(this, Observer {
            it?.let {
                binding.headTrackingSwitch.isChecked = it
                binding.pointerView.isVisible = it
                val faceFragment = supportFragmentManager.findFragmentById(R.id.face_fragment)
                if (faceFragment is FaceTrackFragment) {
                    faceFragment.enableFaceTracking(it)
                }
            }
        })
    }

    override fun getErrorView(): View = binding.errorView.root

    override fun getPointerView(): PointerView = binding.pointerView

    override fun getAllViews(): List<View> {
        return when {
            allViews.isNotEmpty() -> allViews
            binding.settingsConfirmation.root.isVisible -> {
                getAllChildViews(binding.settingsConfirmation.root as ViewGroup)
                allViews
            }
            else -> {
                getAllChildViews(binding.parentLayout)
                allViews
            }
        }
    }

    override fun getLayout(): Int =
        R.layout.activity_settings

    private fun getAllChildViews(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }
}