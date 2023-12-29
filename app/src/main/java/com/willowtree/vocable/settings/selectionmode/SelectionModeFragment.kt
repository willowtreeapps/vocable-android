package com.willowtree.vocable.settings.selectionmode

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.databinding.FragmentSelectionModeBinding
import org.koin.androidx.scope.scopeActivity
import org.koin.androidx.viewmodel.ext.android.getViewModel

class SelectionModeFragment : BaseFragment<FragmentSelectionModeBinding>() {

    override val bindingInflater: BindingInflater<FragmentSelectionModeBinding> = FragmentSelectionModeBinding::inflate
    private lateinit var viewModel: SelectionModeViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = requireNotNull(scopeActivity?.getViewModel()) {
            "SelectionModeFragment requires parent to be a ScopeActivity"
        }

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
