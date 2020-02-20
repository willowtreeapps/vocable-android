package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.android.synthetic.main.fragment_presets.*

class PresetsFragment : BaseFragment() {

    private val allViews = mutableListOf<View>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        SpokenText.observe(viewLifecycleOwner, Observer {
            current_text.text = if (it.isNullOrBlank()) {
                getString(R.string.select_something)
            } else {
                it
            }
        })
        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            speaker_icon.isVisible = it ?: false
        })
    }

    override fun getLayout(): Int = R.layout.fragment_presets

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(presets_parent)
        }
        return allViews
    }

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