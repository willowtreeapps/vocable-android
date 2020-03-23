package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentSelectionModeBinding

class SelectionModeFragment : BaseFragment() {

    private var binding: FragmentSelectionModeBinding? = null
    private var allViews = mutableListOf<View>()
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectionModeBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.selectionModeBackButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }

        binding?.selectionModeOptions?.let {
            it.headTrackingContainer.action = {
                it.headTrackingSwitch.isChecked = !it.headTrackingSwitch.isChecked
            }
        }

        binding?.selectionModeOptions?.headTrackingSwitch?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onHeadTrackingChecked(isChecked)
        }

        viewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel::class.java)
        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.headTrackingEnabled.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding?.selectionModeOptions?.headTrackingSwitch?.isChecked = it
            }
        })
    }

    override fun getAllViews(): List<View> {
        return listOf()
    }


    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
