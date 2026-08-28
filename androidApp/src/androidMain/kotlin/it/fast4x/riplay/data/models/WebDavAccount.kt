package it.fast4x.riplay.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "webdav_account",
    indices = [Index(value = ["baseUrl", "username"], unique = true)] // Evita duplicati
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
    val musicFolder: String = "/" // Il path della cartella musica per questo account
)