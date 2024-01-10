package com.willowtree.vocable

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.databinding.ActivityMainBinding
import com.willowtree.vocable.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.utils.FaceTrackingManager
import com.willowtree.vocable.utils.FaceTrackingPointerUpdates
import com.willowtree.vocable.utils.IVocableSharedPreferences
import com.willowtree.vocable.utils.VocableTextToSpeech
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.getViewModel

class MainActivity : ScopeActivity() {

    private var currentView: View? = null
    private var paused = false
    private lateinit var binding: ActivityMainBinding
    private val sharedPrefs: IVocableSharedPreferences by inject()
    private val allViews = mutableListOf<View>()

    private val faceTrackingManager: FaceTrackingManager by inject()

    private lateinit var faceTrackingViewModel: FaceTrackingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        faceTrackingViewModel = scope.getViewModel(owner = { ViewModelOwner.from(this) })

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            faceTrackingManager.initialize(
                faceTrackingPointerUpdates = object : FaceTrackingPointerUpdates {
                    override fun toggleVisibility(visible: Boolean) {
                        binding.pointerView.isVisible = visible
                    }
                })
        }

        faceTrackingViewModel.showError.observe(this) { showError ->
            if (!sharedPrefs.getHeadTrackingEnabled()) {
                getPointerView().isVisible = false
                getErrorView().isVisible = false
                return@observe
            }
            if (showError) {
                (currentView as? PointerListener)?.onPointerExit()
            }
            getErrorView().isVisible = showError
            getPointerView().isVisible = !showError
        }


        faceTrackingViewModel.pointerLocation.observe(this) {
            updatePointer(it.x, it.y)
        }

        supportActionBar?.hide()
        VocableTextToSpeech.initialize(this)

        binding.mainNavHostFragment.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            allViews.clear()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onDestroy() {
        super.onDestroy()
        VocableTextToSpeech.shutdown()
    }

    private fun getErrorView(): View = binding.errorView.root

    private fun getPointerView(): PointerView = binding.pointerView

    fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.parentLayout)
            getAllFragmentViews()
        }
        return allViews
    }

    fun resetAllViews() {
        allViews.clear()
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

    private fun updatePointer(x: Float, y: Float) {
        var newX = x
        var newY = y
        if (x < 0) {
            newX = 0f
        } else if (x > faceTrackingManager.displayMetrics.widthPixels) {
            newX = faceTrackingManager.displayMetrics.widthPixels.toFloat()
        }

        if (y < 0) {
            newY = 0f
        } else if (y > faceTrackingManager.displayMetrics.heightPixels) {
            newY = faceTrackingManager.displayMetrics.heightPixels.toFloat()
        }
        getPointerView().updatePointerPosition(newX, newY)
        getPointerView().bringToFront()

        if (currentView == null) {
            findIntersectingView()
        } else {
            if (!viewIntersects(currentView!!, getPointerView())) {
                (currentView as? PointerListener)?.onPointerExit()
                findIntersectingView()
            }
        }
    }

    private fun findIntersectingView() {
        currentView = null
        if (!paused) {
            getAllViews().forEach {
                if (viewIntersects(it, getPointerView())) {
                    if (it.isEnabled && it.isVisible) {
                        currentView = it
                        (currentView as PointerListener).onPointerEnter()
                        return
                    }
                }
            }
        }
    }

    private fun viewIntersects(view1: View, view2: View): Boolean {
        val coords = IntArray(2)
        view1.getLocationOnScreen(coords)
        val rect = Rect(
            coords[0],
            coords[1],
            coords[0] + view1.measuredWidth,
            coords[1] + view1.measuredHeight
        )

        val view2Coords = IntArray(2)
        view2.getLocationOnScreen(view2Coords)
        val view2Rect = Rect(
            view2Coords[0],
            view2Coords[1],
            view2Coords[0] + view2.measuredWidth,
            view2Coords[1] + view2.measuredHeight
        )
        return rect.contains(view2Rect.centerX(), view2Rect.centerY())
    }

}