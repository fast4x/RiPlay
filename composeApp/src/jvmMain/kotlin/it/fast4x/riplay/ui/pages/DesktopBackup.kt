package it.fast4x.riplay.ui.pages

import androidx.sqlite.execSQL
import androidx.compose.ui.awt.ComposeWindow
import androidx.room.execSQL
import androidx.room.useWriterConnection
import it.fast4x.riplay.data.DesktopDbRegistry
import it.fast4x.riplay.i18n.ds
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Window
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * STEP 3ae: BACKUP del database desktop (tab Dati delle Impostazioni).
 * Il db Room vive in un unico file (DesktopDbRegistry.databaseFilePath(),
 * struttura IDENTICA a Android: stessi modelli e stesse migrazioni), quindi
 * il backup e' un semplice file dati (formato .db, vedi sotto).
 *
 * ESPORTA: dialog di salvataggio (AWT FileDialog, pattern pickLocalMediaPath
 * di step3m) -> checkpoint WAL -> VACUUM INTO su un file temporaneo (la
 * STESSA tecnica di DatabaseBackupManager.backupDatabase Android: snapshot
 * compatto, completo e autonomo, nessun wal) -> copia sul file scelto.
 * Il risultato e' un SINGOLO FILE .db compatibile AL 100% con il ripristino
 * Android: si puo' esportare dal desktop e ripristinare su Android, e
 * viceversa (richiesta utente step3ah: il flusso Android esporta un .db
 * nudo, non uno zip).
 *
 * IMPORTA: dialog di apertura -> il file viene riconosciuto DAL CONTENUTO:
 *   - .db nudo (backup Android o export desktop step3ah+: header
 *     "SQLite format 3") -> usato direttamente come data.db;
 *   - zip legacy (export desktop fino a v51, contiene data.db) -> estratto
 *     come prima.
 * I file vengono estratti con nome *.restore-pending accanto al db
 * corrente. La sostituzione VERA avviene al PROSSIMO AVVIO (DesktopDbRegistry
 * applyPendingRestoreIfNeeded, prima che Room apra il file): e' l'unico
 * modo cross-platform per sostituire un db in uso, senza lock (Windows) e
 * senza corrompere il WAL. All'utente viene chiesto di riavviare.
 *
 * FIX step3af (crash "Method addObserver must be called on the main thread"
 * alla voce IMPORTA): la prima versione girava interamente dentro
 * withContext(Dispatchers.IO) e creava il parent del dialog con
 * ComposeWindow() SU UN THREAD DI BACKGROUND. Creare una finestra Compose
 * fuori dal main thread registra i suoi observer di lifecycle/finestra sul
 * thread sbagliato e fa crashare l'app (check isMainThread del
 * LifecycleRegistry multiplatform di JetBrains). Ora:
 *  - il FileDialog viene SEMPRE aperto su Dispatchers.Main (regola AWT:
 *    finestre e dialog appartengono all'EDT, che e' anche il main di Compose
 *    Desktop), con parent = la finestra principale dell'app (nessuna
 *    finestra creata al volo);
 *  - solo le operazioni di I/O pesanti (lettura zip, scrittura file
 *    pendenti) girano su Dispatchers.IO.
 * L'EXPORT prima funzionava solo per caso (il dialog girava nel contesto
 * Main del chiamante): ora la garanzia e' dentro le funzioni stesse.
 *
 * FIX step3ag (compilazione): FileDialog NON ha il costruttore
 * (Window, String, int): i costruttori con mode accettano SOLO
 * (Frame, String, int) oppure (Dialog, String, int). parentWindow()
 * dichiarava Window? e la chiamata FileDialog(parent, title, mode)
 * non risolveva. Il ritorno diventa Frame? (ComposeWindow ESTENDE
 * java.awt.Frame, come dimostra UploadDialog.pickLocalMediaPath di
 * step3m che passa una ComposeWindow direttamente a FileDialog e
 * compila dallo step3m).
 */
object DesktopBackup {

    private const val DB_ENTRY = "data.db"

    sealed interface ImportResult {
        data object Cancelled : ImportResult
        data object Scheduled : ImportResult
        data class Invalid(val reason: String) : ImportResult
        data class Failed(val error: String) : ImportResult
    }

    /**
     * FIX step3af: parent del FileDialog = la finestra principale dell'app
     * (trovata tra le finestre AWT vive). Prima veniva creata una
     * ComposeWindow usa-e-getta: se creata fuori dal main thread causava il
     * crash "Method addObserver must be called on the main thread", e anche
     * sul main era un componente inutile (finestra invisibile solo per fare
     * da parent). Se la finestra principale non e' trovata si apre un
     * dialog senza parent (comunque valido).
     *
     * FIX step3ag: tipo di ritorno Frame? (e non Window?): FileDialog
     * accetta solo Frame o Dialog come parent con la firma a 3 argomenti;
     * ComposeWindow estende Frame quindi il cast implicito e' sempre
     * valido e lo smart-cast di Kotlin risolve FileDialog(parent, ...).
     */
    private fun parentWindow(): Frame? =
        runCatching {
            Window.getWindows().filterIsInstance<ComposeWindow>().firstOrNull()
        }.getOrNull()

    /**
     * Apre il FileDialog nativo e ritorna (directory, file) oppure null se
     * l'utente annulla. FIX step3af: DEVE essere chiamata SOLO sul main
     * thread (Dispatchers.Main): e' una regola AWT (finestre/dialog
     * appartengono all'EDT) ed e' la regola che elimina il crash
     * "Method addObserver must be called on the main thread". Il chiamante
     * (exportBackup/importBackup) lo garantisce con withContext(Main).
     */
    private fun pickPath(title: String, mode: Int, suggestedFile: String? = null): Pair<String, String>? {
        val parent = parentWindow()
        val dialog = if (parent != null)
            FileDialog(parent, title, mode)
        else
            FileDialog(null as java.awt.Frame?, title, mode)
        if (suggestedFile != null) dialog.file = suggestedFile
        dialog.isVisible = true // blocca finche' l'utente non chiude il dialog (EDT, pump modale)
        val file = dialog.file ?: return null
        val dir = dialog.directory ?: return null
        return dir to file
    }

    /** Checkpoint WAL: il file principale diventa completo e autonomo. */
    private suspend fun checkpointWal() {
        runCatching {
            withContext(Dispatchers.IO) {
                DesktopDbRegistry.instance.useWriterConnection { conn ->
                    conn.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                }
            }
            println("[RiPlay-Backup] checkpoint WAL eseguito")
        }.onFailure {
            println("[RiPlay-Backup] checkpoint WAL non riuscito (${it.message}): segue zip con i residui")
        }
    }

    /**
     * Esporta il db come SINGOLO FILE .db (formato Android: VACUUM INTO,
     * vedi note di classe). Ritorna il percorso o null (annullato).
     */
    suspend fun exportBackup(): String? {
        val dbFile = DesktopDbRegistry.databaseFilePath()
        checkpointWal()
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        // FIX step3af: il dialog va sul MAIN (vedi pickPath)
        // step3ah: estensione .db (prima era un .zip)
        val pick = withContext(Dispatchers.Main) {
            pickPath(ds("settings.data.export_backup"), FileDialog.SAVE, "RiPlay-backup-$stamp.db")
        } ?: return null
        return try {
            var target = File(pick.first, pick.second)
            if (!target.name.endsWith(".db")) target = File(target.parentFile, target.name + ".db")

            withContext(Dispatchers.IO) {
                // VACUUM INTO (come Android DatabaseBackupManager.backupDatabase):
                // snapshot compatto e autonomo. SQLite richiede che il file di
                // destinazione NON esista: si passa da un temp e si copia dopo
                // (sovrascrive il file scelto dall'utente se gia' presente).
                val tmp = File.createTempFile("riplay-vacuum", ".db")
                runCatching {
                    tmp.delete() // VACUUM INTO fallisce se il file esiste gia'
                    DesktopDbRegistry.instance.useWriterConnection { conn ->
                        conn.execSQL("VACUUM INTO '${tmp.absolutePath.replace("'", "''")}'")
                    }
                }.recoverCatching {
                    // fallback (sqlite senza VACUUM INTO?): copia diretta del
                    // file principale, gia' completo dopo il checkpoint TRUNCATE
                    dbFile.copyTo(tmp, overwrite = true)
                }
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            println("[RiPlay-Backup] esportato in ${target.absolutePath}")
            target.absolutePath
        } catch (e: Exception) {
            println("[RiPlay-Backup] export FALLITO: ${e.message}")
            throw e
        }
    }

    /**
     * Importa un backup (.db Android/desktop o zip legacy desktop):
     * estrazione a ripristino PENDENTE (valido al prossimo avvio). Vedi
     * note di classe.
     *
     * FIX step3af: il dialog gira su Dispatchers.Main (prima era TUTTO
     * dentro withContext(Dispatchers.IO): la creazione del parent
     * ComposeWindow fuori dal main thread causava il crash
     * "Method addObserver must be called on the main thread"). L'estrazione
     * resta su Dispatchers.IO.
     */
    suspend fun importBackup(): ImportResult {
        val pick = withContext(Dispatchers.Main) {
            pickPath(ds("settings.data.import_backup"), FileDialog.LOAD)
        } ?: return ImportResult.Cancelled
        val source = File(pick.first, pick.second)
        return withContext(Dispatchers.IO) { extractToPending(source) }
    }

    /**
     * Estrazione del backup a file di ripristino PENDENTE (nessun tocco al
     * db corrente in uso: vedi note di classe). Solo I/O, nessun componente
     * UI: puo' girare su Dispatchers.IO.
     *
     * step3ah: il formato viene riconosciuto DAL CONTENUTO, non dall'estensione:
     *   - file che inizia con "SQLite format 3" = db NUDO (backup Android o
     *     nuovo export desktop): usato direttamente come data.db;
     *   - file che inizia con "PK" = zip legacy desktop (fino a v51):
     *     si estrae data.db (-wal/-shm se presenti).
     */
    private fun extractToPending(source: File): ImportResult {
        val dbFile = DesktopDbRegistry.databaseFilePath()
        try {
            if (!source.isFile)
                return ImportResult.Invalid(ds("settings.data.import_invalid"))

            val head = source.inputStream().use { it.readNBytes(16) }
            val isZip = head.size >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
            val isRawDb = head.size >= 15 && String(head.copyOf(15)).startsWith("SQLite format")

            var dbBytes: ByteArray? = null
            var walBytes: ByteArray? = null
            var shmBytes: ByteArray? = null

            if (isZip) {
                ZipInputStream(source.inputStream().buffered()).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val bytes = zip.readBytes()
                            when (entry.name.substringAfterLast('/')) {
                                DB_ENTRY -> dbBytes = bytes
                                "$DB_ENTRY-wal" -> walBytes = bytes
                                "$DB_ENTRY-shm" -> shmBytes = bytes
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
            } else if (isRawDb) {
                // backup Android o export desktop step3ah: il file E' il db
                dbBytes = source.readBytes()
            } else {
                println("[RiPlay-Backup] import: formato non riconosciuto (${source.name})")
                return ImportResult.Invalid(ds("settings.data.import_invalid"))
            }

            val db = dbBytes ?: run {
                println("[RiPlay-Backup] import: zip senza $DB_ENTRY (${source.name})")
                return ImportResult.Invalid(ds("settings.data.import_invalid"))
            }
            // un db SQLite valido inizia con "SQLite format 3"
            if (db.size < 4096 || !String(db.copyOf(15)).startsWith("SQLite format")) {
                return ImportResult.Invalid(ds("settings.data.import_invalid"))
            }

            // scrivi i file PENDENTI (nessun tocco al db corrente in uso)
            File(dbFile.parentFile, DesktopDbRegistry.PENDING_DB).writeBytes(db)
            if (walBytes != null)
                File(dbFile.parentFile, DesktopDbRegistry.PENDING_WAL).writeBytes(walBytes!!)
            else
                File(dbFile.parentFile, DesktopDbRegistry.PENDING_WAL).delete()
            if (shmBytes != null)
                File(dbFile.parentFile, DesktopDbRegistry.PENDING_SHM).writeBytes(shmBytes!!)
            else
                File(dbFile.parentFile, DesktopDbRegistry.PENDING_SHM).delete()

            println("[RiPlay-Backup] import: ripristino programmato da ${source.name} (${db.size} byte)")
            return ImportResult.Scheduled
        } catch (e: Exception) {
            println("[RiPlay-Backup] import FALLITO: ${e.message}")
            return ImportResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Percorso del db per la UI (info nella tab Dati). */
    fun dbPathLabel(): String = DesktopDbRegistry.databaseFilePath().absolutePath
}
