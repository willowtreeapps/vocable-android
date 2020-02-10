package com.example.eyespeak

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity() {

    companion object {
        private const val PRIVACY_POLICY = "https://vocable.app/privacy.html"
        private const val MAIL_TO = "mailto:vocable@willowtreeapps.com"
    }

    private val allViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        privacy_policy_button.action = {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)))
        }

        contact_devs_button.action = {
            val sendEmail = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse(MAIL_TO)
            }
            startActivity(sendEmail)
        }
    }

    override fun getPointerView(): PointerView = pointer_view

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(parent_layout)
        }
        return allViews
    }

    override fun getLayout(): Int = R.layout.activity_settings

    override fun getPauseButton(): PauseButton? = settings_pause_button

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