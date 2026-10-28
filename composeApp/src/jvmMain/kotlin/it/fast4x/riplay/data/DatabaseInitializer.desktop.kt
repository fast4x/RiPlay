package it.fast4x.riplay.data

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
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

    // STEP 3ae: nomi dei file di ripristino PENDENTE. DesktopBackup (ui)
    // estrae il backup con questi nomi accanto al db; qui, PRIMA di aprire
    // Room (quindi senza lock di scrittura, cross-platform), i file vengono
    // promossi a db corrente. Gli eventi sono registrati nel log.
    const val PENDING_DB = "data.db.restore-pending"
    const val PENDING_WAL = "data.db.restore-pending-wal"
    const val PENDING_SHM = "data.db.restore-pending-shm"

    // STEP 3an: archivio di sicurezza del db CORRENTE durante il restore.
    // Non viene mai cancellato al termine (sovrascritto al prossimo
    // restore): se il backup ripristinato si rivelasse indesiderato si puo'
    // tornare indietro a mano rinominandolo su data.db (a app spenta).
    const val PRE_RESTORE_DB = "data.db.pre-restore"

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

    /** STEP 3an: sidecar SQLite che accompagnano data.db (journal/wal/shm). */
    private fun sidecarFiles(parent: File): List<File> = listOf(
        File(parent, "${DatabaseInitializer.DB_NAME}-journal"),
        File(parent, "${DatabaseInitializer.DB_NAME}-wal"),
        File(parent, "${DatabaseInitializer.DB_NAME}-shm")
    )

    /**
     * STEP 3ae: applica un ripristino PENDENTE (import backup) prima che
     * Room apra il db: sostituisce data.db (e wal/shm se presenti nel
     * backup) con i file *.restore-pending scritti da DesktopBackup.
     * Chiamato SOLO da build(): a quell'istante nessuna connessione tiene
     * aperto il file, quindi la sostituzione e' sicura anche su Windows.
     *
     * FIX step3an (corruzione "database disk image is malformed" dopo un
     * restore): la versione 3ae sostituiva SOLO data.db e lasciava sul
     * disco i sidecar della sessione precedente (data.db-wal / -shm /
     * -journal). L'app esce con exitApplication() SENZA chiudere Room,
     * quindi quei file esistono praticamente sempre; confermato inoltre
     * con javap sul jar room-runtime 2.8.5 che Room KMP apre il db SEMPRE
     * in WAL (BaseRoomConnectionManager: "PRAGMA journal_mode = WAL").
     * Alla successiva apertura SQLite trovava il vecchio data.db-wal e NE
     * RIGIOCAVA le pagine (salt, size e contenuto del VECCHIO database)
     * dentro il db appena ripristinato: corruzione immediata, crash
     * "malformed" alla prima query (Albums).
     *
     * Il restore ora e':
     *  1. NON distruttivo: il db corrente viene ARCHIVIATO in
     *     data.db.pre-restore (non cancellato prima di avere la certezza
     *     che il nuovo sia a posto). Se l'archiviazione fallisce (file
     *     bloccato da un'altra istanza/antivirus) si interrompe TUTTO
     *     senza toccare nulla e si ritenta al prossimo avvio;
     *  2. COMPLETO: elimina SEMPRE journal + wal + shm del vecchio db.
     *     Un backup VACUUM INTO e' autosufficiente e non ha bisogno di
     *     alcun sidecar; i sidecar di uno zip legacy vengono promossi
     *     SOLO DOPO, insieme al loro db;
     *  3. VERIFICATO: PRAGMA quick_check sul file ripristinato PRIMA di
     *     consegnarlo a Room. Se fallisce -> rollback automatico al db
     *     precedente e pending rimosso (nessun loop, nessun crash).
     */
    private fun applyPendingRestoreIfNeeded() {
        val dbFile = databaseFilePath()
        val parent = dbFile.parentFile
        val pending = File(parent, PENDING_DB)
        if (!pending.isFile) return
        val preRestore = File(parent, PRE_RESTORE_DB)
        val sidecars = sidecarFiles(parent)

        println(
            "[RiPlay-Backup] restore pendente: backup=${pending.length()}B, " +
                "db corrente=${if (dbFile.isFile) "${dbFile.length()}B" else "assente"}, " +
                "sidecar=[${sidecars.filter { it.isFile }.joinToString { it.name }}]"
        )

        runCatching {
            // 1. archivia il db corrente (NON distruttivo)
            preRestore.delete()
            if (dbFile.isFile) {
                check(dbFile.renameTo(preRestore)) {
                    "archiviazione del db corrente fallita (file bloccato?)"
                }
            }

            // 2. promuovi il backup a db corrente
            check(pending.renameTo(dbFile)) { "promozione di ${pending.name} fallita" }

            // 3. il nuovo db non ha NESSUN legame con i sidecar vecchi: via
            //    tutti (QUESTO era il bug 3ae: il vecchio wal sopravviveva e
            //    veniva rigiocato dentro il db nuovo alla prima apertura)
            sidecars.forEach { f ->
                if (f.isFile && !f.delete())
                    println("[RiPlay-Backup] ATTENZIONE: ${f.name} non eliminato")
            }

            // 4. sidecar CON il backup (solo zip legacy desktop): promossi
            //    insieme al loro db, MAI mescolati con quelli del vecchio
            File(parent, PENDING_WAL).takeIf { it.isFile }?.let {
                check(it.renameTo(File(parent, "${DatabaseInitializer.DB_NAME}-wal"))) {
                    "promozione wal del backup fallita"
                }
            }
            File(parent, PENDING_SHM).takeIf { it.isFile }?.let {
                check(it.renameTo(File(parent, "${DatabaseInitializer.DB_NAME}-shm"))) {
                    "promozione shm del backup fallita"
                }
            }

            // 5. verifica lo stato FINALE (db + eventuali sidecar promossi)
            //    prima di consegnare il file a Room
            check(quickCheckOk(dbFile)) { "quick_check del backup ripristinato FALLITO: file corrotto" }

            println(
                "[RiPlay-Backup] ripristino pendente APPLICATO " +
                    "(db=${dbFile.length()}B, copia di sicurezza: ${preRestore.name})"
            )
        }.onFailure { e ->
            println("[RiPlay-Backup] ripristino pendente FALLITO: ${e.message}")
            rollbackPendingRestore(dbFile, preRestore, parent)
        }
    }

    /**
     * STEP 3an: PRAGMA quick_check su un file SQLite SENZA passare da Room
     * (Room non e' ancora stato costruito: si usa direttamente il driver
     * bundled, gli stessi identici metodi usati da DatabaseBackupManager
     * Android: prepare/step/getText). Aperta e chiusa subito: se il file
     * e' in WAL la connessione viene ripulita alla chiusura, quindi nessun
     * sidecar residuo della verifica.
     */
    private fun quickCheckOk(file: File): Boolean {
        if (!file.isFile) return false
        var conn: SQLiteConnection? = null
        return try {
            conn = BundledSQLiteDriver().open(file.absolutePath)
            val stmt = conn.prepare("PRAGMA quick_check")
            val ok = try {
                stmt.step() && stmt.getText(0) == "ok"
            } finally {
                stmt.close()
            }
            ok
        } catch (e: Throwable) {
            println("[RiPlay-Backup] quick_check eccezione su ${file.name}: ${e.message}")
            false
        } finally {
            runCatching { conn?.close() }
        }
    }

    /**
     * STEP 3an: rollback di un restore fallito DOPO l'archiviazione del db
     * corrente (pre-restore presente). Elimina il db ripristinato
     * (corrotto/incompleto) con i suoi sidecar, riporta il db precedente
     * al suo posto e rimuove il pending per evitare un loop di ripristini
     * falliti. Se il pre-restore NON esiste l'archiviazione non era ancora
     * avvenuta: il db corrente e' intatto e il pending resta per il
     * prossimo avvio (es. file temporaneamente bloccato su Windows).
     */
    private fun rollbackPendingRestore(dbFile: File, preRestore: File, parent: File) {
        if (!preRestore.isFile) {
            println("[RiPlay-Backup] nessun rollback necessario: db corrente intatto, ritenta al prossimo avvio")
            return
        }
        runCatching {
            if (dbFile.isFile) dbFile.delete()
            sidecarFiles(parent).forEach { it.delete() }
            File(parent, PENDING_WAL).delete()
            File(parent, PENDING_SHM).delete()
            File(parent, PENDING_DB).delete()
            check(preRestore.renameTo(dbFile)) { "rename di rollback fallita" }
            println("[RiPlay-Backup] rollback OK: db precedente ripristinato, backup scartato")
        }.onFailure { e ->
            println(
                "[RiPlay-Backup] rollback FALLITO: ${e.message} " +
                    "(ripristino manuale: rinominare ${preRestore.absolutePath} su ${dbFile.name} a app spenta)"
            )
        }
    }

    private fun build(): DatabaseInitializer {
        applyPendingRestoreIfNeeded()
        return Room.databaseBuilder<DatabaseInitializer>(name = databaseFilePath().absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(*DatabaseInitializer.MANUAL_MIGRATIONS.toTypedArray())
            .addCallback(DatabaseInitializer.DbCallback)
            .build()
    }
}

actual fun getDatabaseInitializer(): DatabaseInitializer = DesktopDbRegistry.instance

actual fun getDatabaseDao(): Database = DesktopDbRegistry.instance.databaseDao()
