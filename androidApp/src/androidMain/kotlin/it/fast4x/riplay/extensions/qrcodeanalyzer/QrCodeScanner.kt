package it.fast4x.riplay.extensions.qrcodeanalyzer

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QrCodeScanner(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
    onError: (Exception) -> Unit = {}
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val backgroundExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier
    ) { previewView ->

        scope.launch {
            val cameraProvider = withContext(Dispatchers.IO) {
                cameraProviderFuture.get()
            }

            try {
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(backgroundExecutor, QrCodeAnalyzer(onQrCodeScanned))
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.bindToLifecycle(
                    previewView.findViewTreeLifecycleOwner()!!,
                    cameraSelector,
                    preview, imageAnalysis
                )

            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}