package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentSelectionModeBinding
import org.koin.android.ext.android.inject

class SelectionModeFragment : BaseFragment<FragmentSelectionModeBinding>() {

    override val bindingInflater: BindingInflater<FragmentSelectionModeBinding> = FragmentSelectionModeBinding::inflate
    private var allViews = mutableListOf<View>()
    private val viewModel: SettingsViewModel by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.selectionModeBackButton.action = {
            findNavController().popBackStack()
        }

        binding.selectionModeOptions.apply {
            headTrackingContainer.action = {
                headTrackingSwitch.isChecked = !headTrackingSwitch.isChecked
            }
        }

        binding.selectionModeOptions.headTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onHeadTrackingChecked(isChecked)
        }

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
