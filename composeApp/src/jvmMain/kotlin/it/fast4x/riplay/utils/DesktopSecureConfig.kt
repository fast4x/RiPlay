package it.fast4x.riplay.utils

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * PORTING DESKTOP di SecureUtils.kt (androidApp): decifratura delle
 * chiavi di configurazione generate da composeApp/build.gradle.kts
 * (it.fast4x.riplay.config.EnvironmentConfig).
 *
 * Stessa logica dell'originale Android (AES/GCM, vettore di
 * inizializzazione a 12 byte zero, stesso master key byte-per-byte):
 * i valori cifrati presenti in gradle.properties si decifrano quindi
 * in modo IDENTICO su desktop e su Android, senza alcuna duplicazione
 * di segreti.
 *
 * Tutto e' java.* puro (javax.crypto + java.util.Base64): gira su JVM
 * desktop senza dipendenze Android.
 */
object DesktopSecureConfig {

    private val masterKeyBytes = byteArrayOf(
        98, 89, 53, 82, 103, 107, 56, 119, 53, 98, 56, 100,
        50, 122, 57, 106, 55, 84, 53, 108, 48, 87, 50, 33, 33, 49, 50, 98, 110, 81, 56, 122
    )

    /** Decifra un valore di configurazione; stringa vuota = valore vuoto. */
    fun getApiKey(apiKey: String): String {
        if (apiKey.isEmpty()) return ""
        return runCatching { decrypt(apiKey, masterKeyBytes) }
            .onFailure { println("[RiPlay-SecureConfig] decifratura fallita: ${it.javaClass.simpleName}") }
            .getOrDefault("")
    }

    private fun decrypt(str: String, key: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        // Initialization vector a zero, come SecureUtils.kt Android
        val iv = ByteArray(12)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val decoded = Base64.getDecoder().decode(str)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted)
    }
}
