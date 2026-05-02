package it.fast4x.riplay.utils

import android.content.Context
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object SecureConfig {

    private val masterKeyBytes = byteArrayOf(98, 89, 53, 82, 103, 107, 56, 119, 53, 98, 56, 100,
        50, 122, 57, 106, 55, 84, 53, 108, 48, 87, 50, 33, 33, 49, 50, 98, 110, 81, 56, 122)

    fun getApiKey(apiKey: String): String {

        if (apiKey.isEmpty()) return ""

        return decrypt(apiKey, masterKeyBytes)
    }

    private fun decrypt(str: String, key: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        // Initialization vector
        val iv = ByteArray(12)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val decoded = Base64.getDecoder().decode(str)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted)
    }
}