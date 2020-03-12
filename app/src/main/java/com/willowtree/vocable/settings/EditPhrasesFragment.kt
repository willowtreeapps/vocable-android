package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditPhrasesBinding

class EditPhrasesFragment: BaseFragment() {

    private var binding: FragmentEditPhrasesBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditPhrasesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.backButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}