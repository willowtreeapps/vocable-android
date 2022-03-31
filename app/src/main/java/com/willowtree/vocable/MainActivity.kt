package com.willowtree.vocable

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.NavHostFragment
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.databinding.ActivityMainBinding
import com.willowtree.vocable.facetracking.FaceTrackFragment
import com.willowtree.vocable.settings.SettingsViewModel
import com.willowtree.vocable.utils.VocableSharedPreferences
import com.willowtree.vocable.utils.VocableTextToSpeech
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import org.koin.android.ext.android.inject


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sharedPrefs: VocableSharedPreferences by inject()
    private val allViews = mutableListOf<View>()
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        VocableTextToSpeech.initialize(this)

        binding.mainNavHostFragment.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            allViews.clear()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onResume() {
        super.onResume()
        if (!BuildConfig.USE_HEAD_TRACKING) {
            binding.pointerView.isVisible = false
        } else {
            binding.pointerView.isVisible = sharedPrefs.getHeadTrackingEnabled()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VocableTextToSpeech.shutdown()
    }

    override fun getErrorView(): View = binding.errorView.root

    override fun getPointerView(): PointerView = binding.pointerView

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.parentLayout)
            getAllFragmentViews()
        }
        return allViews
    }

    fun resetAllViews() {
        allViews.clear()
    }

    override fun getLayout(): Int = R.layout.activity_main

    override fun subscribeToViewModel() {
        super.subscribeToViewModel()
        settingsViewModel = ViewModelProviders.of(
            this,
            BaseViewModelFactory()
        ).get(SettingsViewModel::class.java)
        
        settingsViewModel.headTrackingEnabled.observe(this, Observer {
            it?.let {
                val faceFragment = supportFragmentManager.findFragmentById(R.id.face_fragment)
                if (faceFragment is FaceTrackFragment) {
                    faceFragment.enableFaceTracking(it)
                }
            }
        })
    }

    private fun getAllChildViews(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }

    private fun getAllFragmentViews() {
        supportFragmentManager.fragments.forEach {
            if (it is BaseFragment<*>) {
                allViews.addAll(it.getAllViews())
            }
        }
    }
}