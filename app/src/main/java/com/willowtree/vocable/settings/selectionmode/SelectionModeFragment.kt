package com.willowtree.vocable.settings.selectionmode

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.databinding.FragmentSelectionModeBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectionModeFragment : BaseFragment<FragmentSelectionModeBinding>() {

    override val bindingInflater: BindingInflater<FragmentSelectionModeBinding> = FragmentSelectionModeBinding::inflate
    private val viewModel: SelectionModeViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.selectionModeBackButton.action = {
            findNavController().popBackStack()
        }

        viewModel.headTrackingEnabled.observe(viewLifecycleOwner) {
            binding.selectionModeOptions.headTrackingSwitch.isChecked = it
        }

        binding.selectionModeOptions.headTrackingContainer.setOnClickListener {
            if (!binding.selectionModeOptions.headTrackingSwitch.isChecked) {
                viewModel.requestHeadTracking()
            } else {
                viewModel.disableHeadTracking()
            }
        }
    }

    override fun getAllViews(): List<View> {
        return listOf()
    }
}
