package it.fast4x.riplay.extensions.webdav

import androidx.room.execSQL
import androidx.room.useWriterConnection
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.DesktopDbRegistry
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.ui.pages.DesktopBackup
import it.fast4x.riplay.utils.DesktopCryptoManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * PORTING DESKTOP di WebDavLibraryViewModel.kt (androidApp): gestore degli
 * account WebDAV per la tab Account (senza ViewModel Android: e' un
 * singleton con StateFlow, i flussi Room alimentano la UI).
 *
 * Funzionalita' portate su desktop:
 *  - elenco account dal database Room (stessa tabella webdav_account di
 *    Android: un backup Android importato su desktop conserva gli account);
 *  - salvataggio/modifica/cancellazione (password cifrata con
 *    DesktopCryptoManager, AES/GCM compatibile);
 *  - TEST CONNESSIONE (PROPFIND sulla cartella remota, stesse mappature
 *    errore dell'originale);
 *  - BACKUP del database: esportazione come Android (VACUUM INTO,
 *    via DesktopBackup), zip e upload diretto su WebDAV in
 *    RiPlayBackup/riplay_sync.db.zip + riplay_meta.json;
 *  - RESTORE dal cloud: download dello zip, estrazione e messa a
 *    ripristino PENDENTE (stesso meccanismo dell'import della tab Dati:
 *    si applica al PROSSIMO avvio, con copia di sicurezza del db corrente).
 *
 * Scansioni della libreria musicale (loadAllMusicFolders Android): il
 * flusso e' implementato in scanMusicFolders(), che upserta i brani WebDAV
 * nel database; il PLAYER desktop li riproduce via VLCJ con Basic auth
 * (DesktopVideoSource riconosce il prefisso webdav:).
 */
object DesktopWebDavManager {

    val repository = DesktopWebDavRepository()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("RiPlay-WebDav"))

    // ── STATI osservabili (equivalenti del ViewModel Android) ────────────────

    private val _accounts = MutableStateFlow<List<WebDavAccount>>(emptyList())
    val accounts = _accounts.asStateFlow()

    sealed class TestConnectionState {
        data object Idle : TestConnectionState()
        data object Testing : TestConnectionState()
        data class Success(val message: String) : TestConnectionState()
        data class Error(val message: String) : TestConnectionState()
    }

    private val _testConnectionState =
        MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testConnectionState = _testConnectionState.asStateFlow()

    /** Eventi di stato della sincronizzazione musica (per la UI della tab). */
    sealed class SyncState {
        data object Idle : SyncState()
        data object Loading : SyncState()
        data class Success(val folders: Int, val songs: Int) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _backupState = MutableStateFlow<String?>(null)
    val backupState = _backupState.asStateFlow()

    /** Messaggi one-shot per la UI (testo + true se errore). */
    private val _messages = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    init {
        refreshAccounts()
    }

    // ── CRUD ACCOUNT ────────────────────────────────────────────────────────

    fun refreshAccounts() {
        scope.launch {
            try {
                _accounts.value = Database.webDavAccountDao().getAll()
                println("[RiPlay-WebDAV] account caricati: ${_accounts.value.size}")
            } catch (t: Throwable) {
                println("[RiPlay-WebDAV] errore caricamento account: ${t.message}")
            }
        }
    }

    /** Salva (insert se nuovo, update se esistente) come saveAccount Android. */
    fun saveAccount(account: WebDavAccount) {
        scope.launch {
            try {
                val accountToSave = account.copy(
                    baseUrl = account.baseUrl.trimEnd('/'),
                    remoteFolder = account.remoteFolder.ifBlank { "/" },
                )
                val dao = Database.webDavAccountDao()
                if (accountToSave.id == 0L) {
                    val newId = dao.insert(accountToSave)
                    if (newId == -1L) {
                        println("[RiPlay-WebDAV] inserimento bloccato: account gia' esistente")
                        _messages.emit("WebDAV: account already exists (same server, user and folder)" to true)
                    } else {
                        println("[RiPlay-WebDAV] nuovo account inserito (id=$newId)")
                    }
                } else {
                    dao.update(accountToSave)
                    println("[RiPlay-WebDAV] account modificato (id=${accountToSave.id})")
                }
                refreshAccounts()
            } catch (t: Throwable) {
                println("[RiPlay-WebDAV] errore salvataggio account: ${t.message}")
                _messages.emit("WebDAV save error: ${t.message}" to true)
            }
        }
    }

    /** Cancella l'account e TUTTI i dati relativi (stessa cascata di Android). */
    fun deleteAccount(account: WebDavAccount) {
        scope.launch {
            try {
                Database.webDavAccountDao().deleteAccountAndAllData(account)
                refreshAccounts()
                _messages.emit("WebDAV: account deleted" to false)
            } catch (t: Throwable) {
                println("[RiPlay-WebDAV] errore cancellazione account: ${t.message}")
                _messages.emit("WebDAV delete error: ${t.message}" to true)
            }
        }
    }

    fun resetTestState() {
        _testConnectionState.value = TestConnectionState.Idle
    }

    // ── TEST CONNESSIONE ────────────────────────────────────────────────────

    /**
     * Test di connessione (stesse regole di WebDavLibraryViewModel.testConnection):
     * campi obbligatori, al massimo UN account di backup, PROPFIND sulla
     * cartella; errori mappati con gli stessi criteri (401/404/rete).
     */
    fun testConnection(account: WebDavAccount) {
        if (account.baseUrl.isBlank() || account.username.isBlank() || account.encryptedPassword.isBlank()) {
            _testConnectionState.value = TestConnectionState.Error(
                ds("settings.webdav.test.fill_fields")
            )
            return
        }

        scope.launch {
            val backupExists = _accounts.value.any { !it.isMusicSource && it.id != account.id }
            if (backupExists && !account.isMusicSource) {
                _testConnectionState.value = TestConnectionState.Error(
                    ds("settings.webdav.test.backup_exists")
                )
                return@launch
            }

            _testConnectionState.value = TestConnectionState.Testing
            try {
                repository.listDirectory(account)
                _testConnectionState.value = TestConnectionState.Success(
                    ds("settings.webdav.test.success")
                )
            } catch (e: IOException) {
                val message = when {
                    e.message?.contains("401") == true -> ds("settings.webdav.test.auth_error")
                    e.message?.contains("404") == true -> ds("settings.webdav.test.not_found")
                    e.message?.contains("Host") == true || e.message?.contains("Unable") == true ->
                        ds("settings.webdav.test.unreachable")
                    else -> ds(
                        "settings.webdav.test.network_error", "error" to (e.message ?: "")
                    )
                }
                _testConnectionState.value = TestConnectionState.Error(message)
            } catch (e: Exception) {
                _testConnectionState.value = TestConnectionState.Error(
                    ds("settings.webdav.test.unexpected_error", "error" to (e.message ?: ""))
                )
            }
        }
    }

    // ── SCANSIONE MUSICA (link musicale, come loadAllMusicFolders) ──────────

    /** Scansiona TUTTI gli account sorgente musicale e upserta i brani nel db. */
    fun scanMusicFolders() {
        scope.launch {
            _syncState.value = SyncState.Loading
            try {
                var totalFolders = 0
                var totalSongs = 0
                val sources = _accounts.value.filter { it.isMusicSource }
                if (sources.isEmpty()) {
                    _syncState.value = SyncState.Error(
                        ds("settings.webdav.sync.no_music_accounts")
                    )
                    return@launch
                }
                for (account in sources) {
                    val rawItems = if (account.scanSubFolders)
                        repository.listDirectoryRecursive(account)
                    else repository.listDirectory(account)
                    val folderItems = rawItems.drop(1)
                    val songs = folderItems.toSongs(account.baseUrl).distinctBy { it.id }
                    val folders = folderItems.filter { it.isDirectory }.distinctBy { it.href }
                    withContext(Dispatchers.IO) {
                        songs.forEach { song ->
                            // FASE 1 Android: upsert grezzo se nuovo, altrimenti
                            // conserva i dati reali gia' presenti nel db
                            val songInDb = song.mediaId?.let { Database.songDao().getById(it) }
                            val existing = Database.getSong(song.id)
                            if (songInDb != null || existing != null) { /* dati reali conservati */ }
                            else Database.upsert(song)
                        }
                    }
                    totalFolders += folders.size
                    totalSongs += songs.size
                    println("[RiPlay-WebDAV] scan '${account.name}': ${songs.size} brani, ${folders.size} cartelle")
                }
                _syncState.value = SyncState.Success(totalFolders, totalSongs)
                _messages.emit(
                    ds(
                        "settings.webdav.sync.done", "songs" to totalSongs, "folders" to totalFolders
                    ) to false
                )
            } catch (t: Throwable) {
                println("[RiPlay-WebDAV] errore scansione: ${t.message}")
                _syncState.value = SyncState.Error(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    // ── BACKUP DATABASE SU WEBDAV (come WebDavDatabaseSyncBackupWorker) ─────

    /**
     * Esegue il backup del db e lo carica sull'account di backup.
     * @return null se avviato (esito via backupState), oppure il messaggio d'errore
     */
    fun backupDatabaseToWebDav(account: WebDavAccount) {
        scope.launch {
            try {
                val password = DesktopCryptoManager.decrypt(account.encryptedPassword)
                val config = WebDavConfig(account.baseUrl, account.username, password)
                if (config.baseUrl.isEmpty()) {
                    _backupState.value = "WebDAV: empty server URL"
                    return@launch
                }
                val remoteFolder = if (account.remoteFolder != "/") account.remoteFolder
                else WEBDAV_DEFAULT_BACKUP_FOLDER

                _backupState.value = "backup: exporting database..."
                val dbFile = exportDbToTemp()

                // 1. riplay_meta.json (timestamp + nome dispositivo, come worker Android)
                _backupState.value = "backup: uploading metadata..."
                val info = WebDavBackupInfo(
                    timestamp = System.currentTimeMillis(),
                    deviceName = deviceName()
                )
                val metaFile = File.createTempFile("riplay_meta", ".json")
                metaFile.writeText(DesktopWebDavJson.encodeToString(info))
                repository.uploadFileDirect(config, remoteFolder, metaFile)
                metaFile.delete()

                // 2. zip del db (riduce dimensioni come su Android)
                _backupState.value = "backup: compressing database..."
                val zipped = File.createTempFile("riplay_sync", ".zip")
                zipFile(dbFile, zipped)
                println("[RiPlay-WebDAV] db compresso: ${dbFile.length()} -> ${zipped.length()} bytes")

                // 3. upload dello zip
                _backupState.value = "backup: uploading database..."
                repository.uploadFileDirect(config, remoteFolder, zipped)
                zipped.delete()
                dbFile.delete()

                _backupState.value = null
                _messages.emit(
                    ds("settings.webdav.backup.done", "folder" to remoteFolder) to false
                )
                println("[RiPlay-WebDAV] backup completato su $remoteFolder")
            } catch (t: Throwable) {
                println("[RiPlay-WebDAV] backup FALLITO: ${t.message}")
                _backupState.value = null
                _messages.emit("WebDAV backup error: ${t.message}" to true)
            }
        }
    }

    // ── RESTORE DA WEBDAV (come syncDatabaseFromWebDav, forma desktop) ──────

    /**
     * Scarica riplay_sync.db.zip da RiPlayBackup e lo mette a ripristino
     * PENDENTE (si applica al prossimo avvio, stesso meccanismo della tab
     * Dati: il db corrente viene archiviato in data.db.pre-restore).
     * @return null = restore programmato; stringa = messaggio d'errore/feedback
     */
    suspend fun restoreDatabaseFromWebDav(account: WebDavAccount): String? {
        return try {
            val password = DesktopCryptoManager.decrypt(account.encryptedPassword)
            val config = WebDavConfig(account.baseUrl, account.username, password)
            val remoteZipPath = "$WEBDAV_DEFAULT_BACKUP_FOLDER/$WEBDAV_DEFAULT_BACKUP_DB_ZIPPED_FILE"

            val tempZip = File.createTempFile("riplay_restore", ".zip")
            repository.downloadFile(config, remoteZipPath, tempZip)
            println("[RiPlay-WebDAV] scaricato ${tempZip.length()} bytes da $remoteZipPath")

            // Estrai il db nudo dallo zip (prima entry .db)
            val tempDb = File.createTempFile("riplay_restore", ".db")
            ZipInputStream(tempZip.inputStream().buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                var found = false
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.substringAfterLast('/').endsWith(".db")) {
                        FileOutputStream(tempDb).use { zip.copyTo(it) }
                        found = true
                        break
                    }
                    entry = zip.nextEntry
                }
                if (!found) throw IOException("no .db file inside ${WEBDAV_DEFAULT_BACKUP_DB_ZIPPED_FILE}")
            }
            tempZip.delete()

            // Ripristino PENDENTE: stesso percorso della tab Dati (DesktopBackup)
            val result = DesktopBackup.scheduleRestoreFromExternalFile(tempDb)
            tempDb.delete()
            result
        } catch (t: Throwable) {
            println("[RiPlay-WebDAV] restore FALLITO: ${t.message}")
            "WebDAV restore error: ${t.message}"
        }
    }

    /** Info del backup remoto (per il dialog di conferma del restore). */
    suspend fun fetchBackupInfo(account: WebDavAccount): WebDavBackupInfo? {
        return try {
            val password = DesktopCryptoManager.decrypt(account.encryptedPassword)
            val config = WebDavConfig(account.baseUrl, account.username, password)
            repository.fetchBackupInfo(config)
        } catch (t: Throwable) {
            null
        }
    }

    // ── UTIL ────────────────────────────────────────────────────────────────

    /** Decripta la password di un account (lettura per l'eventuale player). */
    fun decryptedPassword(account: WebDavAccount): String =
        DesktopCryptoManager.decrypt(account.encryptedPassword)

    /** Mappa baseUrl -> account per il routing del player (webdav:). */
    fun accountForBaseUrl(baseUrl: String): WebDavAccount? =
        _accounts.value.firstOrNull { baseUrl.startsWith(it.baseUrl.trimEnd('/')) }

    /** Esporta il db corrente in un file temporaneo (VACUUM INTO come Android). */
    private suspend fun exportDbToTemp(): File {
        val dbFile = DesktopDbRegistry.databaseFilePath()
        runCatching {
            DesktopDbRegistry.instance.useWriterConnection { conn ->
                conn.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            }
        }.onFailure { println("[RiPlay-WebDAV] checkpoint WAL non riuscito: ${it.message}") }
        val tmp = File.createTempFile("riplay-sync", ".db")
        runCatching {
            tmp.delete()
            DesktopDbRegistry.instance.useWriterConnection { conn ->
                conn.execSQL("VACUUM INTO '${tmp.absolutePath.replace("'", "''")}'")
            }
        }.recoverCatching {
            dbFile.copyTo(tmp, overwrite = true)
        }
        return tmp
    }

    private fun zipFile(source: File, target: File) {
        ZipOutputStream(FileOutputStream(target).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(source.name))
            source.inputStream().buffered().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    /** Nome dispositivo (hostname), equivalente desktop del deviceInfo Android. */
    private fun deviceName(): String = runCatching {
        InetAddress.getLocalHost().hostName ?: "Desktop"
    }.getOrDefault("Desktop")
}
