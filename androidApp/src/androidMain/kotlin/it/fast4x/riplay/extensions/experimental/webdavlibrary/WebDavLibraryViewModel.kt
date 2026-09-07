package it.fast4x.riplay.extensions.experimental.webdavlibrary

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import it.fast4x.environment.models.PlayerResponse
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.enums.RestoreMode
import it.fast4x.riplay.extensions.databasebackup.DatabaseBackupManager
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavBackupInfo
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavBrowserState
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavSongMetadata
import it.fast4x.riplay.extensions.players.getOnlineMetadata
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.utils.CryptoManager
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import it.fast4x.riplay.utils.ZipManager
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.formatAsDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

const val WEBDAV_DEFAULT_BACKUP_FOLDER = "RiPlayBackup"
const val WEBDAV_DEFAULT_BACKUP_DB_FILE = "riplay_sync.db"
const val WEBDAV_DEFAULT_BACKUP_DB_ZIPPED_FILE = "riplay_sync.db.zip"
const val WEBDAV_DEFAULT_BACKUP_METADATA_FILE = "riplay_meta.json"
class WebDavLibraryViewModel () : ViewModel(), ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebDavLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WebDavLibraryViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    val context = (appContext() as MainApplication)

    // Limite di 3 richieste contemporanee a YouTube
    private val metadataSemaphore = Semaphore(3)
    val webDavLibraryRepository = WebDavLibraryRepository()

    val appSettingsManager = context.appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value

    private val _uiState = MutableStateFlow<WebDavBrowserState>(WebDavBrowserState.Idle)
    val uiState: StateFlow<WebDavBrowserState> = _uiState.asStateFlow()

    private val _accounts = MutableStateFlow<List<WebDavAccount>>(emptyList())
    val accounts: StateFlow<List<WebDavAccount>> = _accounts.asStateFlow()

    sealed class TestConnectionState {
        object Idle : TestConnectionState() // Stato iniziale
        object Testing : TestConnectionState() // Loader visibile
        data class Success(val message: String) : TestConnectionState() // Messaggio verde
        data class Error(val message: String) : TestConnectionState() // Messaggio rosso
    }

    private val _testConnectionState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testConnectionState: StateFlow<TestConnectionState> = _testConnectionState.asStateFlow()

    init { refreshAccounts() }

    override fun onCleared() {
        super.onCleared()
        Timber.d("WebDavLibraryViewModel: onCleared() chiamato! Il ViewModel è stato distrutto.")
    }

    // Gestione degli accounts
    fun refreshAccounts() {
        viewModelScope.launch {
            _accounts.value = Database.webDavAccountDao().getAll()
        }
    }

    fun saveAccount(
        account: WebDavAccount
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                // Creazione dell'oggetto pulito
                val accountToSave = account.copy(
                    baseUrl = account.baseUrl.trimEnd('/'), // Pulizia fondamentale per il matching URL di ExoPlayer!
                    remoteFolder = account.remoteFolder.ifBlank { "/" },
                )

                //  Salvataggio nel DB
                val dao = Database.webDavAccountDao()
                if (account.id == 0L) {
                    // NUOVO ACCOUNT
                    val newId = dao.insert(accountToSave)
                    if (newId == -1L) {
                        // L'insert è fallito perché esiste già (IGNORE ha bloccato la cosa)
                        Timber.w("WebDavViewModel saveAccount Tentativo di inserire un account WebDAV già esistente")
                    } else {
                        Timber.d("WebDavViewModel saveAccountNuovo account WebDAV inserito nel DB con ID = $newId")
                    }
                } else {
                    // MODIFICA ACCOUNT ESISTENTE
                    dao.update(accountToSave)
                    Timber.w("WebDavViewModel saveAccount account WebDAV modificato nel DB")
                }

                // Aggiorno la lista interna degli account
                refreshAccounts()

                // AGGIORNAMENTO DEL PROVIDER IN RAM
                val allAccounts = dao.getAll()
                WebDavCredentialsProvider.updateCredentials(allAccounts)

                // Resetta lo stato del test
                _testConnectionState.value = TestConnectionState.Idle

                // Se è una sorgente musicale nuova, potrei triggerare qui
                // la scansione PROPFIND per popolare la libreria.

            } catch (e: Exception) {
                Timber.e(e, "WebDavLibraryViewModel Errore durante il salvataggio dell'account WebDAV")
            }
        }
    }

    fun deleteAccount1(account: WebDavAccount) {
        viewModelScope.launch {
            Database.webDavAccountDao().delete(account)
            refreshAccounts()
        }
    }


    fun deleteAccount(account: WebDavAccount) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.webDavAccountDao().delete(account)

            // Aggiorniamo le credenziali in RAM rimuovendo questo account
            WebDavCredentialsProvider.updateCredentials(Database.webDavAccountDao().getAll())

            // Se l'account eliminato era quello usato per il backup, lo disattiviamo
            val settings = appSettingsManager.activeSettings.value
            if (settings.backupWebDavAccountId == account.id) {
                appSettingsManager.updateSettings(settings.copy(backupWebDavAccountId = null))
            }

            // (Opzionale) Cancella dal DB locale tutte le Song che avevano l'URL di questo account
            // Database.songDao().deleteByBaseUrl(account.baseUrl)
        }
    }

    fun getAccount(id: Long): WebDavAccount? {
        refreshAccounts()
        return _accounts.value.find { it.id == id }
    }

    // Gestione dei test pre inserimento nel database
    fun testConnection(account: WebDavAccount) {
        // Se i campi base sono vuoti, non facciamo nemmeno la richiesta
        if (account.baseUrl.isBlank() || account.username.isBlank() || account.encryptedPassword.isBlank()) {
            _testConnectionState.value = TestConnectionState.Error(context.getString(R.string.webdav_form_testing_fill_in_all_the_fields))
            return
        }

        viewModelScope.launch {

            // Controlliamo se eiste già un account di backup (Non ha senso gestire la sincronizzazione del db tra dispositivi con più account)
            // Controlliamo oure se è lo stesso account che ad esempio si sta modificando, in questo caso il controllo non deve scattare
            val ifBackupAccountExists = _accounts.value.any { !it.isMusicSource && it.id != account.id }
            if (ifBackupAccountExists && !account.isMusicSource) {
                _testConnectionState.value = TestConnectionState.Error(context.getString(R.string.webdav_form_testing_backup_exists))
                return@launch
            }

            _testConnectionState.value = TestConnectionState.Testing
            try {
                // Chiamiamo il repository. Usiamo la listDirectory sulla root (cartella vuota)
                // Se il server risponde con 200/207, le credenziali e l'URL sono giusti!
                webDavLibraryRepository.listMusicDirectory(account)

                _testConnectionState.value = TestConnectionState.Success(context.getString(R.string.webdav_form_testing_success))

            } catch (e: IOException) {
                // OkHttp o il server hanno rifiutato la connessione
                val errorMessage = when {
                    e.message?.contains("401") == true -> context.getString(R.string.webdav_form_testing_auth_error)
                    e.message?.contains("404") == true -> context.getString(R.string.webdav_form_testing_not_found)
                    e.message?.contains("Host") == true || e.message?.contains("Unable") == true -> context.getString(R.string.webdav_form_testing_unreachable)
                    else -> context.getString(R.string.webdav_form_testing_network_error, e.message)
                }
                _testConnectionState.value = TestConnectionState.Error(errorMessage)
            } catch (e: Exception) {
                _testConnectionState.value = TestConnectionState.Error(context.getString(R.string.webdav_form_testing_unexpected_error, e.message))
            }
        }
    }

    // Da chiamare quando l'account viene salvato o modificato
    fun resetTestState() {
        _testConnectionState.value = TestConnectionState.Idle
    }

    // Gestione della sincronizzazione

    fun loadAllMusicFolders() {
        viewModelScope.launch {
            _uiState.value = WebDavBrowserState.Loading
            _accounts.value.filter { it.isMusicSource }.forEach { account ->
                loadMusicFolder(account, account.remoteFolder)
            }
        }
    }

    fun loadMusicFolder(account: WebDavAccount, folderPath: String) {
        viewModelScope.launch {
            _uiState.value = WebDavBrowserState.Loading
            try {
                val rawItems =
                    if (appSettings.isWebDavScanSubfoldersEnabled)
                        webDavLibraryRepository.listMusicDirectoryRecursive(account)
                    else webDavLibraryRepository.listMusicDirectory(account)
                // Rimuove il primo elemento se è la cartella stessa che stiamo navigando
                val folderItems = rawItems.drop(1)

                val songs = folderItems.toSongs(account.baseUrl).distinctBy { it.id }
                val folders = folderItems.filter { it.isDirectory }.distinctBy { it.href }

                withContext(Dispatchers.IO) {
                    // FASE 1: Sincronizzazione immediata con il DB
                    val updatedSongs = songs.map { song ->
                        Timber.d("WebDavLibraryViewModel upserting song = $song")
                        val mId = song.mediaId

                        val songInDb = mId?.let { Database.songDao().getById(it) }
                        Timber.d("WebDavLibraryViewModel upserting songInDb = $songInDb")
                        if (songInDb != null) {
                            // Aggiorna l'oggetto in memoria con i dati reali del DB
                            song.copy(
                                title = songInDb.title,
                                artistsText = songInDb.artistsText,
                                durationText = songInDb.durationText,
                                thumbnailUrl = songInDb.thumbnailUrl,
                            )
                        } else {
                            // Brano nuovo! Salviamo subito la base nel DB con i dati grezzi
                            Database.upsert(song)
                            song // Ritorna la song con i dati grezzi/estimati
                        }
                    }

                    Timber.d("WebDavLibraryViewModel upserting songs = ${updatedSongs.size}")

                    // Aggiorna la UI con i dati grezzi o del DB
                    _uiState.value = WebDavBrowserState.Success(folders = folders, songs = songs)

                    // FASE 2: Recupero asincrono dei metadati online per i brani nuovi
                    updatedSongs
                        .filter { it.id.isNotBlank() }
                        .forEach { songToFetch ->
                            Timber.d("WebDavLibraryViewModel upserting songToFetch = $songToFetch")
                            // Lancio una coroutine per ogni brano da recuperare
                            // Il Semaphore dentro fetchMetadataIfNeeded eviterà il ban IP permettendo solo 3 richieste contemporanee
                            launch {
                                val config = WebDavConfig(
                                    baseUrl = account.baseUrl,
                                    username = account.username,
                                    password = CryptoManager.decrypt(account.encryptedPassword),
                                )
                                fetchMetadataIfNeeded(config, songToFetch)
                            }
                        }
                }
                Timber.d("WebDavLibraryViewModel Success loading folder")

            } catch (e: Exception) {
                _uiState.value = WebDavBrowserState.Error(e.message ?: context.getString(R.string.error_unknown))
                Timber.e("WebDavLibraryViewModel Error loading folder: ${e.message}")

            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun fetchMetadataIfNeeded(config: WebDavConfig, song: Song) {

        viewModelScope.launch {
            if (song.mediaId.isNullOrBlank()
                || Database.format(song.id).first() != null
                ) return@launch

            // Il Semaphore blocca la coroutine qui se ci sono già 3 richieste in corso
            metadataSemaphore.withPermit {
                var fetchedMetadata: WebDavSongMetadata? = null
                var response: PlayerResponse? = null
                val songRemoteUrl = song.id.substringAfter(WEBDAV_KEY_PREFIX)

                // STEP 1: Estrazione dal File Remoto
                try {
                    Timber.d("WebDavLibraryViewModel fetchMetadataIfNeeded Metadata: Provo ad estrarre i metadata dal file temporaneo ${song.title}")
                    fetchedMetadata = webDavLibraryRepository.fetchMetadataFromRemoteFile(config, songRemoteUrl)
                    Timber.d("WebDavLibraryViewModel fetchMetadataIfNeeded Metadata da file: $fetchedMetadata")
                } catch (e: Exception) {
                    Timber.e(e, "WebDavLibraryViewModel fetchMetadataIfNeeded Metadata: Errore nel recuperare i metadati dal file ${song.title}")
                }

                // STEP 2: Fallback Online (YouTube)
                if (fetchedMetadata == null && song.mediaId.isNotBlank()) {
                    try {
                        Timber.d("WebDavLibraryViewModel fetchMetadataIfNeeded Metadata: Provo ad estrarre i metadata da YouTube")
                        response = getOnlineMetadata(videoId = song.mediaId)
                        val videoDetails = response?.videoDetails

                        if (videoDetails != null) {
                            fetchedMetadata = WebDavSongMetadata(
                                title = videoDetails.title.toString(),
                                artist = videoDetails.author.toString(),
                                durationMs = videoDetails.lengthSeconds?.toLongOrNull()?.times(1000)
                                    ?: -1,
                                thumbnailUrl = videoDetails.thumbnail?.thumbnails?.maxByOrNull {
                                    it.width ?: 0
                                }?.url
                            )
                            Timber.d("WebDavLibraryViewModel fetchMetadataIfNeeded Metadata da YouTube: $fetchedMetadata")

                        } else {
                            Timber.d("WebDavLibraryViewModel fetchMetadataIfNeeded no videoDetails for ${song.mediaId}")
                        }

                    } catch (e: Exception) {
                        Timber.e(
                            e,
                            "WebDavLibraryViewModel fetchMetadataIfNeeded Metadata fetch failed for ${song.mediaId}"
                        )
                    }

                }

                // Aggiorno song ed i relativi metadati nel DB
                fetchedMetadata?.let { metadata ->
                    withContext(Dispatchers.IO) {
                        val updatedSong = song.copy(
                            title = metadata.title,
                            artistsText = metadata.artist,
                            durationText = if (metadata.durationMs <= 0) song.durationText else formatAsDuration(metadata.durationMs),
                            thumbnailUrl = metadata.thumbnailUrl
                        )

                        Database.upsert(updatedSong)

                        response?.let { resp ->
                            try {
                                Database.insert(
                                    Format(
                                        songId = song.id,
                                        contentLength = resp.videoDetails?.lengthSeconds?.toLong(),
                                        loudnessDb = resp.playerConfig?.audioConfig?.loudnessDb
                                            ?: resp.playerConfig?.audioConfig?.perceptualLoudnessDb?.toFloat(),
                                        playbackUrl = resp.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                                    )
                                )
                            } catch (e: Exception) {
                                Timber.e("WebDavLibraryViewModel fetchMetadataIfNeeded exception ${e.message}")
                            }
                        }
                    }
                }

            }
        }
    }

    fun syncDatabaseToWebDav(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi (risparmio dati)
            .setRequiresBatteryNotLow(true) // Non farlo se la batteria è al 5%
            .build()

        val backupRequest = OneTimeWorkRequestBuilder<WebDavDatabaseSyncBackupWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "riplay_webdav_db_backup",
                ExistingWorkPolicy.REPLACE, // Se ce n'è già uno, lo sostituisco
                backupRequest
            )
    }

    /**
     * Scarica il DB dal server e lo ripristina nel database locale, chiude il DB
     * e chiede di riavviare l'app.
     */
    @androidx.annotation.OptIn(UnstableApi::class)
    suspend fun syncDatabaseFromWebDav(context: Context, account: WebDavAccount) {
        val repository = WebDavLibraryRepository()
        val remoteZipDbPath = "${WEBDAV_DEFAULT_BACKUP_FOLDER }/$WEBDAV_DEFAULT_BACKUP_DB_ZIPPED_FILE"
        val backupManager = DatabaseBackupManager(context, Database)

        val config = WebDavConfig(
            baseUrl = account.baseUrl,
            username = account.username,
            password = CryptoManager.decrypt(account.encryptedPassword),
        )

        ////////////////////////////
        // utile per un backup o copia di files basato sulla data di ultima modifica
        // Recupera la data remota
//        val remoteLastModified = repository.getRemoteFileLastModified(config, remoteDbPath)
//        if (remoteLastModified == 0L) {
//            Timber.d("WebDavLibraryViewModel Nessun backup remoto trovato.")
//            return
//        }

        // Recupera la data locale
//        val localDbFile = context.getDatabasePath(Database.getDatabaseName)
//        val localLastModified = if (localDbFile.exists()) localDbFile.lastModified() else 0L
//        Timber.d("WebDavLibraryViewModel databaseName = ${Database.getDatabaseName} Data locale = $localLastModified Data remota = $remoteLastModified")


        // Confronto
        //if (remoteLastModified > localLastModified) {
        //////////////////////////////

        Timber.d("WebDavLibraryViewModel Procedo al download.")

        // File database zippato temporaneo in cache
        val tempZipFile = File(context.cacheDir, "restore_tmp.zip")

        // Download
        try {
            repository.downloadFile(config, remoteZipDbPath, tempZipFile)
        } catch (e: Exception) {
            Timber.e(e, "WebDavLibraryViewModel Restore: Errore durante il download di $remoteZipDbPath")
            return
        }


        // Decomprimi lo ZIP
        val tempDbFile = File(context.cacheDir, "restore_tmp.db")
        try {
            ZipManager.unzip(tempZipFile, tempDbFile)
            Timber.d("WebDavLibraryViewModel Restore: DB decompresso con successo!")
        } catch (e: Exception) {
            Timber.e(e, "WebDavLibraryViewModel Restore: Errore durante la decompressione")
            tempZipFile.delete()
            return
        }

        // Stop preventivo del servizio, smartrestore gestirà tutto il necessario
        context.stopService(Intent(context, PlayerService::class.java))

        try {
            backupManager.smartRestoredatabase(tempDbFile.toUri(), RestoreMode.REPLACE)
            delay(1500.milliseconds)
            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
            val restartIntent = Intent.makeRestartActivityTask(launchIntent?.component)
            context.startActivity(restartIntent)
            Runtime.getRuntime().exit(0)
        } catch (e: IOException) {
            Timber.e("WebDavLibraryViewModel Restore error: ${e.message}")
        } catch (e: Exception) {
            Timber.e("WebDavLibraryViewModel Unknown restore error: ${e.message}")
        }

        Timber.d("WebDavLibraryViewModel Database ripristinato con successo!")

//        } else {
//            Timber.d("WebDavLibraryViewModel Il database locale è aggiornato o più recente del cloud. Nessun download necessario.")
//        }
    }

    suspend fun fetchBackupInfo(account: WebDavAccount): WebDavBackupInfo? {
        val config = WebDavConfig(
            baseUrl = account.baseUrl,
            username = account.username,
            password = CryptoManager.decrypt(account.encryptedPassword),
        )
        return webDavLibraryRepository.fetchBackupInfo(config)
    }
}
