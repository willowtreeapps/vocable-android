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
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.android.synthetic.main.fragment_presets.*
import kotlin.math.ceil
import kotlin.math.min

class PresetsFragment : BaseFragment() {

    companion object {
        private const val MAX_CATEGORIES = 4
        private const val MAX_PHRASES = 9
    }

    private val allViews = mutableListOf<View>()

    private lateinit var presetsViewModel: PresetsViewModel

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
                    with(category_view) {
                        adapter = CategoriesPagerAdapter(fragmentManager, categories)
                        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                activity?.let { activity ->
                                    allViews.clear()
                                    if (activity is MainActivity) {
                                        activity.resetAllViews()
                                    }
                                }
                            }
                        })
                    }
                }
            }
        })

        presetsViewModel.currentPhrases.observe(viewLifecycleOwner, Observer {
            it?.let { phrases ->
                fragmentManager?.let { fragmentManager ->
                    phrases_view.adapter = PhrasesPagerAdapter(fragmentManager, phrases)
                    with(phrases_view) {
                        adapter = PhrasesPagerAdapter(fragmentManager, phrases)
                        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                activity?.let { activity ->
                                    allViews.clear()
                                    if (activity is MainActivity) {
                                        activity.resetAllViews()
                                    }
                                }
                            }
                        })
                    }
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
            return ceil(categories.size / MAX_CATEGORIES.toDouble()).toInt()
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = position * MAX_CATEGORIES
            val subList = categories.subList(
                startPosition,
                min(categories.size, startPosition + MAX_CATEGORIES)
            )

            return CategoriesFragment.newInstance(subList)
        }
    }

    inner class PhrasesPagerAdapter(fm: FragmentManager, private val phrases: List<String>) :
        FragmentStateAdapter(fm, viewLifecycleOwner.lifecycle) {

        override fun getItemCount(): Int {
            return ceil(phrases.size / MAX_PHRASES.toDouble()).toInt()
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = position * MAX_PHRASES
            val sublist =
                phrases.subList(startPosition, min(phrases.size, startPosition + MAX_PHRASES))

            return PhrasesFragment.newInstance(sublist)
        }

    }
}