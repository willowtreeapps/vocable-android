package com.willowtree.vocable

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.willowtree.vocable.customviews.PauseButton
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.presets.PresetsFragment
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity() {

    private val allViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VocableTextToSpeech.initialize(this)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, PresetsFragment())
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        VocableTextToSpeech.shutdown()
    }

    override fun getErrorView(): View = error_view

    override fun getPointerView(): PointerView = pointer_view

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(parent_layout)
            getAllFragmentViews()
        }
        return allViews
    }

    override fun getLayout(): Int = R.layout.activity_main

    override fun getPauseButton(): PauseButton? = pause_button

    private fun getAllChildViews(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }

    private fun getAllFragmentViews() {
        supportFragmentManager.fragments.forEach {
            if (it is BaseFragment) {
                allViews.addAll(it.getAllViews())
            }
        }
    }
}