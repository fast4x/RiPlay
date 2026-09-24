package it.fast4x.riplay.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.CryptoManager

@Entity(
    tableName = "webdav_account",
    indices = [Index(value = ["baseUrl", "username", "remoteFolder"], unique = true)] // Evita duplicati
)
data class WebDavAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // Es. "NAS di Casa", "Nextcloud Personale"
    val baseUrl: String,
    val username: String,
    val encryptedPassword: String, // Sempre criptato con CryptoManager!

    // Se true, l'app scansionerà questo account per cercare musica.
    // Se false, l'app lo ignorerà nella libreria musicale e lo userà per i backup
    val isMusicSource: Boolean = true,
    val remoteFolder: String = "/", // Il path della cartella predefinita per questo account
    val scanSubFolders: Boolean = false // Scansiona le sottocartelle della cartella musica
) {
    val decryptedPassword: String
        get() = CryptoManager.decrypt(encryptedPassword)

    val icon: Int
    get() = if (isMusicSource) R.drawable.musical_note else R.drawable.sync
}

fun webDavAccountEmpty(): WebDavAccount {
    return WebDavAccount(
        id = 0L,
        name = "",
        baseUrl = "",
        username = "",
        encryptedPassword = ""
    )
}