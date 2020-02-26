package com.willowtree.vocable.presets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import com.willowtree.vocable.databinding.FragmentPresetsBinding
import com.willowtree.vocable.keyboard.KeyboardFragment
import com.willowtree.vocable.settings.SettingsActivity
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlin.math.ceil
import kotlin.math.min

class PresetsFragment : BaseFragment() {

    companion object {
        const val MAX_CATEGORIES = 4
        const val MAX_PHRASES = 9
    }

    private var binding: FragmentPresetsBinding? = null
    private val allViews = mutableListOf<View>()

    private lateinit var presetsViewModel: PresetsViewModel
    private lateinit var categoriesAdapter: CategoriesPagerAdapter
    private lateinit var phrasesAdapter: PhrasesPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPresetsBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.categoryForwardButton?.action = {
            when (val currentPosition = binding?.categoryView?.currentItem) {
                null -> {
                    // No-op
                }
                categoriesAdapter.itemCount - 1 -> {
                    binding?.categoryView?.setCurrentItem(0, true)
                }
                else -> {
                    binding?.categoryView?.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding?.categoryBackButton?.action = {
            when (val currentPosition = binding?.categoryView?.currentItem) {
                null -> {
                    // No-op
                }
                0 -> {
                    binding?.categoryView?.setCurrentItem(categoriesAdapter.itemCount - 1, true)
                }
                else -> {
                    binding?.categoryView?.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding?.phrasesForwardButton?.action = {
            when (val currentPosition = binding?.phrasesView?.currentItem) {
                null -> {
                    // No-op
                }
                phrasesAdapter.itemCount - 1 -> {
                    binding?.phrasesView?.setCurrentItem(0, true)
                }
                else -> {
                    binding?.phrasesView?.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding?.phrasesBackButton?.action = {
            when (val currentPosition = binding?.phrasesView?.currentItem) {
                null -> {
                    // No-op
                }
                0 -> {
                    binding?.phrasesView?.setCurrentItem(phrasesAdapter.itemCount - 1, true)
                }
                else -> {
                    binding?.phrasesView?.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        with(binding?.actionButtonContainer?.keyboardButton) {
            this?.setIconWithNoText(R.drawable.ic_keyboard)
            this?.action = {
                fragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.fragment_container, KeyboardFragment())
                    ?.commit()
            }
        }

        with(binding?.actionButtonContainer?.settingsButton) {
            this?.setIconWithNoText(R.drawable.ic_settings_light_48dp)
            this?.action = {
                val intent = Intent(activity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        fragmentManager?.let { fragmentManager ->
            categoriesAdapter = CategoriesPagerAdapter(fragmentManager)
            phrasesAdapter = PhrasesPagerAdapter(fragmentManager)
        }

        binding?.categoryView?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activity?.let { activity ->
                    allViews.clear()
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        binding?.phrasesView?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageNum = position % phrasesAdapter.numPages + 1
                binding?.phrasesPageNumber?.text = getString(
                    R.string.phrases_page_number,
                    pageNum,
                    phrasesAdapter.numPages
                )
                activity?.let { activity ->
                    allViews.clear()
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        SpokenText.postValue(null)

        presetsViewModel =
            ViewModelProviders.of(requireActivity()).get(PresetsViewModel::class.java)
        subscribeToViewModel()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun subscribeToViewModel() {
        SpokenText.observe(viewLifecycleOwner, Observer {
            binding?.currentText?.text = if (it.isNullOrBlank()) {
                getString(R.string.select_something)
            } else {
                it
            }
        })

        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            binding?.speakerIcon?.isVisible = it ?: false
        })

        presetsViewModel.categoryList.observe(viewLifecycleOwner, Observer {
            it?.let { categories ->
                with(binding?.categoryView) {
                    this?.adapter = categoriesAdapter
                    categoriesAdapter.setCategories(categories)
                    // Move adapter to middle so user can scroll both directions
                    val middle = categoriesAdapter.itemCount / 2
                    if (middle % categoriesAdapter.numPages == 0) {
                        binding?.categoryView?.setCurrentItem(middle, false)
                    } else {
                        val mod = middle % categoriesAdapter.numPages
                        binding?.categoryView?.setCurrentItem(
                            middle + (categoriesAdapter.numPages - mod),
                            false
                        )
                    }
                }
            }
        })

        presetsViewModel.currentPhrases.observe(viewLifecycleOwner, Observer {
            it?.let { phrases ->
                with(binding?.phrasesView) {
                    this?.adapter = phrasesAdapter
                    phrasesAdapter.setPhrases(phrases)
                    // Move adapter to middle so user can scroll both directions
                    val middle = phrasesAdapter.itemCount / 2
                    if (middle % phrasesAdapter.numPages == 0) {
                        binding?.phrasesView?.setCurrentItem(middle, false)
                    } else {
                        val mod = middle % phrasesAdapter.numPages
                        binding?.phrasesView?.setCurrentItem(
                            middle + (phrasesAdapter.numPages - mod),
                            false
                        )
                    }
                }
            }
        })
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding?.presetsParent)
        }
        return allViews
    }

    private fun getAllChildViews(viewGroup: ViewGroup?) {
        viewGroup?.children?.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }

    inner class CategoriesPagerAdapter(fm: FragmentManager) :
        FragmentStateAdapter(fm, viewLifecycleOwner.lifecycle) {

        private val categories = mutableListOf<String>()

        var numPages = 0

        fun setCategories(categories: List<String>) {
            with(this.categories) {
                clear()
                addAll(categories)
            }
            numPages = ceil(categories.size / MAX_CATEGORIES.toDouble()).toInt()
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return Int.MAX_VALUE
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = (position % numPages) * MAX_CATEGORIES
            val subList = categories.subList(
                startPosition,
                min(categories.size, startPosition + MAX_CATEGORIES)
            )

            return CategoriesFragment.newInstance(subList)
        }
    }

    inner class PhrasesPagerAdapter(fm: FragmentManager) :
        FragmentStateAdapter(fm, viewLifecycleOwner.lifecycle) {

        private val phrases = mutableListOf<String>()
        var numPages: Int = 0

        fun setPhrases(phrases: List<String>) {
            with(this.phrases) {
                clear()
                addAll(phrases)
            }
            numPages = ceil(phrases.size / MAX_PHRASES.toDouble()).toInt()
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return Int.MAX_VALUE
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = (position % numPages) * MAX_PHRASES
            val sublist =
                phrases.subList(startPosition, min(phrases.size, startPosition + MAX_PHRASES))

            return PhrasesFragment.newInstance(sublist)
        }

    }
}