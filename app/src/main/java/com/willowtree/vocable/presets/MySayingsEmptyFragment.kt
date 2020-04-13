package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.databinding.MySayingsEmptyLayoutBinding

class MySayingsEmptyFragment: BaseFragment() {

    private var binding: MySayingsEmptyLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MySayingsEmptyLayoutBinding.inflate(inflater, container, false)

        return binding?.root
    }


    override fun getAllViews(): List<View> {
        return emptyList()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}