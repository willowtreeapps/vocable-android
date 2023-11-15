package com.willowtree.vocable.settings.selectionmode

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.databinding.FragmentSelectionModeBinding
import com.willowtree.vocable.settings.selectionmode.HeadTrackingPermissionState
import com.willowtree.vocable.settings.selectionmode.SelectionModeViewModel
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectionModeFragment : BaseFragment<FragmentSelectionModeBinding>() {

    override val bindingInflater: BindingInflater<FragmentSelectionModeBinding> = FragmentSelectionModeBinding::inflate
    private val viewModel: SelectionModeViewModel by viewModel(owner = {
        ViewModelOwner.from(requireActivity())
    })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.selectionModeBackButton.action = {
            findNavController().popBackStack()
        }

        viewModel.headTrackingPermissionState.observe(viewLifecycleOwner) {
            binding.selectionModeOptions.headTrackingSwitch.isChecked = it == HeadTrackingPermissionState.Enabled
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
