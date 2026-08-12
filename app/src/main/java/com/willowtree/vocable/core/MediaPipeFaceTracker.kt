package com.willowtree.vocable.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Prototype (#676) tracking source: MediaPipe FaceLandmarker + CameraX, as an alternative to the
 * ARCore-based path in FaceTrackingScreen/FaceTrackingViewModel.onSceneUpdate. Dumb detection
 * source only — no feel/smoothing tuning here, that lives in FaceTrackingViewModel.onMediaPipeUpdate.
 *
 * Branched off prototype/mediapipe-facedetector: that branch used FaceDetector's bare 2D
 * nose-tip keypoint, which is a materially weaker signal for head *rotation* than ARKit's
 * pose-based approach on iOS (position vs. orientation). FaceLandmarker's
 * facialTransformationMatrixes gives an actual head-pose matrix per face - this class decodes
 * the transformed local Z-axis ("which way the head is pointing") out of it, the same class of
 * signal ARKit's ARFaceAnchor.transform gives iOS's HeadGazeTrackingInterpolator.
 */
class MediaPipeFaceTracker(
    context: Context,
    private val onHeadForward: (Offset?) -> Unit,
) {

    companion object {
        private const val MODEL_ASSET_PATH = "face_landmarker.task"
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    // Prototype (#676) latency diagnostic: time between successive result callbacks, i.e. the
    // actual achieved update rate, separate from per-frame inference latency.
    private var lastResultTimeMs = 0L

    private val faceLandmarker: FaceLandmarker = FaceLandmarker.createFromOptions(
        context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET_PATH)
                    // GPU delegate tried and measured (2026-08-12, Pixel 3a): silently fell
                    // back to CPU (XNNPack) per logcat, no latency/interval improvement -
                    // reverted to explicit CPU rather than keep a no-op delegate request.
                    .setDelegate(Delegate.CPU)
                    .build()
            )
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setOutputFaceBlendshapes(false)
            .setOutputFacialTransformationMatrixes(true)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(::onDetectorResult)
            .setErrorListener { error -> Timber.e(error, "MediaPipeFaceTracker detection error") }
            .build()
    )

    fun bind(lifecycleOwner: LifecycleOwner, context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, ::detectLivestreamFrame) }
                imageAnalysis = analysis

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                provider.unbindAll()
                try {
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
                } catch (e: Exception) {
                    Timber.e(e, "MediaPipeFaceTracker camera bind failed")
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageAnalysis = null
    }

    fun close() {
        unbind()
        analysisExecutor.shutdown()
        faceLandmarker.close()
    }

    private fun detectLivestreamFrame(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()

        val bitmap = imageProxy.use { it.toRgbaBitmap() }

        // Two separate steps, not one combined Matrix - a 90/270 degree rotation swaps
        // width/height, so the mirror step's pivot must use the ROTATED bitmap's own
        // dimensions, not the pre-rotation ones, or the result is offset, not just reflected.
        val rotateMatrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, rotateMatrix, true
        )

        val mirrorMatrix = Matrix().apply {
            postScale(-1f, 1f, rotatedBitmap.width.toFloat(), rotatedBitmap.height.toFloat())
        }
        val mirroredBitmap = Bitmap.createBitmap(
            rotatedBitmap, 0, 0, rotatedBitmap.width, rotatedBitmap.height, mirrorMatrix, true
        )

        val mpImage = BitmapImageBuilder(mirroredBitmap).build()
        faceLandmarker.detectAsync(mpImage, frameTime)
    }

    // Row-stride-aware RGBA_8888 copy. A plain bitmap.copyPixelsFromBuffer(plane.buffer)
    // assumes rowStride == width * pixelStride, which isn't guaranteed on every
    // device/resolution - when it's wrong, the copy silently garbles the image into noise
    // (detection then finds zero faces, rather than crashing) instead of throwing.
    private fun ImageProxy.toRgbaBitmap(): Bitmap {
        val plane = planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val buffer = plane.buffer

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (rowStride == width * pixelStride) {
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }

        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val pixels = IntArray(width * height)
        for (row in 0 until height) {
            val rowStart = row * rowStride
            for (col in 0 until width) {
                val idx = rowStart + col * pixelStride
                val r = bytes[idx].toInt() and 0xFF
                val g = bytes[idx + 1].toInt() and 0xFF
                val b = bytes[idx + 2].toInt() and 0xFF
                val a = bytes[idx + 3].toInt() and 0xFF
                pixels[row * width + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun onDetectorResult(result: FaceLandmarkerResult, input: MPImage) {
        // Prototype (#676) latency diagnostic - detectAsync was called with frameTime =
        // SystemClock.uptimeMillis(), which comes back unchanged as result.timestampMs().
        // The delta is wall-clock inference latency (detectAsync call -> this callback),
        // not just model compute time - it also includes any queuing if a prior frame's
        // inference was still running (LIVE_STREAM drops overlapping frames, but the one
        // that does run still waits for the executor to be free).
        val now = SystemClock.uptimeMillis()
        val latencyMs = now - result.timestampMs()
        val intervalMs = if (lastResultTimeMs == 0L) 0L else now - lastResultTimeMs
        lastResultTimeMs = now

        val numFaces = result.faceLandmarks().size
        val matrices = result.facialTransformationMatrixes().orElse(null)
        val matrix = matrices?.firstOrNull()
        val forward = matrix?.let { headForwardYawPitch(it) }
        // Prototype (#676) diagnostic - remove once the sign/axis mapping is confirmed correct.
        // x/y here are yaw/pitch in DEGREES (converted from the atan2 radians result for readability).
        if (forward != null) {
            Timber.d(
                "MediaPipeFaceTracker yawDeg=%.2f pitchDeg=%.2f numFaces=%d latencyMs=%d intervalMs=%d",
                Math.toDegrees(forward.x.toDouble()),
                Math.toDegrees(forward.y.toDouble()),
                numFaces,
                latencyMs,
                intervalMs
            )
        } else {
            Timber.d("MediaPipeFaceTracker forward null numFaces=%d hasMatrixList=%b", numFaces, matrices != null)
        }
        onHeadForward(forward)
    }

    // facialTransformationMatrixes() is a flat column-major 4x4 matrix (16 floats): column c
    // occupies indices [4c, 4c+3]. Column index 2 (elements[8..10]) is the transformed local
    // Z-axis ("which way the head is pointing" in camera space) - the same signal ARKit's
    // ARFaceAnchor.transform gives iOS.
    //
    // NOT read as raw X/Y components directly: for a handheld phone (not on a fixed
    // perpendicular stand like a typical mounted AAC setup), this forward vector for a
    // straight-ahead pose is genuinely diagonal (measured on-device: ~(0.69, 0.07, 0.72), not
    // ~(0,0,1)) - the natural angle between the phone's camera axis and "looking straight at
    // the screen" when held by hand. Reading raw X/Y off a vector already this far from
    // (0,0,1) responds very non-linearly to actual head rotation. Converting to yaw/pitch
    // angles via atan2 (using all 3 components) behaves linearly with real head rotation
    // regardless of that baseline tilt, which raw vector components don't.
    private fun headForwardYawPitch(matrix: FloatArray): Offset {
        val fx = matrix[8]
        val fy = matrix[9]
        val fz = matrix[10]
        val yaw = atan2(fx.toDouble(), fz.toDouble()).toFloat()
        val pitch = atan2(-fy.toDouble(), sqrt((fx * fx + fz * fz).toDouble())).toFloat()
        return Offset(yaw, pitch)
    }
}
