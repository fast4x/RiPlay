package it.fast4x.riplay.extensions.experimental.webdavlibrary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import it.fast4x.environment.models.PlayerResponse
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavBrowserState
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavConfig
import it.fast4x.riplay.extensions.experimental.webdavlibrary.models.WebDavSongMetadata
import it.fast4x.riplay.extensions.players.getOnlineMetadata
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.formatAsDuration
import kotlinx.coroutines.Dispatchers
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

class WebDavLibraryViewModel () : ViewModel(), ViewModelProvider.Factory {

    // Limite di 3 richieste contemporanee a YouTube
    private val metadataSemaphore = Semaphore(3)
    val webDavLibraryRepository = WebDavLibraryRepository()

    val appSettingsManager = (appContext() as MainApplication).appSettingsManager
    val appSettings = appSettingsManager.activeSettings.value

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebDavLibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WebDavLibraryViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("WebDavLibraryViewModel: onCleared() chiamato! Il ViewModel è stato distrutto.")
    }

    private val _uiState = MutableStateFlow<WebDavBrowserState>(WebDavBrowserState.Idle)
    val uiState: StateFlow<WebDavBrowserState> = _uiState.asStateFlow()

    fun loadFolder(config: WebDavConfig, folderPath: String) {
        viewModelScope.launch {
            _uiState.value = WebDavBrowserState.Loading
            try {
                val rawItems =
                    if (appSettings.isWebDavScanSubfoldersEnabled) webDavLibraryRepository.listDirectoryRecursive(config, folderPath)
                    else webDavLibraryRepository.listDirectory(config, folderPath)
                // Rimuove il primo elemento se è la cartella stessa che stiamo navigando
                val folderItems = rawItems.drop(1)

                val songs = folderItems.toSongs(config.baseUrl).distinctBy { it.id }
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
                                fetchMetadataIfNeeded(config, songToFetch)
                            }
                        }
                }
                Timber.d("WebDavLibraryViewModel Success loading folder")

            } catch (e: Exception) {
                _uiState.value = WebDavBrowserState.Error(e.message ?: "Errore sconosciuto")
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

    fun databaseBackupSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi (risparmio dati)
            .setRequiresBatteryNotLow(true) // Non farlo se la batteria è al 5%
            .build()

        val backupRequest = OneTimeWorkRequestBuilder<WebDavDatabaseSyncBackupWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueue(backupRequest)
    }
}
