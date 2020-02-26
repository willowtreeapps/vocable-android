package com.willowtree.vocable.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.facetracking.FaceTrackFragment
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.vocable_confirmation_dialog.*

class SettingsActivity : BaseActivity() {

    companion object {
        private const val PRIVACY_POLICY = "https://vocable.app/privacy.html"
        private const val MAIL_TO = "mailto:vocable@willowtreeapps.com"
    }

    private val allViews = mutableListOf<View>()
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)

        super.onCreate(savedInstanceState)

        privacy_policy_button.action = {
            showLeavingAppDialog {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)))
            }
        }

        contact_devs_button.action = {
            showLeavingAppDialog {
                val sendEmail = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(MAIL_TO)
                }
                startActivity(sendEmail)
            }
        }

        with(settings_close_button) {
            setIconWithNoText(R.drawable.ic_close)
            action = {
                finish()
            }
        }

        head_tracking_container.action = {
            head_tracking_switch.isChecked = !head_tracking_switch.isChecked
        }

        head_tracking_switch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onHeadTrackingChecked(isChecked)
        }
    }

    private fun showLeavingAppDialog(positiveAction: (() -> Unit)) {
        dialog_title.setText(R.string.settings_dialog_title)
        dialog_message.setText(R.string.settings_dialog_message)
        with(dialog_positive_button) {
            setText(R.string.settings_dialog_continue)
            action = {
                positiveAction.invoke()
                toggleDialogVisibility(false)
            }
        }
        with(dialog_negative_button) {
            setText(R.string.settings_dialog_cancel)
            action = {
                toggleDialogVisibility(false)
            }
        }
        toggleDialogVisibility(true)
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        with(settings_confirmation) {
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
                head_tracking_switch.isChecked = it
                pointer_view.isVisible = it
                val faceFragment = supportFragmentManager.findFragmentById(R.id.face_fragment)
                if (faceFragment is FaceTrackFragment) {
                    faceFragment.enableFaceTracking(it)
                }
            }
        })
    }

    override fun getErrorView(): View = error_view

    override fun getPointerView(): PointerView = pointer_view

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            if (settings_confirmation.isVisible) {
                getAllChildViews(settings_confirmation as ViewGroup)
            } else {
                getAllChildViews(parent_layout)
            }
        }
        return allViews
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