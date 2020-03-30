package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentTimingSensitivityBinding
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.android.ext.android.inject

class SensitivityFragment : BaseFragment() {

    companion object {
        private const val LOW_SENSITIVITY = 0.05F
        private const val MEDIUM_SENSITIVITY = 0.1F
        private const val HIGH_SENSITIVITY = 0.15F
    }

    private var binding: FragmentTimingSensitivityBinding? = null

    private val sharedPrefs: VocableSharedPreferences by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTimingSensitivityBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (sharedPrefs.getSensitivity()) {
            LOW_SENSITIVITY -> {
                toggleSensitivityButtons(lowActivated = true)
            }

            MEDIUM_SENSITIVITY -> {
                toggleSensitivityButtons(mediumActivated = true)
            }

            HIGH_SENSITIVITY -> {
                toggleSensitivityButtons(highActivated = true)
            }
        }

        binding?.timingSensitivityBackButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }

        binding?.decreaseHoverTime?.action = {

        }

        binding?.increaseHoverTime?.action = {

        }

        binding?.lowSensitivityButton?.action = {
            sharedPrefs.setSensitivity(LOW_SENSITIVITY)
            toggleSensitivityButtons(lowActivated = true)
        }

        binding?.mediumSensitivityButton?.action = {
            sharedPrefs.setSensitivity(MEDIUM_SENSITIVITY)
            toggleSensitivityButtons(mediumActivated = true)
        }

        binding?.highSensitivityButton?.action = {
            sharedPrefs.setSensitivity(HIGH_SENSITIVITY)
            toggleSensitivityButtons(highActivated = true)
        }

    }

    override fun getAllViews(): List<View> = emptyList()

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun toggleSensitivityButtons(
        lowActivated: Boolean = false,
        mediumActivated: Boolean = false,
        highActivated: Boolean = false
    ) {
        binding?.lowSensitivityButton?.let {
            it.isSelected = lowActivated
            it.isEnabled = !lowActivated
        }

        binding?.mediumSensitivityButton?.let {
            it.isSelected = mediumActivated
            it.isEnabled = !mediumActivated
        }

        binding?.highSensitivityButton?.let {
            it.isSelected = highActivated
            it.isEnabled = !highActivated
        }

    }
}