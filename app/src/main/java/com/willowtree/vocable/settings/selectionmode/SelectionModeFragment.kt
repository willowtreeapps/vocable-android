package com.willowtree.vocable.settings.selectionmode

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
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

        binding.selectionModeOptions.selectionModeSwitch.text = (getString(R.string.settings_head_tracking))

        viewModel.headTrackingEnabled.observe(viewLifecycleOwner) {
            binding.selectionModeOptions.selectionModeSwitch.isChecked = it
        }

        binding.selectionModeOptions.selectionModeSwitch.action = {
            if (!binding.selectionModeOptions.selectionModeSwitch.isChecked) {
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
