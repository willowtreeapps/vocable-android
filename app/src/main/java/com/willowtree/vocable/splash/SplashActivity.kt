package com.willowtree.vocable.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.R
import org.koin.android.ext.android.inject

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: SplashViewModel by inject()

        viewModel.exitSplash.observe(this, Observer {
            if (it) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        })
    }
}