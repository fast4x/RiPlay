package it.fast4x.riplay.data.models

import it.fast4x.riplay.R
import it.fast4x.riplay.utils.CryptoManager

/**
 * Extension Android per WebDavAccount (model migrato in commonMain senza dipendenze android):
 * - decryptedPassword: usa CryptoManager (javax.crypto, Android/JVM)
 * - icon: risorse drawable Android
 */
val WebDavAccount.decryptedPassword: String
    get() = CryptoManager.decrypt(encryptedPassword)

val WebDavAccount.icon: Int
    get() = if (isMusicSource) R.drawable.musical_note else R.drawable.sync
