package com.willowtree.vocable.splash

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.MainActivity


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = km.newKeyguardLock("TAG")
        keyguardLock.disableKeyguard()
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)


        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProviders.of(
            this,
            BaseViewModelFactory()
        ).get(SplashViewModel::class.java)

        viewModel.exitSplash.observe(this, Observer {
            if (it) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        })
    }
}