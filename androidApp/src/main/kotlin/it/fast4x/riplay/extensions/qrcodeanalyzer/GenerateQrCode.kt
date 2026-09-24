package it.fast4x.riplay.extensions.qrcodeanalyzer

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Genera un QR Code in formato Bitmap.
 *
 * @param content Il testo/URL da codificare.
 * @param size La dimensione in pixel dell'immagine quadrata risultante.
 * @return Bitmap del QR Code, o null se il contenuto è troppo lungo o invalido.
 */
fun generateQrCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        // 1. ZXing genera una matrice di bit (true = nero, false = bianco)
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

        // 2. Creiamo una bitmap vuota
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        // 3. Creiamo un array di interi per i pixel (molto più veloce di setPixel)
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                // Se il bit è true, il pixel è nero, altrimenti bianco
                pixels[y * size + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        // 4. Applichiamo l'array alla bitmap in un colpo solo
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        bitmap

    } catch (e: WriterException) {
        // Viene lanciata se la stringa è troppo lunga per un QR code
        null
    } catch (e: Exception) {
        null
    }
}