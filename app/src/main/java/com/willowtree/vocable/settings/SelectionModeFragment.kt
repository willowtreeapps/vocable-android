package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentEditPresetsBinding
import com.willowtree.vocable.room.Phrase
import java.lang.Math.ceil

class SelectionModeFragment : BaseFragment() {

    //private var binding: FragmentSelectionModeBinding? = null
    private var allViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //binding = FragmentEditPresetsBinding.inflate(inflater, container, false)
        return null //binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding?.backButton?.action = {
//            parentFragmentManager
//                .beginTransaction()
//                .replace(R.id.settings_fragment_container, SettingsFragment())
//                .commit()
//        }

    }

    private fun subscribeToViewModel() {

    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            //getAllChildViews(binding?.presetsParent)
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

    override fun onDestroyView() {
        //binding = null
        super.onDestroyView()
    }
}
