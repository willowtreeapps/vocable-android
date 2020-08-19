package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentTimingSensitivityBinding
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.android.ext.android.inject
import java.text.DecimalFormat

class SensitivityFragment : BaseFragment<FragmentTimingSensitivityBinding>() {

    companion object {
        private const val LOW_SENSITIVITY = 0.05F
        const val MEDIUM_SENSITIVITY = 0.1F
        private const val HIGH_SENSITIVITY = 0.15F
        private const val DWELL_TIME_CHANGE = 500L
        const val DWELL_TIME_ONE_SECOND = 1000L
        private const val MIN_DWELL_TIME = 500L
        private const val MAX_DWELL_TIME = 4000L
    }

    override val bindingInflater: BindingInflater<FragmentTimingSensitivityBinding> = FragmentTimingSensitivityBinding::inflate

    private val sharedPrefs: VocableSharedPreferences by inject()
    private var dwellTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        dwellTime = sharedPrefs.getDwellTime()
        setDwellTimeText()
        return binding.root
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

        binding.timingSensitivityBackButton.action = {
            findNavController().popBackStack()
        }

        binding.decreaseHoverTime.action = {
            setDwellTime(false)
        }

        binding.increaseHoverTime.action = {
            setDwellTime(true)
        }

        binding.lowSensitivityButton.action = {
            sharedPrefs.setSensitivity(LOW_SENSITIVITY)
            toggleSensitivityButtons(lowActivated = true)
        }

        binding.mediumSensitivityButton.action = {
            sharedPrefs.setSensitivity(MEDIUM_SENSITIVITY)
            toggleSensitivityButtons(mediumActivated = true)
        }

        binding.highSensitivityButton.action = {
            sharedPrefs.setSensitivity(HIGH_SENSITIVITY)
            toggleSensitivityButtons(highActivated = true)
        }

    }

    override fun getAllViews(): List<View> = emptyList()

    private fun toggleSensitivityButtons(
        lowActivated: Boolean = false,
        mediumActivated: Boolean = false,
        highActivated: Boolean = false
    ) {
        binding.lowSensitivityButton.apply {
            isSelected = lowActivated
            isEnabled = !lowActivated
        }

        binding.mediumSensitivityButton.apply {
            isSelected = mediumActivated
            isEnabled = !mediumActivated
        }

        binding.highSensitivityButton.apply {
            isSelected = highActivated
            isEnabled = !highActivated
        }

    }

    private fun setDwellTime(increase: Boolean) {
        dwellTime = if (increase) {
            dwellTime + DWELL_TIME_CHANGE
        } else {
            dwellTime - DWELL_TIME_CHANGE
        }
        sharedPrefs.setDwellTime(dwellTime)
        setDwellTimeText()

        when {
            dwellTime >= MAX_DWELL_TIME -> {
                binding.increaseHoverTime.isEnabled = false
            }
            dwellTime <= MIN_DWELL_TIME -> {
                binding.decreaseHoverTime.isEnabled = false
            }
            else -> {
                binding.apply {
                    increaseHoverTime.isEnabled = true
                    decreaseHoverTime.isEnabled = true
                }
            }
        }
    }

    private fun setDwellTimeText() {
        if (dwellTime == DWELL_TIME_ONE_SECOND) {
            binding.hoverTimeText.text = getString(R.string.hover_time_one_text)
        } else {
            val df = DecimalFormat("#.#")
            binding.hoverTimeText.text = getString(
                R.string.hover_time_amount_text,
                df.format(dwellTime.toDouble() / DWELL_TIME_ONE_SECOND)
            )
        }
    }
}
