package it.fast4x.riplay.extensions.qrcodeanalyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Configuriamo il lettore per cercare SOLO QR Code (ottimizza le prestazioni)
    private val reader = MultiFormatReader().apply {
        val hints = mapOf<DecodeHintType, Any>(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        setHints(hints)
    }

    @Volatile
    private var isScanning = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            isScanning = true

            try {
                // 1. Estrai il piano Y (Luminosità) dal formato YUV_420_888
                val yBuffer = mediaImage.planes[0].buffer
                val ySize = yBuffer.remaining()
                val yBytes = ByteArray(ySize)
                yBuffer.get(yBytes)

                // 2. Crea la sorgente per ZXing
                val source = PlanarYUVLuminanceSource(
                    yBytes,
                    mediaImage.width,
                    mediaImage.height,
                    0, 0,
                    mediaImage.width,
                    mediaImage.height,
                    false
                )

                // 3. Crea la bitmap binaria per il decodificatore
                val bitmap = BinaryBitmap(HybridBinarizer(source))

                // 4. Tentativo di decodifica
                try {
                    val result = reader.decode(bitmap)
                    result.text?.let { qrText ->
                        onQrCodeDetected(qrText)
                    }
                } catch (e: NotFoundException) {
                    // Comportamento normale: significa che in questo frame non c'è un QR
                } catch (e: Exception) {
                    // Altri errori (Checksum, formato), li ignoriamo per non interrompere lo stream
                } finally {
                    // FONDAMENTALE: il lettore va resettato
                    reader.reset()
                }

            } catch (e: Exception) {
                // Errore nell'estrazione del buffer Y
            } finally {
                isScanning = false
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}