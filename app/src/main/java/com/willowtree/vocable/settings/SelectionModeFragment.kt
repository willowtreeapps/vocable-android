package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewbinding.ViewBinding
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentSelectionModeBinding

class SelectionModeFragment : BaseFragment<FragmentSelectionModeBinding>() {

    override val bindingInflater: (LayoutInflater) -> ViewBinding = FragmentSelectionModeBinding::inflate
    private var allViews = mutableListOf<View>()
    private lateinit var viewModel: SettingsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.selectionModeBackButton.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }

        binding.selectionModeOptions.apply {
            headTrackingContainer.action = {
                headTrackingSwitch.isChecked = !headTrackingSwitch.isChecked
            }
        }

        binding.selectionModeOptions.headTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onHeadTrackingChecked(isChecked)
        }

        viewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory(
                getString(R.string.category_123_id),
                getString(R.string.category_my_sayings_id)
            )
        ).get(SettingsViewModel::class.java)
        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        viewModel.headTrackingEnabled.observe(viewLifecycleOwner, Observer {
            binding.selectionModeOptions.headTrackingSwitch.isChecked = it
        })
    }

    override fun getAllViews(): List<View> {
        return listOf()
    }
}
