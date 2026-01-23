package it.fast4x.riplay.service

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastFilter
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.completed
import it.fast4x.environment.utils.from
import it.fast4x.riplay.commonutils.MODIFIED_PREFIX
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.MaxTopPlaylistItems
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.preferences.MaxTopPlaylistItemsKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.commonutils.removePrefix
import it.fast4x.riplay.enums.PlaylistSongSortBy
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SongSortBy
import it.fast4x.riplay.extensions.preferences.albumSortByKey
import it.fast4x.riplay.extensions.preferences.albumSortOrderKey
import it.fast4x.riplay.extensions.preferences.artistSortByKey
import it.fast4x.riplay.extensions.preferences.artistSortOrderKey
import it.fast4x.riplay.extensions.preferences.observeKeyEnum
import it.fast4x.riplay.extensions.preferences.observeSortBy
import it.fast4x.riplay.extensions.preferences.playlistSongSortByKey
import it.fast4x.riplay.extensions.preferences.playlistSortByKey
import it.fast4x.riplay.extensions.preferences.playlistSortOrderKey
import it.fast4x.riplay.extensions.preferences.songSortByKey
import it.fast4x.riplay.extensions.preferences.songSortOrderKey
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.getTitleMonthlyPlaylist
import it.fast4x.riplay.utils.intent
import it.fast4x.riplay.utils.showFavoritesPlaylistsAA
import it.fast4x.riplay.utils.showGridAA
import it.fast4x.riplay.utils.showInLibraryAA
import it.fast4x.riplay.utils.showMonthlyPlaylistsAA
import it.fast4x.riplay.utils.showOnDeviceAA
import it.fast4x.riplay.utils.showTopPlaylistAA
import it.fast4x.riplay.utils.shuffleSongsAAEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.also

@UnstableApi
class PlayerMediaBrowserService : MediaBrowserServiceCompat(),
    ServiceConnection,
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        var lastSongs = emptyList<Song>()
        var searchedSongs = emptyList<Song>()
    }

    //val context = (this as Context)
    //private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var playlistSongsSortBy: PlaylistSongSortBy = PlaylistSongSortBy.DateAdded
    private var songsSortBy: SongSortBy = SongSortBy.DateAdded
    private var playlistSortBy: PlaylistSortBy = PlaylistSortBy.DateAdded
    private var artistSortBy: ArtistSortBy = ArtistSortBy.DateAdded
    private var albumSortBy: AlbumSortBy = AlbumSortBy.DateAdded


    private var songSortOrder: SortOrder = SortOrder.Descending
    private var artistSortOrder: SortOrder = SortOrder.Descending
    private var albumSortOrder: SortOrder = SortOrder.Descending



    private var bound = false


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        Timber.d("PlayerMediaBrowserService onSharedPreferenceChanged $key")
        when (key) {
            songSortByKey -> {
                songsSortBy = sharedPreferences.getEnum(songSortByKey, SongSortBy.DateAdded)
                notifyChildrenChanged(MediaId.SONGS)
            }

            songSortOrderKey -> {
                songSortOrder = sharedPreferences.getEnum(songSortOrderKey, SortOrder.Descending)
                notifyChildrenChanged(MediaId.SONGS)
            }

            artistSortOrderKey -> {
                artistSortOrder =
                    sharedPreferences.getEnum(artistSortOrderKey, SortOrder.Descending)
                notifyChildrenChanged(MediaId.ARTISTS_FAVORITES)
            }
            artistSortByKey -> {
                artistSortBy =
                    sharedPreferences.getEnum(artistSortByKey, ArtistSortBy.DateAdded)
                notifyChildrenChanged(MediaId.ARTISTS_FAVORITES)
            }

            albumSortOrderKey -> {
                albumSortOrder = sharedPreferences.getEnum(albumSortOrderKey, SortOrder.Descending)
                notifyChildrenChanged(MediaId.ALBUMS_FAVORITES)
            }

            albumSortByKey -> {
                albumSortBy =
                    sharedPreferences.getEnum(albumSortByKey, AlbumSortBy.DateAdded)
                notifyChildrenChanged(MediaId.ALBUMS_FAVORITES)
            }

            playlistSongSortByKey -> {
                playlistSongsSortBy =
                    sharedPreferences.getEnum(playlistSongSortByKey, PlaylistSongSortBy.DateAdded)
                notifyChildrenChanged(MediaId.PLAYLISTS)
            }

            playlistSortByKey -> {
                playlistSortBy =
                    sharedPreferences.getEnum(playlistSortByKey, PlaylistSortBy.DateAdded)
                notifyChildrenChanged(MediaId.PLAYLISTS)
            }

        }
    }

    override fun onCreate() {
        super.onCreate()

        songsSortBy = preferences.getEnum(songSortByKey, SongSortBy.DateAdded)
        songSortOrder = preferences.getEnum(songSortOrderKey, SortOrder.Descending)
        playlistSortBy = preferences.getEnum(playlistSortByKey, PlaylistSortBy.DateAdded)
        playlistSongsSortBy = preferences.getEnum(playlistSongSortByKey, PlaylistSongSortBy.DateAdded)
        artistSortBy = preferences.getEnum(artistSortByKey, ArtistSortBy.DateAdded)
        artistSortOrder = preferences.getEnum(artistSortOrderKey, SortOrder.Descending)
        albumSortBy = preferences.getEnum(albumSortByKey, AlbumSortBy.DateAdded)
        albumSortOrder = preferences.getEnum(albumSortOrderKey, SortOrder.Descending)



        preferences.registerOnSharedPreferenceChangeListener(this)

        Timber.d("PlayerMediaBrowserService onCreate")
    }


    override fun onDestroy() {
        if (bound) {
            unbindService(this)
        }
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    @UnstableApi
    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        if (service is PlayerService.Binder) {
            bound = true
            sessionToken = service.mediaSession.sessionToken
            // IMPORTANT: Do not override the MediaSession callback here.
            // PlayerService owns the callback and implements the authoritative queue/skip logic.
        }
    }

    override fun onServiceDisconnected(name: ComponentName) = Unit

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        bindService(intent<PlayerService>(), this, BIND_AUTO_CREATE)
        return BrowserRoot(
            MediaId.ROOT,
            Bundle().apply {
                putBoolean(MEDIA_SEARCH_SUPPORTED, true)
                putBoolean(CONTENT_STYLE_SUPPORTED, true)
                putInt(CONTENT_STYLE_BROWSABLE_HINT, if (showGridAA()) CONTENT_STYLE_GRID else CONTENT_STYLE_LIST)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
            }
        )
        /*
        return if (clientUid == Process.myUid()
            || clientUid == Process.SYSTEM_UID
            || clientPackageName == "com.google.android.projection.gearhead"
        ) {
            bindService(intent<PlayerService>(), this, Context.BIND_AUTO_CREATE)
            BrowserRoot(
                MediaId.root,
                bundleOf("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT" to 1)
            )
        } else {
            null
        }
         */
    }

    @OptIn(UnstableApi::class)
    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<List<MediaItem>>
    ) {
        result.detach()
        runBlocking(Dispatchers.IO) {
            searchedSongs = Environment.searchPage(
                body = SearchBody(
                    query = query,
                    params = Environment.SearchFilter.Song.value
                ),
                fromMusicShelfRendererContent = Environment.SongItem.Companion::from
            )?.map {
                it?.items?.map { it.asSong }
            }?.getOrNull() ?: emptyList()

            val resultList = searchedSongs.map {
                MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(MediaId.forSearched(it.id))
                        .setTitle(it.title.removePrefix())
                        .setSubtitle(it.artistsText)
                        .setIconUri(it.thumbnailUrl?.toUri())
                        .build(),
                    MediaItem.FLAG_PLAYABLE
                )
            }

            result.sendResult(resultList)
        }
    }


    @OptIn(UnstableApi::class)
    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaItem?>?>
    ) {
        val data = parentId.split('/')
        val id = data.getOrNull(1) ?: ""
        Timber.d("PlayerMediaBrowserService onLoadChildren $parentId data $data")
        runBlocking(Dispatchers.IO) {
            result.sendResult(
                when (data.firstOrNull()) {

                    MediaId.FAULT ->listOf(
                        faultBrowserMediaItem
                    )

                    // Start Navigation items
                    MediaId.ROOT -> listOf(
                        songsBrowserMediaItem,
                        artistsFavoritesBrowserMediaItem,
                        albumsFavoritesBrowserMediaItem,
                        playlistsBrowserMediaItem
                    )

                    MediaId.SONGS -> {

                        Database
                            //.songsByPlayTimeDesc()
                            .songs(songsSortBy, songSortOrder, 0)
                            .first()
                            .take(500)
                            .also { lastSongs = it.map { it.song } }
                            .map { it.song.asBrowserMediaItem }
                            .toMutableList()
                            .apply {
                                if (shuffleSongsAAEnabled() && isNotEmpty()) add(
                                    0,
                                    shuffleBrowserMediaItem
                                )
                            }
                    }

                    MediaId.PLAYLISTS -> {

                        if (id == "") {
                            Database
                                .playlistPreviews(playlistSortBy, songSortOrder)
                                //.playlistPreviewsByNameAsc()
                                .first()
                                .fastFilter {
                                    if (showMonthlyPlaylistsAA()) true
                                    else !it.playlist.name.startsWith(MONTHLY_PREFIX)
                                }
                                .map { it.asBrowserMediaItem }
                                .sortedBy { it.description.title.toString() }
                                .map { it.asCleanMediaItem }
                                .toMutableList()
                                .apply {
                                    if (showFavoritesPlaylistsAA())
                                        add(0, favoritesBrowserMediaItem)
                                    if (showTopPlaylistAA())
                                        add(1, topBrowserMediaItem)
                                    if (showOnDeviceAA())
                                        add(2, ondeviceBrowserMediaItem)
                                }
                        } else {
                            Database
                                .songsPlaylist(id.toLong(), playlistSongsSortBy, songSortOrder)
                                //.playlistWithSongs(id.toLong())
                                .first()
                                //?.songs
                                .also { lastSongs = it.map { it.song } }
                                .map { it.song.asBrowserMediaItem }
                                .toMutableList()
                        }

                    }

                    MediaId.ARTISTS_FAVORITES -> {

                        if (id == "") {
                            Database
                                .artists(artistSortBy, artistSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ARTISTS_FAVORITES) }
                                .toMutableList()
                                .apply {
                                    if (showInLibraryAA())
                                        add(0, artistsInLibraryBrowserMediaItem)
                                    if (showOnDeviceAA())
                                        add(1, artistsOnDeviceBrowserMediaItem)
                                }
                        } else {
                            val artist = Database.artist(id).first()
                            //var songs = Database.artistAllSongs(id).first()
                            var songs = emptyList<Song>()
                            //if (songs.isEmpty()) {
                                EnvironmentExt.getArtistPage(browseId = id)
                                    .onSuccess { currentArtistPage ->
                                        var moreEndPointBrowseId: String? = null
                                        var moreEndPointParams: String? = null
                                        currentArtistPage.sections
                                            .forEach {
                                                if (it.items.firstOrNull() is Environment.SongItem) {
                                                    moreEndPointBrowseId = it.moreEndpoint?.browseId
                                                    moreEndPointParams = it.moreEndpoint?.params
                                                    Timber.d("PlayerMediaBrowserService onGetchildren artist songs moreEndPointBrowseId $moreEndPointBrowseId")
                                                }
                                            }
                                            .also {
                                                if (moreEndPointBrowseId != null)
                                                    if (artist != null) {
                                                        EnvironmentExt.getArtistItemsPage(
                                                            BrowseEndpoint(
                                                                browseId = moreEndPointBrowseId,
                                                                params = moreEndPointParams!!
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
                            //}
                            songs
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ARTISTS_ONDEVICE -> {

                        if (id == "") {
                            Database
                                .artistsOnDevice(artistSortBy, artistSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ARTISTS_ONDEVICE) }
                                .toMutableList()
                        } else {
                            Database.artistTopSongs(id, 100)
                                .first()
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ARTISTS_IN_LIBRARY -> {

                        if (id == "") {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside artists in library id $id")
                            Database
                                .artistsInLibrary(artistSortBy, artistSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ARTISTS_IN_LIBRARY) }
                                .toMutableList()
                        } else {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside artist single in library id $id")
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
                                                    Timber.d("PlayerMediaBrowserService onLoadChildren artist in library songs moreEndPointBrowseId $moreEndPointBrowseId")
                                                }
                                            }
                                            .also {
                                                if (moreEndPointBrowseId != null)
                                                    if (artist != null) {
                                                        EnvironmentExt.getArtistItemsPage(
                                                            BrowseEndpoint(
                                                                browseId = moreEndPointBrowseId,
                                                                params = moreEndPointParams!!
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
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside artist single in library id $id with songs size ${songs.size}")
                            songs
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_FAVORITES -> {

                        if (id == "") {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside albums id $id")
                            Database
                                .albums(albumSortBy, albumSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_FAVORITES) }
                                .toMutableList()
                                .apply {
                                    if (showInLibraryAA())
                                        add(0, albumsInLibraryBrowserMediaItem)
                                    if (showOnDeviceAA())
                                        add(1, albumsOnDeviceBrowserMediaItem)
                                }
                        } else {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside albums SONGS id $id")
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
                                        Database.upsert(
                                            Album(
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
                                            ),
                                            innerSongsAlbumMap
                                        )
                                    }
                            }

                            songs
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_ON_DEVICE -> {

                        if (id == "") {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside albums on device id $id")
                            Database
                                .albumsOnDevice(albumSortBy, albumSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_ON_DEVICE) }
                                .toMutableList()
                        } else {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside albums SONGS id $id")
                            Database.albumSongs(id)
                                .first()
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_IN_LIBRARY -> {

                        if (id == "") {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside albums on device id $id")
                            Database
                                .albumsInLibrary(albumSortBy, albumSortOrder)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_IN_LIBRARY) }
                                .toMutableList()
                        } else {
                            Timber.d("PlayerMediaBrowserService onLoadChildren inside albums SONGS id $id")
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
                                        Database.upsert(
                                            Album(
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
                                            ),
                                            innerSongsAlbumMap
                                        )
                                    }
                            }

                            songs
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    // End Navigation items

                    // Start Browsable and playable items
                    MediaId.SHUFFLE -> lastSongs.shuffled().map { it.asBrowserMediaItem }.toMutableList()
                    MediaId.FAVORITES -> {

                        Database
                            .songsFavorites(songsSortBy, songSortOrder)
                            //.favorites()
                            .first()
                            .also { lastSongs = it.map { it.song }}
                            .map { it.song.asBrowserMediaItem }
                            .toMutableList()
                    }
                    MediaId.TOP -> {
                        val maxTopSongs = preferences.getEnum(MaxTopPlaylistItemsKey,
                            MaxTopPlaylistItems.`10`).number.toInt()

                        Database.trending(maxTopSongs)
                            .first()
                            .also { lastSongs = it }
                            .map { it.asBrowserMediaItem }.toMutableList()
                    }
                    MediaId.ONDEVICE -> {
                        Database
                            .songsOnDevice()
                            .first()
                            .also { lastSongs = it }
                            .map { it.asBrowserMediaItem }
                            .toMutableList()
                    }

                    // End Browsable and playable items

                    else -> mutableListOf()
                }
            )
        }
    }

    private fun uriFor(@DrawableRes id: Int) = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(id))
        .appendPath(resources.getResourceTypeName(id))
        .appendPath(resources.getResourceEntryName(id))
        .build()


    private val Song.asBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.forSong(id))
                .setTitle(title.removePrefix())
                .setSubtitle(artistsText)
                .setIconUri(thumbnailUrl?.toUri())
                .build(),
            MediaItem.FLAG_PLAYABLE
        )

    private val PlaylistPreview.asBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.forPlaylist(playlist.id))
                //.setTitle(playlist.name.substringAfter(PINNED_PREFIX))
                .setTitle(if (playlist.name.startsWith(PINNED_PREFIX)) playlist.name.replace(PINNED_PREFIX,"0:",true) else
                    if (playlist.name.startsWith(MONTHLY_PREFIX)) playlist.name.replace(
                        MONTHLY_PREFIX,"1:",true) else playlist.name.removePrefix())
                .setSubtitle("$songCount ${(this@PlayerMediaBrowserService as Context).resources.getString(R.string.songs)}")
                .setIconUri(uriFor(if (playlist.name.startsWith(PINNED_PREFIX)) R.drawable.pin else
                    if (playlist.name.startsWith(MONTHLY_PREFIX)) R.drawable.stat_month else R.drawable.playlist))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private fun Album.asBrowserMediaItem(type: String) =
        MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(
                    when(type) {
                        MediaId.ALBUMS_FAVORITES -> MediaId.forAlbumFavorites(id)
                        MediaId.ALBUMS_ON_DEVICE -> MediaId.forAlbumOnDevice(id)
                        MediaId.ALBUMS_IN_LIBRARY -> MediaId.forAlbumInLibrary(id)
                        else -> MediaId.forAlbumFavorites(id)
                    }
                )
                .setTitle(title?.removePrefix())
                .setSubtitle(authorsText)
                .setIconUri(thumbnailUrl?.toUri())
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private fun Artist.asBrowserMediaItem(type: String) =
        MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(
                    when(type) {
                        MediaId.ARTISTS_FAVORITES -> MediaId.forArtistFavorites(id)
                        MediaId.ARTISTS_ONDEVICE -> MediaId.forArtistOnDevice(id)
                        MediaId.ARTISTS_IN_LIBRARY -> MediaId.forArtistInLibrary(id)
                        else -> MediaId.forArtistFavorites(id)
                    }
                )
                .setTitle(name?.removePrefix())
                //.setSubtitle()
                .setIconUri(thumbnailUrl?.toUri())
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val MediaItem.asCleanMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(if (description.title.toString().startsWith("0:")) description.title.toString().substringAfter("0:") else
                    if (description.title.toString().startsWith("1:")) getTitleMonthlyPlaylist(description.title.toString().substringAfter("1:"), this@PlayerMediaBrowserService) else description.title.toString())
                .setIconUri(uriFor(if (description.title.toString().startsWith("0:")) R.drawable.pin else
                    if (description.title.toString().startsWith("1:")) R.drawable.stat_month else R.drawable.playlist))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val faultBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.FAULT)
                //.setTitle((this as Context).resources.getString(R.string.songs))
                .setTitle("Fault")
                .setIconUri(uriFor(R.drawable.close))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val songsBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.SONGS)
                .setTitle((this as Context).resources.getString(R.string.songs))
                .setIconUri(uriFor(R.drawable.musical_notes))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val playlistsBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.PLAYLISTS)
                .setTitle((this as Context).resources.getString(R.string.playlists))
                .setIconUri(uriFor(R.drawable.music_library))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val albumsFavoritesBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ALBUMS_FAVORITES)
                .setTitle((this as Context).resources.getString(R.string.albums))
                .setIconUri(uriFor(R.drawable.music_album))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val albumsInLibraryBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ALBUMS_IN_LIBRARY)
                .setTitle((this as Context).resources.getString(R.string.library))
                .setIconUri(uriFor(R.drawable.music_album))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val albumsOnDeviceBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ALBUMS_ON_DEVICE)
                .setTitle((this as Context).resources.getString(R.string.on_device))
                .setIconUri(uriFor(R.drawable.music_album))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val artistsFavoritesBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ARTISTS_FAVORITES)
                .setTitle((this as Context).resources.getString(R.string.artists))
                .setIconUri(uriFor(R.drawable.music_artist))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val artistsInLibraryBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ARTISTS_IN_LIBRARY)
                .setTitle((this as Context).resources.getString(R.string.library))
                .setIconUri(uriFor(R.drawable.music_artist))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val artistsOnDeviceBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ARTISTS_ONDEVICE)
                .setTitle((this as Context).resources.getString(R.string.on_device))
                .setIconUri(uriFor(R.drawable.music_artist))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val shuffleBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.SHUFFLE)
                .setTitle((this as Context).resources.getString(R.string.shuffle))
                .setIconUri(uriFor(R.drawable.shuffle))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val favoritesBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.FAVORITES)
                .setTitle((this as Context).resources.getString(R.string.favorites))
                .setIconUri(uriFor(R.drawable.heart))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val topBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.TOP)
                .setTitle((this as Context).resources.getString(R.string.my_playlist_top)
                    .format((this as Context).preferences.getEnum(MaxTopPlaylistItemsKey,
                        MaxTopPlaylistItems.`10`).number))
                .setIconUri(uriFor(R.drawable.trending))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val ondeviceBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ONDEVICE)
                .setTitle((this as Context).resources.getString(R.string.on_device))
                .setIconUri(uriFor(R.drawable.musical_notes))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    fun reloadPlaylist(){
        notifyChildrenChanged(MediaId.PLAYLISTS)
    }

    object MediaId {
        const val FAULT = "fault"
        const val ROOT = "root"
        const val SONGS = "songs"
        const val PLAYLISTS = "playlists"
        const val ALBUMS_FAVORITES = "albumsFavorites"
        const val ALBUMS_IN_LIBRARY = "albumsInLibrary"
        const val ALBUMS_ON_DEVICE = "albumsOnDevice"
        const val ARTISTS_FAVORITES = "artistsFavorites"
        const val ARTISTS_IN_LIBRARY = "artistsInLibrary"
        const val ARTISTS_ONDEVICE = "artistsOnDevice"

        const val SEARCHED = "searched"

        const val FAVORITES = "favorites"
        const val SHUFFLE = "shuffle"
        const val ONDEVICE = "ondevice"
        const val TOP = "top"

        fun forSong(id: String) = "$SONGS/$id"
        fun forPlaylist(id: Long) = "$PLAYLISTS/$id"
        fun forAlbumFavorites(id: String) = "$ALBUMS_FAVORITES/$id"
        fun forAlbumInLibrary(id: String) = "$ALBUMS_IN_LIBRARY/$id"
        fun forAlbumOnDevice(id: String) = "$ALBUMS_ON_DEVICE/$id"
        fun forArtistFavorites(id: String) = "$ARTISTS_FAVORITES/$id"
        fun forArtistInLibrary(id: String) = "$ARTISTS_IN_LIBRARY/$id"
        fun forArtistOnDevice(id: String) = "$ARTISTS_ONDEVICE/$id"

        fun forSearched(id: String) = "$SEARCHED/$id"
    }
}

private const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
private const val CONTENT_STYLE_LIST = 1
private const val CONTENT_STYLE_GRID = 2