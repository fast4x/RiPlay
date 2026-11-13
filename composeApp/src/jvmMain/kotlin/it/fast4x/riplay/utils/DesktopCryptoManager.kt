package it.fast4x.riplay.utils

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * PORTING DESKTOP di CriptoManager.kt (androidApp): cifra/decifra le
 * password degli account WebDAV con AES/GCM software.
 *
 * COMPATIBILE BYTE-PER-BYTE con l'originale Android:
 *  - stessa chiave deterministica (SHA-256 di "it.fast4x.riplay" + SALT
 *    fisso dell'applicazione, identico su qualsiasi dispositivo);
 *  - stesso formato payload (IV di 12 byte concatenato al testo cifrato);
 *  - stessa codifica Base64 (android.util.Base64.NO_WRAP = getEncoder()
 *    di java.util.Base64: nessun a-capo).
 *
 * Grazie a cio' un backup Android ripristinato su desktop (o viceversa)
 * conserva account WebDAV utilizzabili. Tutto java.* puro.
 */
object DesktopCryptoManager {

    private const val IV_SIZE = 12
    private const val TAG_BIT_LENGTH = 128

    // Salt fisso dell'applicazione per irrobustire la generazione della chiave software.
    // STESI byte dell'originale Android (CriptoManager.kt).
    private val SALT = byteArrayOf(
        0x6a, 0x42, 0x74, 0x39, 0x2f, 0x6d, 0x4e, 0x51,
        0x7a, 0x31, 0x6c, 0x57, 0x4b, 0x3d, 0x6f, 0x50
    )

    /**
     * Genera una chiave AES a 256-bit deterministica e portabile.
     * Sara' identica su qualsiasi dispositivo su cui gira RiPlay.
     */
    private fun getSecretKey(): SecretKeySpec {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update("it.fast4x.riplay".toByteArray(Charsets.UTF_8))
            digest.update(SALT)
            val keyBytes = digest.digest() // Genera esattamente 32 byte (256 bit)
            SecretKeySpec(keyBytes, "AES")
        } catch (e: Exception) {
            // Fallback estremo in caso di anomalie algoritmiche
            val fallbackBytes = ByteArray(32) { i -> (i * 3).toByte() }
            SecretKeySpec(fallbackBytes, "AES")
        }
    }

    /** Cifra una stringa in modalita' AES-GCM software. */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            // IV casuale e sicuro generato per questa sessione
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // IV + testo cifrato in un unico array
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            println("[RiPlay-Crypto] errore encryption: ${e.message}")
            ""
        }
    }

    /**
     * Decifra una stringa cifrata con AES-GCM software.
     * Fallisce silenziosamente (stringa vuota) se i dati sono corrotti.
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""

        return try {
            val combined = Base64.getDecoder().decode(encryptedText)
            if (combined.size <= IV_SIZE) return ""

            // IV nei primi 12 byte, payload cifrato nel resto
            val iv = combined.copyOfRange(0, IV_SIZE)
            val encryptedBytes = combined.copyOfRange(IV_SIZE, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(TAG_BIT_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            println("[RiPlay-Crypto] decrittografia fallita (dati obsoleti o corrotti): ${e.message}")
            ""
        }
    }
}
