package it.fast4x.riplay.services.playback

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.DrawableRes
import androidx.compose.ui.util.fastFilter
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.completed
import it.fast4x.environment.utils.from
import it.fast4x.riplay.Dependencies.application
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.commonutils.removePrefix
import it.fast4x.riplay.commonutils.toThumbnail
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.musicbrainz.repository.AlbumRepository
import it.fast4x.riplay.extensions.ondevice.OnDeviceViewModel
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.getTitleMonthlyPlaylist
import it.fast4x.riplay.utils.seamlessQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap


private const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
private const val CONTENT_STYLE_LIST = 1
private const val CONTENT_STYLE_GRID = 2


@UnstableApi
class MediaLibraryServiceCallback(
    private val binder: PlayerService.Binder,
    private val playerService: PlayerService
) : MediaLibrarySession.Callback {

    private val onDeviceViewModel: OnDeviceViewModel by lazy {
        OnDeviceViewModel(application)
    }

    var currentSongsListContext: List<Song> = emptyList()

    val appSettings = playerService.appSettings
    private val androidAutoPackages = setOf(
        "com.google.android.projection.gearhead",   // Android Auto
        "com.google.android.automotiveui"            // Android Automotive OS
    )

    // Cache thread-safe globale nel tuo Callback
    private val searchResultsCache = ConcurrentHashMap<String, List<MediaItem>>()

    // ==========================================================
    // ANDROID AUTO: Costruzione dell'albero (Root e Children)
    // ==========================================================

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)

        val availableCustomSessionCommands = NotificationButtons.entries.map { it.sessionCommand }
        val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            // Abilitiamo esplicitamente il comando di ricerca per Android Auto
            .add(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
            // Abilitiamo i comandi custom per Android Auto
            .addSessionCommands(availableCustomSessionCommands)
            .build()

        // Creiamo la configurazione dei pulsanti grafici (Custom Layout) per Android Auto
        val customLayout =
            NotificationButtons.entries
                .map {
                    CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                        .setDisplayName(it.name)
                        .setCustomIconResId(
                            it.getStateIcon(
                                it,
                                session.player.currentMediaItem?.asSong?.likedAt,
                                session.player.repeatMode,
                                session.player.shuffleModeEnabled
                            )
                        )
                        .setSessionCommand(it.sessionCommand)
                        .build()
                }


        val availablePlayerCommands = connectionResult.availablePlayerCommands.buildUpon()
            .add(Player.COMMAND_SET_MEDIA_ITEM)
            .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .build()



        val result = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableSessionCommands)
            .setAvailablePlayerCommands(availablePlayerCommands)
            .setCustomLayout(customLayout)
            .build()

        return result


    }


    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {

        if (!playerService.appSettings.isAndroidAutoEnabled) {
            return Futures.immediateFuture(LibraryResult.ofItem(MediaItem.EMPTY, params))
        }

        if (browser.packageName in androidAutoPackages) {
            GlobalSharedData.androidAutoConnected.value = true
            Timber.d("PlayerService: Android Auto connected (${browser.packageName})")
        }

        val rootExtras = Bundle().apply {
            // Dice ad Android Auto che la libreria è dinamica e supporta l'aggiornamento dei nodi figli
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            // altre opzioni di configurazione
            putBoolean(MEDIA_SEARCH_SUPPORTED, true)
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, if (playerService.appSettings.showGridAA) CONTENT_STYLE_GRID else CONTENT_STYLE_LIST)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }

        val libraryParams = MediaLibraryService.LibraryParams.Builder()
            .setExtras(rootExtras)
            .build()

        val rootItem = MediaItem.Builder()
            .setMediaId(MediaId.ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Riplay")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        playerService.serviceScope.launch(Dispatchers.IO) {
            try {
                val data = parentId.split('/')
                val id = data.getOrNull(1) ?: ""
                val idOnDeviceFolder = parentId.split(MediaId.PLAYLISTS_ONDEVICE).getOrNull(1)?.substringAfter('/') ?: ""

                Timber.d("RiplayMediaLibraryCallback onGetChildren $parentId data $data id $id")

                val resultList: MutableList<MediaItem> = when (data.firstOrNull()) {

                    MediaId.FAULT -> listOf(
                        faultBrowserMediaItem
                    )

                    MediaId.ROOT -> listOf(
                        songsBrowserMediaItem,
                        artistsFavoritesBrowserMediaItem,
                        albumsFavoritesBrowserMediaItem,
                        playlistsBrowserMediaItem
                    )

                    MediaId.SONGS -> {
                        Database
                            .songsFavorites(playerService.songsSortBy, playerService.songSortOrder)
                            .first()
                            .take(500)
                            .also { currentSongsListContext = it.map(SongEntity::song) }
                            .map { it.song.asPlayableMediaItem }
                            .toMutableList()
                            .apply {
                                if (playerService.appSettings.showOnDeviceAA) add(0, ondeviceBrowserMediaItem)
                                if (playerService.appSettings.showShuffleSongsAA) add(0, shuffleBrowserMediaItem)
                                if (playerService.appSettings.showTopSongsAA) add(0, topBrowserMediaItem)
                                if (playerService.appSettings.showAllSongsAA) add(0, allBrowserMediaItem)
                            }
                    }

                    MediaId.SONGS_ONDEVICE -> {
                        Database
                            .songsOnDevice()
                            .first()
                            .take(500)
                            .also { currentSongsListContext = it }
                            .map { it.asPlayableMediaItem }
                            .toMutableList()
                    }

                    MediaId.SONGS_SHUFFLE -> {
                        val shuffled = currentSongsListContext.shuffled()
                        currentSongsListContext = shuffled
                        shuffled.map { it.asPlayableMediaItem }.toMutableList()
                    }

                    MediaId.SONGS_ALL -> {
                        Database
                            .songs(playerService.songsSortBy, playerService.songSortOrder, 0)
                            .first()
                            .take(500)
                            .also { currentSongsListContext = it.map(SongEntity::song) }
                            .map { it.song.asPlayableMediaItem }
                            .toMutableList()
                    }

                    MediaId.SONGS_TOP -> {
                        val maxTopSongs = appSettings.maxTopPlaylistItems.number.toInt()
                        Database.trending(maxTopSongs)
                            .first()
                            .also { currentSongsListContext = it }
                            .map { it.asPlayableMediaItem }.toMutableList()
                    }

                    MediaId.PLAYLISTS -> {
                        if (id == "") {
                            Database
                                .playlistPreviews(playerService.playlistSortBy, playerService.songSortOrder)
                                .first()
                                .fastFilter {
                                    if (playerService.appSettings.showMonthlyPlaylistAA) true
                                    else !it.playlist.name.startsWith(MONTHLY_PREFIX)
                                }
                                .map { it.asBrowserMediaItem(Database.playlistThumbnailUrls(it.playlist.id).first().take(1)) }
                                .sortedBy { it.mediaMetadata.title.toString() }
                                .map { it.asCleanMediaItem }
                                .toMutableList()
                                .apply {
                                    if (playerService.appSettings.showOnDeviceAA) add(0, playlistsOnDeviceBrowserMediaItem)
                                    if (playerService.appSettings.showMonthlyPlaylistAA) add(0, playlistsMonthlyBrowserMediaItem)
                                    if (playerService.appSettings.showPinnedAA) add(0, playlistsPinnedBrowserMediaItem)
                                    if (playerService.appSettings.showPodcastAA) add(0, playlistsPodcastBrowserMediaItem)
                                    if (playerService.appSettings.showInLibraryAA) add(0, playlistsInLibraryBrowserMediaItem)
                                }
                        } else {
                            val playlistLimit = playerService.appSettings.androidAutoPlaylistLimit.number
                            Database
                                .songsPlaylist(id.toLong(), playerService.playlistSongsSortBy, playerService.songSortOrder)
                                .first()
                                .let { songs -> playlistLimit?.let { limit -> songs.take(limit) } ?: songs }
                                .also { currentSongsListContext = it.map(SongEntity::song) }
                                .map { it.song.asPlayableMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.PLAYLISTS_IN_LIBRARY -> {
                        Database
                            .playlistPreviews(playerService.playlistSortBy, playerService.songSortOrder)
                            .first()
                            .fastFilter {
                                it.playlist.isYoutubePlaylist
                            }
                            .map { it.asBrowserMediaItem(Database.playlistThumbnailUrls(it.playlist.id).first().take(1)) }
                            .sortedBy { it.mediaMetadata.title.toString() }
                            .map { it.asCleanMediaItem }
                            .toMutableList()
                    }

                    MediaId.PLAYLISTS_PODCAST -> {
                        Database
                            .playlistPreviews(playerService.playlistSortBy, playerService.songSortOrder)
                            .first()
                            .fastFilter {
                                it.playlist.isPodcast
                            }
                            .map { it.asBrowserMediaItem(Database.playlistThumbnailUrls(it.playlist.id).first().take(1)) }
                            .sortedBy { it.mediaMetadata.title.toString() }
                            .map { it.asCleanMediaItem }
                            .toMutableList()
                    }

                    MediaId.PLAYLISTS_PINNED -> {
                        Database
                            .playlistPreviews(playerService.playlistSortBy, playerService.songSortOrder)
                            .first()
                            .fastFilter {
                                it.playlist.isPinned
                            }
                            .map { it.asBrowserMediaItem(Database.playlistThumbnailUrls(it.playlist.id).first().take(1)) }
                            .sortedBy { it.mediaMetadata.title.toString() }
                            .map { it.asCleanMediaItem }
                            .toMutableList()
                    }

                    MediaId.PLAYLISTS_MONTHLY -> {
                        Database
                            .playlistPreviews(playerService.playlistSortBy, playerService.songSortOrder)
                            .first()
                            .fastFilter {
                                it.playlist.isMonthly
                            }
                            .map { it.asBrowserMediaItem(Database.playlistThumbnailUrls(it.playlist.id).first().take(1)) }
                            .sortedBy { it.mediaMetadata.title.toString() }
                            .map { it.asCleanMediaItem }
                            .toMutableList()
                    }

                    MediaId.PLAYLISTS_ONDEVICE -> {
                        if (idOnDeviceFolder == "") {
                            onDeviceViewModel.audioFoldersAsPlaylists().first()
                                .let { folders ->
                                    when (playerService.playlistSortBy) {
                                        PlaylistSortBy.Name -> when (playerService.songSortOrder) {
                                            SortOrder.Ascending -> folders.sortedBy { it.playlist.name }
                                            SortOrder.Descending -> folders.sortedByDescending { it.playlist.name }
                                        }

                                        PlaylistSortBy.DateAdded -> when (playerService.songSortOrder) {
                                            SortOrder.Ascending -> folders.sortedBy { it.totalPlayTimeMs }
                                            SortOrder.Descending -> folders.sortedByDescending { it.totalPlayTimeMs }
                                        }

                                        PlaylistSortBy.SongCount -> when (playerService.songSortOrder) {
                                            SortOrder.Ascending -> folders.sortedBy { it.songCount }
                                            SortOrder.Descending -> folders.sortedByDescending { it.songCount }
                                        }

                                        PlaylistSortBy.MostPlayed -> when (playerService.songSortOrder) {
                                            SortOrder.Ascending -> folders.sortedBy { it.totalPlayTimeMs }
                                            SortOrder.Descending -> folders.sortedByDescending { it.totalPlayTimeMs }
                                        }
                                    }.map {
                                        it.asBrowserMediaItem(
                                            Database.playlistThumbnailUrls(it.playlist.id).first()
                                                .take(1),
                                            true
                                        )
                                    }
                                        .sortedBy { it.mediaMetadata.title.toString() }
                                        .map { it.asCleanMediaItem }
                                        .toMutableList()
                                }

                        } else {
                            onDeviceViewModel.audioFilesFromFolder(
                                idOnDeviceFolder
                            ).first()
                                .map { it.song }
                                .also { currentSongsListContext = it }
                                .map { it.asPlayableMediaItem }
                                .toMutableList()
                        }

                    }

                    MediaId.ARTISTS_FAVORITES -> {

                        if (id == "") {
                            Database
                                .artists(playerService.artistSortBy, playerService.artistSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ARTISTS_FAVORITES) }
                                .toMutableList()
                                .apply {
                                    if (playerService.appSettings.showOnDeviceAA) add(0,artistsOnDeviceBrowserMediaItem)
                                    if (playerService.appSettings.showInLibraryAA) add(0,artistsInLibraryBrowserMediaItem)
                                }
                        } else {
                            val artist = Database.artist(id).first()
                            var songs = emptyList<Song>()
                            EnvironmentExt.getArtistPage(browseId = id)
                                .onSuccess { currentArtistPage ->
                                    var moreEndPointBrowseId: String? = null
                                    var moreEndPointParams: String? = null
                                    currentArtistPage.sections
                                        .forEach {
                                            if (it.items.firstOrNull() is Environment.SongItem) {
                                                moreEndPointBrowseId = it.moreEndpoint?.browseId
                                                moreEndPointParams = it.moreEndpoint?.params
                                                Timber.d("MediaLibraryCallback onGetchildren artist songs moreEndPointBrowseId $moreEndPointBrowseId")
                                            }
                                        }
                                        .also {
                                            if (moreEndPointBrowseId != null)
                                                if (artist != null) {
                                                    EnvironmentExt.getArtistItemsPage(
                                                        BrowseEndpoint(
                                                            browseId = moreEndPointBrowseId,
                                                            params = moreEndPointParams
                                                        )
                                                    ).completed().getOrNull()
                                                        ?.items
                                                        ?.map { it as Environment.SongItem }
                                                        ?.map { it.asSong }
                                                        .also {
                                                            if (it != null) {
                                                                songs = it
                                                            }
                                                        }
                                                        ?.onEach(Database::insert)
                                                        ?.map {
                                                            SongArtistMap(
                                                                songId = it.id,
                                                                artistId = artist.id
                                                            )
                                                        }
                                                        ?.onEach(Database::insert)
                                                }

                                        }

                                }

                            songs
                                .also { currentSongsListContext = it }
                                .map { it.asPlayableMediaItem }

                        }
                    }

                    MediaId.ARTISTS_ONDEVICE -> {

                        if (id == "") {
                            Database
                                .artistsOnDevice(playerService.artistSortBy, playerService.artistSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ARTISTS_ONDEVICE) }
                                .toMutableList()
                        } else {
                            Database.artistTopSongs(id, 100)
                                .first()
                                .also { currentSongsListContext = it }
                                .map { it.asPlayableMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ARTISTS_IN_LIBRARY -> {

                        if (id == "") {
                            Timber.d("MediaLibraryCallback onLoadChildren inside artists in library id $id")
                            Database
                                .artistsInLibrary(playerService.artistSortBy, playerService.artistSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ARTISTS_IN_LIBRARY) }
                                .toMutableList()
                        } else {
                            Timber.d("MediaLibraryCallback onLoadChildren inside artist single in library id $id")
                            val artist = Database.artist(id).first()
                            var songs = Database.artistAllSongs(id).first()
                            if (songs.isEmpty()) {
                                EnvironmentExt.getArtistPage(browseId = id)
                                    .onSuccess { currentArtistPage ->
                                        var moreEndPointBrowseId: String? = null
                                        var moreEndPointParams: String? = null
                                        currentArtistPage.sections
                                            .forEach {
                                                if (it.items.firstOrNull() is Environment.SongItem) {
                                                    moreEndPointBrowseId = it.moreEndpoint?.browseId
                                                    moreEndPointParams = it.moreEndpoint?.params
                                                    Timber.d("MediaLibraryCallback onLoadChildren artist in library songs moreEndPointBrowseId $moreEndPointBrowseId")
                                                }
                                            }
                                            .also {
                                                if (moreEndPointBrowseId != null)
                                                    if (artist != null) {
                                                        EnvironmentExt.getArtistItemsPage(
                                                            BrowseEndpoint(
                                                                browseId = moreEndPointBrowseId,
                                                                params = moreEndPointParams
                                                            )
                                                        ).completed().getOrNull()
                                                            ?.items
                                                            ?.map { it as Environment.SongItem }
                                                            ?.map { it.asSong }
                                                            .also {
                                                                if (it != null) {
                                                                    songs = it
                                                                }
                                                            }
                                                            ?.onEach(Database::insert)
                                                            ?.map {
                                                                SongArtistMap(
                                                                    songId = it.id,
                                                                    artistId = artist.id
                                                                )
                                                            }
                                                            ?.onEach(Database::insert)
                                                    }

                                            }

                                    }
                            }
                            Timber.d("MediaLibraryCallback onLoadChildren inside artist single in library id $id with songs size ${songs.size}")
                            songs
                                .also { currentSongsListContext = it }
                                .map { it.asPlayableMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_FAVORITES -> {

                        if (id == "") {
                            Timber.d("MediaLibraryCallback onLoadChildren inside albums id $id")
                            Database
                                .albums(playerService.albumSortBy, playerService.albumSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_FAVORITES) }
                                .toMutableList()
                                .apply {
                                    if (playerService.appSettings.showOnDeviceAA) add(0,albumsOnDeviceBrowserMediaItem)
                                    if (playerService.appSettings.showInLibraryAA) add(0,albumsInLibraryBrowserMediaItem)
                                }
                        } else {
                            Timber.d("MediaLibraryCallback onLoadChildren inside albums SONGS id $id")
                            val album = Database.album(id).first()
                            var songs = Database.albumSongs(id).first()
                            if (songs.isEmpty()) {
                                EnvironmentExt.getAlbum(id)
                                    .onSuccess { currentAlbumPage ->
                                        val innerSongs = currentAlbumPage
                                            .songs.distinct()
                                            .also { songItems ->
                                                songs = songItems
                                                    .map(Environment.SongItem::asSong)
                                            }

                                        val innerSongsAlbumMap = innerSongs
                                            .map(Environment.SongItem::asMediaItem)
                                            .onEach(Database::insert)
                                            .mapIndexed { position, mediaItem ->
                                                SongAlbumMap(
                                                    songId = mediaItem.mediaId,
                                                    albumId = id,
                                                    position = position
                                                )
                                            }

                                        val album = Album(
                                            id = id,
                                            title = album?.title ?: currentAlbumPage.album.title,
                                            thumbnailUrl = if (album?.thumbnailUrl?.startsWith(
                                                    MODIFIED_PREFIX
                                                ) == true
                                            ) album.thumbnailUrl else currentAlbumPage.album.thumbnail?.url,
                                            year = currentAlbumPage.album.year,
                                            authorsText = if (album?.authorsText?.startsWith(
                                                    MODIFIED_PREFIX
                                                ) == true
                                            ) album.authorsText else currentAlbumPage.album.authors
                                                ?.joinToString(", ") { it.name ?: "" },
                                            shareUrl = currentAlbumPage.url,
                                            timestamp = System.currentTimeMillis(),
                                            bookmarkedAt = album?.bookmarkedAt,
                                            isYoutubeAlbum = album?.isYoutubeAlbum == true
                                        )
                                        AlbumRepository().upsertSmart(album)
                                        Database.upsertSongsAlbumMaps(innerSongsAlbumMap)
                                    }
                            }

                            songs
                                .also { currentSongsListContext = it }
                                .map { it.asPlayableMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_ON_DEVICE -> {

                        if (id == "") {
                            Timber.d("MediaLibraryCallback onLoadChildren inside albums on device id $id")
                            Database
                                .albumsOnDevice(playerService.albumSortBy, playerService.albumSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_ON_DEVICE) }
                                .toMutableList()
                        } else {
                            Timber.d("MediaLibraryCallback onLoadChildren inside albums SONGS id $id")
                            Database.albumSongs(id)
                                .first()
                                .also { currentSongsListContext = it }
                                .map { it.asPlayableMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_IN_LIBRARY -> {

                        if (id == "") {
                            Timber.d("MediaLibraryCallback onLoadChildren inside albums on device id $id")
                            Database
                                .albumsInLibrary(playerService.albumSortBy, playerService.albumSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_IN_LIBRARY) }
                                .toMutableList()
                        } else {
                            Timber.d("MediaLibraryCallback onLoadChildren inside albums SONGS id $id")
                            val album = Database.album(id).first()
                            var songs = Database.albumSongs(id).first()
                            if (songs.isEmpty()) {
                                EnvironmentExt.getAlbum(id)
                                    .onSuccess { currentAlbumPage ->
                                        val innerSongs = currentAlbumPage
                                            .songs.distinct()
                                            .also { songItems ->
                                                songs = songItems
                                                    .map(Environment.SongItem::asSong)
                                            }

                                        val innerSongsAlbumMap = innerSongs
                                            .map(Environment.SongItem::asMediaItem)
                                            .onEach(Database::insert)
                                            .mapIndexed { position, mediaItem ->
                                                SongAlbumMap(
                                                    songId = mediaItem.mediaId,
                                                    albumId = id,
                                                    position = position
                                                )
                                            }
                                        val album = Album(
                                            id = id,
                                            title = album?.title ?: currentAlbumPage.album.title,
                                            thumbnailUrl = if (album?.thumbnailUrl?.startsWith(
                                                    MODIFIED_PREFIX
                                                ) == true
                                            ) album.thumbnailUrl else currentAlbumPage.album.thumbnail?.url,
                                            year = currentAlbumPage.album.year,
                                            authorsText = if (album?.authorsText?.startsWith(
                                                    MODIFIED_PREFIX
                                                ) == true
                                            ) album.authorsText else currentAlbumPage.album.authors
                                                ?.joinToString(", ") { it.name ?: "" },
                                            shareUrl = currentAlbumPage.url,
                                            timestamp = System.currentTimeMillis(),
                                            bookmarkedAt = album?.bookmarkedAt,
                                            isYoutubeAlbum = album?.isYoutubeAlbum == true
                                        )
                                        AlbumRepository().upsertSmart(album)
                                        Database.upsertSongsAlbumMaps(innerSongsAlbumMap)
                                    }
                            }

                            songs
                                .also { currentSongsListContext = it }
                                .map { it.asPlayableMediaItem }
                                .toMutableList()
                        }
                    }

                    else -> mutableListOf()
                } as MutableList<MediaItem>

                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(resultList), params))

            } catch (e: Exception) {
                Timber.e(e, "Errore critico nel caricamento dei figli per $parentId")
                future.setException(e)
            }
        }

        return future
    }


    override fun onSetMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
        playerService.serviceScope.future {
            Timber.d("MediaLibraryCallback onSetMediaItems")
            // 1. Verifichiamo se la richiesta proviene da una ricerca vocale (Google Assistant)
            // Se c'è una searchQuery, significa che l'utente ha pronunciato un testo
            val firstItem = mediaItems.firstOrNull()
            //val searchQuery = firstItem?.requestMetadata?.searchQuery
            val extras = firstItem?.requestMetadata?.extras

            val defaultResult =
                MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
            val voiceAssistantQuery = mediaItems.firstOrNull()?.requestMetadata?.searchQuery

            val data = if (!voiceAssistantQuery.isNullOrBlank()) {
                listOf(MediaId.SEARCHED, voiceAssistantQuery, "")
            } else {
                mediaItems.firstOrNull()?.mediaId?.split("/")
            } ?: return@future defaultResult

            val idToPlay = data.getOrNull(1) ?: ""




            when (data.firstOrNull()) {
                MediaId.SEARCHED -> {
                    val cleanQuery = voiceAssistantQuery?.trim()

                    if (cleanQuery?.isNotBlank() == true) {

                        if (extras != null) {
                            val isArtistRequest = extras.containsKey(android.provider.MediaStore.EXTRA_MEDIA_ARTIST)
                            val isAlbumRequest = extras.containsKey(android.provider.MediaStore.EXTRA_MEDIA_ALBUM)

                            when {
                                isArtistRequest -> {
                                    val artistName = extras.getString(android.provider.MediaStore.EXTRA_MEDIA_ARTIST)
                                    // Esegui una query mirata solo sulla colonna degli Artisti
                                    Timber.d("MediaLibraryCallback onSetMediaItems from voice artistName $artistName")
                                }
                                isAlbumRequest -> {
                                    val albumTitle = extras.getString(android.provider.MediaStore.EXTRA_MEDIA_ALBUM)
                                    // Esegui una query mirata solo sulla colonna degli Album
                                    Timber.d("MediaLibraryCallback onSetMediaItems from voice albumTitle $albumTitle")
                                }
                            }
                        }
                        // 2. L'utente ha cercato un brano specifico (es: "Riproduci brano X")
                        val songs = Database.songDao().getSongsByTitleLike(cleanQuery, limit = 50)
                        Timber.d("MediaLibraryCallback onSetMediaItems from voice songs ${songs.size}")
                        if (songs.isNotEmpty()) {
                            // Restituiamo i brani trovati convertiti in MediaItem
                            return@future MediaItemsWithStartPosition(songs.map { it.asMediaItem }, startIndex, startPositionMs)
                        }
                    }

                    // 3. FALLBACK CASUALE (Se la ricerca non produce risultati o la query è vuota / "metti musica")
                    // Recuperiamo i brani locali/casuali per non lasciare l'auto in silenzio totale
                    val fallbackSongs = Database.songDao().getSongsWithGenres(50)
                    return@future MediaItemsWithStartPosition(fallbackSongs.map { it.asMediaItem }.toMutableList(), startIndex, startPositionMs)
                }
            }

            val safeStartIndex = currentSongsListContext.indexOfFirst { it.id == idToPlay }.takeIf { it != -1 } ?: 0

            Timber.d("MediaLibraryCallback onSetMediaItems data = $data idToPlay = $idToPlay currentBrowseContext ${currentSongsListContext.map { it.id }} safeStartIndex = $safeStartIndex startIndex = $startIndex startPositionMs = $startPositionMs")

            return@future MediaItemsWithStartPosition(currentSongsListContext.map { it.asMediaItem }, safeStartIndex, startPositionMs)


        }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        playerService.serviceScope.future {
            Timber.d("MediaLibraryCallback onSearch $query")
            if (query.isEmpty()) {
                return@future LibraryResult.ofItemList(emptyList(), params)
            }

            currentSongsListContext = Environment.searchPage(
                body = SearchBody(
                    query = query,
                    params = Environment.SearchFilter.Song.value
                ),
                fromMusicShelfRendererContent = Environment.SongItem.Companion::from
            )?.map {
                it?.items?.map { it.asSong }
            }?.getOrNull() ?: emptyList()

            searchResultsCache[query] = currentSongsListContext.map { it.asPlayableMediaItem }
            session.notifySearchResultChanged(browser, query, currentSongsListContext.size, params)
            return@future LibraryResult.ofVoid()
        }

        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        Timber.d("MediaLibraryCallback onGetSearchResult $query")

        // Recuperiamo i dati precaricati dalla cache
        val fullList = searchResultsCache[query] ?: emptyList()

        // Gestiamo la paginazione richiesta da Android Auto in modo sicuro per evitare IndexOutOfBoundsException
        val fromIndex = page * pageSize
        val pagedList = if (fromIndex >= fullList.size) {
            emptyList()
        } else {
            val toIndex = minOf(fromIndex + pageSize, fullList.size)
            fullList.subList(fromIndex, toIndex)
        }

        // Restituiamo la lista ritagliata per la pagina corrente
        return Futures.immediateFuture(LibraryResult.ofItemList(pagedList, params))

    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        Timber.d("MediaLibraryCallback onCustomCommand $customCommand customAction ${customCommand.customAction}")
        when (customCommand.customAction) {
            MediaSessionConstants.CommandSearch.customAction -> { binder.actionSearch() }
            MediaSessionConstants.CommandToggleLike.customAction -> {
                playerService.serviceScope.launch {
                    binder.toggleLike()
                    withContext(Dispatchers.Main) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            playerService.hybridPlayer.onRefreshCustomLayoutListener?.invoke()
                        }, 100)
                        updateCustomLayout(session)
                    }
                }
            }
            MediaSessionConstants.CommandStartRadio.customAction -> {
                session.player.currentMediaItem?.let {
                    binder.stopRadio()
                    session.player.seamlessQueue(it)

                    binder.setupRadio(
                        NavigationEndpoint.Endpoint.Watch(videoId = it.mediaId)
                    )
                }
            }
            MediaSessionConstants.CommandToggleShuffle.customAction -> { binder.toggleShuffle() }
            MediaSessionConstants.CommandToggleRepeatMode.customAction -> { binder.toggleRepeat()}
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    @OptIn(UnstableApi::class)
    override fun onSubscribe(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        //Accettiamo la sottoscrizione in modo incondizionato per i nostri ID.
        // Questo impedisce a Media3 di disiscrivere Android Auto se onGetItem non è implementato,
        // sbloccando la ricezione di ogni futuro "notifyChildrenChanged".
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    private fun uriFor(@DrawableRes id: Int) = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(playerService.resources.getResourcePackageName(id))
        .appendPath(playerService.resources.getResourceTypeName(id))
        .appendPath(playerService.resources.getResourceEntryName(id))
        .build()

    private val faultBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.FAULT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Fault")
                    .setArtworkUri(uriFor(R.drawable.close))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val songsBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.SONGS)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.songs))
                    .setArtworkUri(uriFor(R.drawable.musical_notes))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val playlistsBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.PLAYLISTS)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.playlists))
                    .setArtworkUri(uriFor(R.drawable.music_library))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val playlistsInLibraryBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.PLAYLISTS_IN_LIBRARY)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.library))
                    .setArtworkUri(uriFor(R.drawable.music_library))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val playlistsPinnedBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.PLAYLISTS_PINNED)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.pinned_playlists))
                    .setArtworkUri(uriFor(R.drawable.pin))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val playlistsMonthlyBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.PLAYLISTS_MONTHLY)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.monthly_playlists))
                    .setArtworkUri(uriFor(R.drawable.stat_month))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val playlistsOnDeviceBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.PLAYLISTS_ONDEVICE)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.on_device))
                    .setArtworkUri(uriFor(R.drawable.folder))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val playlistsPodcastBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.PLAYLISTS_PODCAST)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.podcasts))
                    .setArtworkUri(uriFor(R.drawable.podcast))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val albumsFavoritesBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.ALBUMS_FAVORITES)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.albums))
                    .setArtworkUri(uriFor(R.drawable.music_album))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val albumsInLibraryBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.ALBUMS_IN_LIBRARY)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.library))
                    .setArtworkUri(uriFor(R.drawable.music_album))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val albumsOnDeviceBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.ALBUMS_ON_DEVICE)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.on_device))
                    .setArtworkUri(uriFor(R.drawable.music_album))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val artistsFavoritesBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.ARTISTS_FAVORITES)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.artists))
                    .setArtworkUri(uriFor(R.drawable.music_artist))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val artistsInLibraryBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.ARTISTS_IN_LIBRARY)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.library))
                    .setArtworkUri(uriFor(R.drawable.music_artist))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val artistsOnDeviceBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.ARTISTS_ONDEVICE)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.on_device))
                    .setArtworkUri(uriFor(R.drawable.music_artist))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val shuffleBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.SONGS_SHUFFLE)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.shuffle))
                    .setArtworkUri(uriFor(R.drawable.shuffle))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val allBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.SONGS_ALL)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.all_songs))
                    .setArtworkUri(uriFor(R.drawable.music))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val topBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.SONGS_TOP)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.my_playlist_top).format(appSettings.maxTopPlaylistItems.number))
                    .setArtworkUri(uriFor(R.drawable.trending))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val ondeviceBrowserMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.SONGS_ONDEVICE)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playerService.resources.getString(R.string.on_device))
                    .setArtworkUri(uriFor(R.drawable.musical_notes))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val Song.asPlayableMediaItem: MediaItem
        get() = MediaItem.Builder()
            .setMediaId(MediaId.forSong(id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title.removePrefix())
                    .setArtist(artistsText)
                    .setArtworkUri(thumbnailUrl?.toUri())
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    private fun PlaylistPreview.asBrowserMediaItem(thumbnailUrls: List<String?>, onDevice: Boolean? = false): MediaItem {

        val mediaId = if (onDevice == false) MediaId.forPlaylist(playlist.id)
        else MediaId.forPlaylistOnDevice(folder ?: "")

        val cleanTitle = when {
            playlist.name.startsWith(PINNED_PREFIX) -> playlist.name.replace(PINNED_PREFIX, "0:", true)
            playlist.name.startsWith(MONTHLY_PREFIX) -> playlist.name.replace(MONTHLY_PREFIX, "1:", true)
            else -> playlist.name.removePrefix()
        }

        val artworkUri = if (playlist.browseId?.trim() == "LM") {
            "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-music-@1200.png".toUri()
        } else {
            uriFor(
                when {
                    playlist.name.startsWith(PINNED_PREFIX) -> R.drawable.pin
                    playlist.name.startsWith(MONTHLY_PREFIX) -> R.drawable.stat_month
                    else -> R.drawable.playlist
                }
            )
        }

        val subtitle = "$songCount ${playerService.resources.getString(R.string.songs)}"

        val extrasBundle = bundleOf("browseId" to playlist.browseId).apply {
            thumbnailUrls.forEachIndexed { index, url ->
                putString("thumbnailUrl$index", url)
            }
        }

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(cleanTitle)
                    .setSubtitle(subtitle)
                    .setArtworkUri(artworkUri)
                    .setExtras(extrasBundle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private val PlaylistPreview.asBrowserMediaItem: MediaItem
        get() {
            val cleanTitle = when {
                playlist.name.startsWith(PINNED_PREFIX) -> playlist.name.replace(PINNED_PREFIX, "0:", true)
                playlist.name.startsWith(MONTHLY_PREFIX) -> playlist.name.replace(MONTHLY_PREFIX, "1:", true)
                else -> playlist.name.removePrefix()
            }

            val artworkUri = if (playlist.browseId?.trim() == "LM") {
                "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-music-@1200.png".toUri()
            } else {
                uriFor(
                    when {
                        playlist.name.startsWith(PINNED_PREFIX) -> R.drawable.pin
                        playlist.name.startsWith(MONTHLY_PREFIX) -> R.drawable.stat_month
                        else -> R.drawable.playlist
                    }
                )
            }

            return MediaItem.Builder()
                .setMediaId(MediaId.forPlaylist(playlist.id))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(cleanTitle)
                        .setSubtitle("$songCount ${playerService.resources.getString(R.string.songs)}")
                        .setArtworkUri(artworkUri)
                        .setExtras(bundleOf("browseId" to playlist.browseId))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

    private fun Album.asBrowserMediaItem(type: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(
                when(type) {
                    MediaId.ALBUMS_FAVORITES -> MediaId.forAlbumFavorites(id)
                    MediaId.ALBUMS_ON_DEVICE -> MediaId.forAlbumOnDevice(id)
                    MediaId.ALBUMS_IN_LIBRARY -> MediaId.forAlbumInLibrary(id)
                    else -> MediaId.forAlbumFavorites(id)
                }
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title?.removePrefix())
                    .setSubtitle(authorsText)
                    .setArtworkUri(thumbnailUrl?.toThumbnail(appSettings.albumsItemSize.size)?.toUri())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun Artist.asBrowserMediaItem(type: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(
                when(type) {
                    MediaId.ARTISTS_FAVORITES -> MediaId.forArtistFavorites(id)
                    MediaId.ARTISTS_ONDEVICE -> MediaId.forArtistOnDevice(id)
                    MediaId.ARTISTS_IN_LIBRARY -> MediaId.forArtistInLibrary(id)
                    else -> MediaId.forArtistFavorites(id)
                }
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name?.removePrefix())
                    .setArtworkUri(thumbnailUrl?.toThumbnail(appSettings.artistsItemSize.size)?.toUri())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private val MediaItem.asCleanMediaItem: MediaItem
        get() {
            val rawTitle = mediaMetadata.title?.toString() ?: ""

            val cleanTitle = when {
                rawTitle.startsWith("0:") -> rawTitle.substringAfter("0:")
                rawTitle.startsWith("1:") -> getTitleMonthlyPlaylist(rawTitle.substringAfter("1:"), playerService)
                else -> rawTitle
            }

            val extras = mediaMetadata.extras
            val browseId = extras?.getString("browseId")
            val thumbnailUrl0 = extras?.getString("thumbnailUrl0")

            val artworkUri = when {
                browseId == "LM" -> "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-music-@1200.png".toUri()
                browseId != "LM" && !thumbnailUrl0.isNullOrEmpty() -> thumbnailUrl0.toUri()
                else -> {
                    uriFor(
                        when {
                            rawTitle.startsWith("0:") -> R.drawable.pin
                            rawTitle.startsWith("1:") -> R.drawable.stat_month
                            else -> R.drawable.playlist
                        }
                    )
                }
            }

            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(cleanTitle)
                        .setArtworkUri(artworkUri)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

    fun updateCustomLayout(session: MediaSession) {
        // Rigenero la lista dei 5 bottoni leggendo gli stati aggiornati del player/canzone
        val customLayout = NotificationButtons.entries.map { buttonEntry ->
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setDisplayName(buttonEntry.name)
                .setCustomIconResId(
                    buttonEntry.getStateIcon(
                        buttonEntry,
                        playerService.currentSong.value?.likedAt,
                        session.player.repeatMode,
                        session.player.shuffleModeEnabled
                    )
                )
                .setSessionCommand(buttonEntry.sessionCommand)
                .build()
        }

        // Comunichiamo a Media3 e ad Android Auto di ridisegnare la barra all'istante
        session.setCustomLayout(customLayout)
    }


    // OBJECTS WITH CONSTANTS

    object MediaId {
        const val FAULT = "fault"
        const val ROOT = "root"
        const val SONGS = "songs"
        const val PLAYLISTS = "playlists"
        const val PLAYLISTS_IN_LIBRARY = "playlistsInLibrary"
        const val PLAYLISTS_PODCAST = "playlistsPodcast"
        const val PLAYLISTS_PINNED = "playlistsPinned"
        const val PLAYLISTS_MONTHLY = "playlistsMonthly"
        const val PLAYLISTS_ONDEVICE = "playlistsOnDevice"
        const val ALBUMS_FAVORITES = "albumsFavorites"
        const val ALBUMS_IN_LIBRARY = "albumsInLibrary"
        const val ALBUMS_ON_DEVICE = "albumsOnDevice"
        const val ARTISTS_FAVORITES = "artistsFavorites"
        const val ARTISTS_IN_LIBRARY = "artistsInLibrary"
        const val ARTISTS_ONDEVICE = "artistsOnDevice"

        const val SEARCHED = "searched"

        //const val SONGS_FAVORITES = "favorites"
        const val SONGS_ALL = "all"
        const val SONGS_SHUFFLE = "shuffle"
        const val SONGS_ONDEVICE = "ondevice"
        const val SONGS_TOP = "top"

        fun forSong(id: String) = "$SONGS/$id"
        fun forPlaylist(id: Long) = "$PLAYLISTS/$id"
        fun forPlaylistOnDevice(folder: String) = "$PLAYLISTS_ONDEVICE/$folder"
        fun forAlbumFavorites(id: String) = "$ALBUMS_FAVORITES/$id"
        fun forAlbumInLibrary(id: String) = "$ALBUMS_IN_LIBRARY/$id"
        fun forAlbumOnDevice(id: String) = "$ALBUMS_ON_DEVICE/$id"
        fun forArtistFavorites(id: String) = "$ARTISTS_FAVORITES/$id"
        fun forArtistInLibrary(id: String) = "$ARTISTS_IN_LIBRARY/$id"
        fun forArtistOnDevice(id: String) = "$ARTISTS_ONDEVICE/$id"

        fun forSearched(id: String) = "$SEARCHED/$id"
    }

    object MediaSessionConstants {
        const val ACTION_TOGGLE_LIKE = "TOGGLE_LIKE"
        const val ACTION_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT_MODE = "TOGGLE_REPEAT_MODE"
        const val ACTION_START_RADIO = "START_RADIO"
        const val ACTION_SEARCH = "ACTION_SEARCH"
        val CommandToggleLike = SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY)
        val CommandToggleShuffle = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
        val CommandToggleRepeatMode = SessionCommand(ACTION_TOGGLE_REPEAT_MODE, Bundle.EMPTY)
        val CommandStartRadio = SessionCommand(ACTION_START_RADIO, Bundle.EMPTY)
        val CommandSearch = SessionCommand(ACTION_SEARCH, Bundle.EMPTY)
    }

}