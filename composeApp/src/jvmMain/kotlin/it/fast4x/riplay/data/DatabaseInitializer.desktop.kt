package it.fast4x.riplay.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Registry dell'istanza singleton del database per Desktop (Windows/Linux/macOS).
 *
 * Il file del database vive nella directory dati utente standard:
 * - Windows: %APPDATA%\RiPlay\data.db
 * - Linux:   ~/.local/share/RiPlay/data.db  (rispetta $XDG_DATA_HOME)
 * - macOS:   ~/Library/Application Support/RiPlay/data.db
 *
 * FIX bug precedente: il builder originale puntava a java.io.tmpdir (dati persi a ogni avvio).
 */
object DesktopDbRegistry {

    @Volatile
    private var raw: DatabaseInitializer? = null

    val instance: DatabaseInitializer
        get() = raw ?: synchronized(this) {
            raw ?: build().also { raw = it }
        }

    fun databaseFilePath(): File {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val dir = when {
            os.contains("win") ->
                File(System.getenv("APPDATA") ?: File(home, "AppData/Roaming").absolutePath, "RiPlay")
            os.contains("mac") ->
                File(home, "Library/Application Support/RiPlay")
            else ->
                File(
                    System.getenv("XDG_DATA_HOME") ?: File(home, ".local/share").absolutePath,
                    "RiPlay"
                )
        }
        if (!dir.exists()) dir.mkdirs()
        return File(dir, DatabaseInitializer.DB_NAME)
    }

    private fun build(): DatabaseInitializer =
        Room.databaseBuilder<DatabaseInitializer>(name = databaseFilePath().absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(*DatabaseInitializer.MANUAL_MIGRATIONS.toTypedArray())
            .addCallback(DatabaseInitializer.DbCallback)
            .build()
}

actual fun getDatabaseInitializer(): DatabaseInitializer = DesktopDbRegistry.instance

actual fun getDatabaseDao(): Database = DesktopDbRegistry.instance.databaseDao()
