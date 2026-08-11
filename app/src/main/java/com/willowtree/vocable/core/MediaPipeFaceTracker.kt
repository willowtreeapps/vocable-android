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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Prototype (#676) tracking source: MediaPipe FaceDetector + CameraX, as an alternative to the
 * ARCore-based path in FaceTrackingScreen/FaceTrackingViewModel.onSceneUpdate. Dumb detection
 * source only — no feel/smoothing tuning here, that lives in FaceTrackingViewModel.onMediaPipeUpdate.
 */
class MediaPipeFaceTracker(
    context: Context,
    private val onNoseTip: (NormalizedKeypoint?) -> Unit,
) {

    companion object {
        private const val MODEL_ASSET_PATH = "face_detection_short_range.tflite"

        // FaceDetectorResult.keypoints() ordering is fixed: right eye, left eye, nose tip,
        // mouth center, right ear tragion, left ear tragion.
        private const val NOSE_TIP_KEYPOINT_INDEX = 2
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val faceDetector: FaceDetector = FaceDetector.createFromOptions(
        context,
        FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET_PATH)
                    .setDelegate(Delegate.CPU)
                    .build()
            )
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
        faceDetector.close()
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
        faceDetector.detectAsync(mpImage, frameTime)
    }

    // Row-stride-aware RGBA_8888 copy. A plain bitmap.copyPixelsFromBuffer(plane.buffer)
    // assumes rowStride == width * pixelStride, which isn't guaranteed on every
    // device/resolution - when it's wrong, the copy silently garbles the image into noise
    // (FaceDetector then finds zero faces, rather than crashing) instead of throwing.
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

    private fun onDetectorResult(result: com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult, input: com.google.mediapipe.framework.image.MPImage) {
        val noseTip = result.detections()
            .firstOrNull()
            ?.keypoints()
            ?.orElse(null)
            ?.getOrNull(NOSE_TIP_KEYPOINT_INDEX)
        // Prototype (#676) diagnostic - remove once the coordinate mapping is confirmed correct.
        if (noseTip != null) {
            Timber.d("MediaPipeFaceTracker noseTip x=%.3f y=%.3f inputWxH=%dx%d", noseTip.x(), noseTip.y(), input.width, input.height)
        } else {
            Timber.d("MediaPipeFaceTracker noseTip null inputWxH=%dx%d", input.width, input.height)
        }
        onNoseTip(noseTip)
    }
}

private fun <T> List<T>.getOrNull(index: Int): T? = if (index in indices) this[index] else null
