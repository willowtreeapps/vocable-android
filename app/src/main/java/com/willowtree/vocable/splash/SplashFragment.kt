package com.willowtree.vocable.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.willowtree.vocable.*
import com.willowtree.vocable.databinding.FragmentSplashBinding
import kotlinx.android.synthetic.main.fragment_splash.*

class SplashFragment: BaseFragment<FragmentSplashBinding>() {
    override val bindingInflater: BindingInflater<FragmentSplashBinding> = FragmentSplashBinding::inflate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProviders.of(
            this,
            BaseViewModelFactory()
        ).get(SplashViewModel::class.java)

        viewModel.exitSplash.observe(this, Observer {
            if (it) {
                findNavController().navigate(R.id.action_splashFragment_to_presetsFragment)
            }
        })
    }

    override fun getAllViews(): List<View> {
        return mutableListOf(splash_background)
    }
}