package com.willowtree.vocable

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.willowtree.vocable.core.ComposeGazeTarget
import com.willowtree.vocable.core.FaceTrackingManager
import com.willowtree.vocable.core.FaceTrackingPointerUpdates
import com.willowtree.vocable.core.GazeInteractionManager
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.VocableEnvironment
import com.willowtree.vocable.core.VocableEnvironmentType
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.ui.VocableNavHost
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.facetracking.FaceTrackingEvent
import com.willowtree.vocable.ui.facetracking.FaceTrackingScreen
import com.willowtree.vocable.ui.facetracking.MediaPipeFaceTrackingScreen
import com.willowtree.vocable.ui.theme.VocableTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

// Prototype (#676/#629) debug toggle states - see AppContent's Button.
private enum class TrackingMode { AR_CORE_NOSE_TIP, AR_CORE_CENTER_POSE, MEDIA_PIPE }

class MainActivity : ScopeActivity() {

    private val sharedPrefs: IVocableSharedPreferences by inject()
    private val faceTrackingManager: FaceTrackingManager by inject()
    private val environment: VocableEnvironment by inject()

    private val faceTrackingViewModel: com.willowtree.vocable.ui.facetracking.FaceTrackingViewModel by viewModel()

    private var pointerView: PointerView? = null
    private var currentGazeTarget: ComposeGazeTarget? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (environment.environmentType != VocableEnvironmentType.TESTING) {
            lifecycleScope.launch {
                faceTrackingManager.initialize(
                    faceTrackingPointerUpdates = object : FaceTrackingPointerUpdates {
                        override fun toggleVisibility(visible: Boolean) {
                            pointerView?.visibility = if (visible) View.VISIBLE else View.GONE
                        }
                    }
                )
            }
        }

        lifecycleScope.launch {
            faceTrackingViewModel.uiState.collectLatest { state ->
                state.pointerLocation?.let { pointer ->
                    updatePointer(pointer.x, pointer.y)
                }
            }
        }

        supportActionBar?.hide()
        VocableTextToSpeech.initialize(this)

        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent {
                VocableTheme {
                    AppContent()
                }
            }
        }

        setContentView(composeView)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            VocableTextToSpeech.shutdown()
        }
    }

    @Composable
    private fun AppContent() {
        BackHandler {
            finish()
        }

        // Prototype (#676) demo aid: local, non-persisted toggle so the tracking sources can
        // be flipped live for a side-by-side comparison, without a rebuild. Debug-only - never
        // appears in release builds. Defaults to the build-time flag's value. Three-way:
        // ARCore's existing NOSE_TIP region pose, ARCore's fuller centerPose (per #629/#676
        // findings that ARCore's own signal richness might close the gap with iOS, not just
        // switching tracking libraries), and MediaPipe FaceLandmarker.
        var trackingMode by remember {
            mutableStateOf(if (BuildConfig.USE_MEDIAPIPE_TRACKING) TrackingMode.MEDIA_PIPE else TrackingMode.AR_CORE_NOSE_TIP)
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                VocableNavHost()

                if (environment.environmentType != VocableEnvironmentType.TESTING) {
                    MviScreen(
                        viewModel = faceTrackingViewModel,
                        onEvent = { event ->
                            when (event) {
                                is FaceTrackingEvent.Speak -> {
                                    val selectionWasStale = VocableTextToSpeech.speak(
                                        locale = Locale.getDefault(),
                                        text = event.text,
                                        selectedVoiceName = sharedPrefs.getSelectedVoiceName()
                                    )
                                    if (selectionWasStale) sharedPrefs.setSelectedVoiceName(null)
                                }
                            }
                        }
                    ) { _ ->
                        when (trackingMode) {
                            TrackingMode.MEDIA_PIPE -> MediaPipeFaceTrackingScreen(viewModel = faceTrackingViewModel)
                            TrackingMode.AR_CORE_NOSE_TIP -> FaceTrackingScreen(viewModel = faceTrackingViewModel, useCenterPose = false)
                            TrackingMode.AR_CORE_CENTER_POSE -> FaceTrackingScreen(viewModel = faceTrackingViewModel, useCenterPose = true)
                        }
                    }
                }

                if (BuildConfig.DEBUG) {
                    Button(
                        onClick = {
                            trackingMode = when (trackingMode) {
                                TrackingMode.AR_CORE_NOSE_TIP -> TrackingMode.AR_CORE_CENTER_POSE
                                TrackingMode.AR_CORE_CENTER_POSE -> TrackingMode.MEDIA_PIPE
                                TrackingMode.MEDIA_PIPE -> TrackingMode.AR_CORE_NOSE_TIP
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            "Using: " + when (trackingMode) {
                                TrackingMode.AR_CORE_NOSE_TIP -> "ARCore (nose tip)"
                                TrackingMode.AR_CORE_CENTER_POSE -> "ARCore (center pose)"
                                TrackingMode.MEDIA_PIPE -> "MediaPipe"
                            }
                        )
                    }
                }
            }
        }
    }

    private fun updatePointer(x: Float, y: Float) {
        val pointer = pointerView ?: return

        var newX = x
        var newY = y
        if (x < 0) newX = 0f
        else if (x > faceTrackingManager.displayMetrics.widthPixels) {
            newX = faceTrackingManager.displayMetrics.widthPixels.toFloat()
        }
        if (y < 0) newY = 0f
        else if (y > faceTrackingManager.displayMetrics.heightPixels) {
            newY = faceTrackingManager.displayMetrics.heightPixels.toFloat()
        }

        if (sharedPrefs.getHeadTrackingEnabled()) {
            pointer.visibility = View.VISIBLE
            pointer.bringToFront()
            pointer.updatePointerPosition(newX, newY)
            checkGazeInteractions(newX, newY, pointer)
        } else {
            pointer.visibility = View.GONE
        }
    }

    private fun checkGazeInteractions(pointerX: Float, pointerY: Float, pointerView: View) {
        val pointerCenterX = pointerX + pointerView.width / 2
        val pointerCenterY = pointerY + pointerView.height / 2

        val hitTarget = GazeInteractionManager.getTargets().find { target ->
            target.bounds.contains(pointerCenterX.toInt(), pointerCenterY.toInt())
        }

        if (currentGazeTarget != hitTarget) {
            currentGazeTarget?.onExit()
            currentGazeTarget = hitTarget
            currentGazeTarget?.onEnter()
        }
    }
}

class PointerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    fun updatePointerPosition(x: Float, y: Float) {
        translationX = x
        translationY = y
        invalidate()
    }
}
