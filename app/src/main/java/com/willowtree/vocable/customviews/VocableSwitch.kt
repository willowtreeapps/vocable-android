package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.VocableSwitchLayoutBinding

class VocableSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableConstraintLayout(context, attrs, defStyle){
    var binding: VocableSwitchLayoutBinding
    var isChecked: Boolean
        set(value) {
            binding.toggleSwitch.isChecked = value
        }
        get() = binding.toggleSwitch.isChecked

    var text: String = ""
        set(value) {
            binding.toggleTitle.text = value
        }

    init{
        binding = VocableSwitchLayoutBinding.inflate(LayoutInflater.from(context), this)
        binding.root.setBackgroundResource(R.drawable.settings_group_background)
    }

}