package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentSelectionModeBinding

class SelectionModeFragment : BaseFragment<FragmentSelectionModeBinding>() {

    override val bindingInflater: BindingInflater<FragmentSelectionModeBinding> = FragmentSelectionModeBinding::inflate
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
            BaseViewModelFactory()
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
