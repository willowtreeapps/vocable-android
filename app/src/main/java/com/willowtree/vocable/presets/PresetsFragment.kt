package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.android.synthetic.main.fragment_presets.*
import kotlin.math.ceil
import kotlin.math.min

class PresetsFragment : BaseFragment() {

    private val allViews = mutableListOf<View>()

    private lateinit var presetsViewModel: PresetsViewModel
    private lateinit var pagerAdapter: CategoriesPagerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presetsViewModel =
            ViewModelProviders.of(requireActivity()).get(PresetsViewModel::class.java)
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
        presetsViewModel.categoryList.observe(viewLifecycleOwner, Observer {
            it?.let { categories ->
                fragmentManager?.let { fragmentManager ->
                    pagerAdapter = CategoriesPagerAdapter(fragmentManager, categories)
                    category_view.adapter = pagerAdapter
                }
            }
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

    inner class CategoriesPagerAdapter(fm: FragmentManager, private val categories: List<String>) :
        FragmentStateAdapter(fm, viewLifecycleOwner.lifecycle) {

        override fun getItemCount(): Int {
            return ceil(categories.size / 4.0).toInt()
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = position * 4
            val subList = categories.subList(startPosition, min(categories.size, startPosition + 4))

            return CategoriesFragment.newInstance(subList)
        }
    }
}