package com.willowtree.vocable.presets

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.MySayingsEmptyLayoutBinding

class MySayingsEmptyFragment : BaseFragment<MySayingsEmptyLayoutBinding>() {

    override val bindingInflater: BindingInflater<MySayingsEmptyLayoutBinding> = MySayingsEmptyLayoutBinding::inflate

    companion object {
        private const val KEY_IS_SETTINGS = "KEY_IS_SETTINGS"

        fun newInstance(isSettings: Boolean): MySayingsEmptyFragment {
            return MySayingsEmptyFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(KEY_IS_SETTINGS, isSettings)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        arguments?.getBoolean(KEY_IS_SETTINGS)?.let { isSettings ->
            when {
                isSettings -> {
                    binding.star.isVisible = true
                    binding.emptyMySayingsText.setText(R.string.my_sayings_empty_settings)
                }
                !isSettings && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !resources.getBoolean(R.bool.is_tablet) -> {
                    val param = binding.emptyMySayingsText.layoutParams as ConstraintLayout.LayoutParams
                    param.setMargins(0,0,0,0)
                    binding.emptyMySayingsText.layoutParams = param
                }
                else -> {
                    // no-op
                }
            }
        }

        return binding.root
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }
}