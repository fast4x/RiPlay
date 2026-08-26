package it.fast4x.riplay.utils

import android.util.Base64
import timber.log.Timber
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val IV_SIZE = 12
    private const val TAG_BIT_LENGTH = 128

    // Salt fisso dell'applicazione per irrobustire la generazione della chiave software.
    private val SALT = byteArrayOf(
        0x6a, 0x42, 0x74, 0x39, 0x2f, 0x6d, 0x4e, 0x51,
        0x7a, 0x31, 0x6c, 0x57, 0x4b, 0x3d, 0x6f, 0x50
    )

    /**
     * Genera una chiave AES a 256-bit deterministica e portabile.
     * Sarà identica su qualsiasi dispositivo su cui gira RiPlay.
     */
    private fun getSecretKey(): SecretKeySpec {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            // Usiamo il package id fisso dell'app unito al nostro SALT
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

    /**
     * Cifra una stringa in modalità AES-GCM software.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            // Genera automaticamente un IV (Initialization Vector) casuale e sicuro per questa sessione
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Uniamo l'IV e il testo cifrato in un unico array
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.e("CryptoManager Errore durante l'encryption: ${e.message}")
            ""
        }
    }

    /**
     * Decifra una stringa cifrata con AES-GCM software.
     * Fallisce silenziosamente invece di andare in crash se la decryptazione non va a buon fine a causa di una chiave errata.
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""

        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size <= IV_SIZE) return ""

            // Estraiamo l'IV dai primi 12 byte e il payload cifrato dal resto
            val iv = combined.copyOfRange(0, IV_SIZE)
            val encryptedBytes = combined.copyOfRange(IV_SIZE, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(TAG_BIT_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Se fallisce  restituisce una stringa vuota ed evita il crash dell'app.
            Timber.e("CryptoManager Decrittografia fallita (Dati obsoleti o corrotti): ${e.message}")
            ""
        }
    }
}
