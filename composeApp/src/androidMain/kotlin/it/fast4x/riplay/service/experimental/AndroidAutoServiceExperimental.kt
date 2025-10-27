package it.fast4x.riplay.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastFilter
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.completed
import it.fast4x.environment.utils.from
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.MODIFIED_PREFIX
import it.fast4x.riplay.MONTHLY_PREFIX
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.ONLINEPLAYER_NOTIFICATION_CHANNEL
import it.fast4x.riplay.PINNED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.context
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.removePrefix
import it.fast4x.riplay.enums.MaxTopPlaylistItems
import it.fast4x.riplay.enums.NotificationButtons
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.enums.QueueLoopType
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.history.updateOnlineHistory
import it.fast4x.riplay.extensions.preferences.MaxTopPlaylistItemsKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.notificationPlayerFirstIconKey
import it.fast4x.riplay.extensions.preferences.notificationPlayerSecondIconKey
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.extensions.preferences.putEnum
import it.fast4x.riplay.extensions.preferences.queueLoopTypeKey
import it.fast4x.riplay.utils.isAppRunning
import it.fast4x.riplay.utils.isSkipMediaOnErrorEnabled
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.utils.showFavoritesPlaylistsAA
import it.fast4x.riplay.utils.showGridAA
import it.fast4x.riplay.utils.showInLibraryAA
import it.fast4x.riplay.utils.showMonthlyPlaylistsAA
import it.fast4x.riplay.utils.showOnDeviceAA
import it.fast4x.riplay.utils.showTopPlaylistAA
import it.fast4x.riplay.utils.shuffleSongsAAEnabled
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.player.online.components.customui.CustomDefaultPlayerUiController
import it.fast4x.riplay.utils.BitmapProvider
import it.fast4x.riplay.utils.getTitleMonthlyPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.isAtLeastAndroid12
import kotlin.math.roundToInt
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.cleaned
import it.fast4x.riplay.utils.clearWebViewData
import it.fast4x.riplay.utils.forcePlayAtIndex
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8
import it.fast4x.riplay.utils.mediaItemToggleLike
import it.fast4x.riplay.utils.playNext
import it.fast4x.riplay.utils.playPrevious
import it.fast4x.riplay.utils.seamlessQueue
import it.fast4x.riplay.utils.setQueueLoopState
import it.fast4x.riplay.utils.shuffleQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.first


@UnstableApi
class AndroidAutoServiceExperimental : MediaBrowserServiceCompat(), ServiceConnection, Player.Listener {

//    var _internalLocalPlayerBinder: LocalPlayerService.Binder? = null
//        set(value) {
//            internalLocalPlayerBinder = value
//        }
//
//    var _internalMediaSession: MediaSessionCompat? = null
//        set(value) {
//            internalMediaSession = value
//        }

    var internalMediaSession: MediaSessionCompat? = null
    var internalLocalPlayerBinder: LocalPlayerService.Binder? = null
    var internalBitmapProvider: BitmapProvider? = null
    var isPlayingNow: Boolean = false

    var currentSecond: MutableState<Float> = mutableFloatStateOf(0f)
    var currentDuration: MutableState<Float> = androidx.compose.runtime.mutableFloatStateOf(0f)
//    var internalOnlinePlayerView: MutableState<YouTubePlayerView> = mutableStateOf(
//        LayoutInflater.from(appContext())
//            .inflate(R.layout.youtube_player, null, false)
//                as YouTubePlayerView
//    )
    var internalOnlinePlayerView: MutableState<YouTubePlayerView?> = mutableStateOf(null)
    var internalOnlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)

    var internalOnlinePlayerState: PlayerConstants.PlayerState = PlayerConstants.PlayerState.UNSTARTED
    var localMediaItem: androidx.media3.common.MediaItem? = null
    var load = true
    var playFromSecond = 0f
    var lastError: PlayerConstants.PlayerError? = null

    //var isRunning = false
    var isAppRunning = false

    //var currentMediaItem: androidx.media3.common.MediaItem? = null


    /**
     * Returns the instance of the service
     */
    inner class LocalBinder : Binder() {
        val serviceInstance: AndroidAutoServiceExperimental
            get() = this@AndroidAutoServiceExperimental

        var mediaSessionInjected: MediaSessionCompat? = null
            set(value) {
                this@AndroidAutoServiceExperimental.internalMediaSession = value
            }

        var localPlayerBinderInjected: LocalPlayerService.Binder? = null
            set(value) {
                this@AndroidAutoServiceExperimental.internalLocalPlayerBinder = value
            }

    }

    private val mBinder: IBinder = LocalBinder() // IBinder

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("AndroidAutoService onBind called with intent ${intent?.action}")
        if ( SERVICE_INTERFACE == intent!!.action ) {
            return super.onBind(intent)
        }
        Timber.d("AndroidAutoService onBind process intent ${intent.action}")

        return mBinder
    }

//    override fun onCreate() {
//        super.onCreate()
//        isAppRunning = isAppRunning()
//
//        if (!isAppRunning) {
//            initializeBitmapProvider()
//            initializeOnlinePlayer()
//            initializeMediaSession()
//            //initializePlayerListener()
//
//        }
//
//        Timber.d("AndroidAutoService onCreate")
//    }

    override fun onDestroy() {
        unbindService(this)
        super.onDestroy()
        Timber.d("AndroidAutoService onDestroy")
    }


//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Timber.d("AndroidAutoService onStartCommand")
//        MediaButtonReceiver.handleIntent(internalMediaSession, intent)
//
//        isRunning = true
//        return START_STICKY // If the service is killed, it will be automatically restarted
//    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startNotification(){
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, this.notification)

//        ServiceCompat.startForeground(
//            this,
//            NOTIFICATION_ID,
//            notification,
//            if (isAtLeastAndroid11) {
//                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
//            } else {
//                0
//            }
//        )
    }

    fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            NOTIFICATION_ID.toString(),
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(ONLINEPLAYER_NOTIFICATION_CHANNEL)
            .setShowBadge(false)
            .build()

        NotificationManagerCompat.from(this@AndroidAutoServiceExperimental).createNotificationChannel(channel)
    }

    private val notification: Notification
        @OptIn(UnstableApi::class)
        @RequiresApi(Build.VERSION_CODES.O)
        get() {

            val startIntent = Intent(appContext(), MainActivity::class.java)
            startIntent.action = Intent.ACTION_MAIN
            startIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            val contentIntent: PendingIntent? =
                PendingIntent.getActivity(appContext(), 1, startIntent, if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)


            return if (isAtLeastAndroid8) {
                NotificationCompat.Builder(appContext(), ONLINEPLAYER_NOTIFICATION_CHANNEL)
            } else {
                NotificationCompat.Builder(appContext())
            }
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("RiPlay Android Auto")
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText("RiPlay must be started before AA, click here to start RiPlay now.")
                .setContentIntent(contentIntent)
                .setSilent(false)
                .setAutoCancel(false)
                .build()

        }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
            Timber.d("AndroidAutoService onGetRoot $clientPackageName app is running ? ${isAppRunning()} try to bind services")
            bindService(Intent(this, LocalPlayerService::class.java), this, 0)
            bindService(Intent(this, AndroidAutoService::class.java), this, 0)


            return BrowserRoot(
                MediaId.ROOT,
                Bundle().apply {
                    putBoolean(MEDIA_SEARCH_SUPPORTED, true)
                    putBoolean(CONTENT_STYLE_SUPPORTED, true)
                    putInt(
                        CONTENT_STYLE_BROWSABLE_HINT,
                        if (showGridAA()) CONTENT_STYLE_GRID else CONTENT_STYLE_LIST
                    )
                    putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
                }
            )
    }


    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem?>?>
    ) {
        val data = parentId.split('/')
        val id = data.getOrNull(1) ?: ""
        Timber.d("AndroidAutoService onLoadChildren $parentId data $data")
        runBlocking(Dispatchers.IO) {
            result.sendResult(
                when (data.firstOrNull()) {

                    MediaId.FAULT ->listOf(
                        faultBrowserMediaItem
                    )

                    // Start Navigation items
                    MediaId.ROOT -> listOf(
                        songsBrowserMediaItem,
                        playlistsBrowserMediaItem,
                        albumsFavoritesBrowserMediaItem,
                        artistsFavoritesBrowserMediaItem
                    )

                    MediaId.SONGS -> Database
                        .songsByPlayTimeDesc()
                        .first()
                        .take(500)
                        .also { lastSongs = it.map { it.song } }
                        .map { it.song.asBrowserMediaItem }
                        .toMutableList()
                        .apply {
                            if (shuffleSongsAAEnabled() && isNotEmpty()) add(0, shuffleBrowserMediaItem)
                        }

                    MediaId.PLAYLISTS -> {
                        if (id == "") {
                            Database
                                .playlistPreviewsByNameAsc()
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
                            Database.playlistWithSongs(id.toLong())
                                .first()
                                ?.songs
                                .also { lastSongs = it ?: emptyList() }
                                ?.map { it.asBrowserMediaItem }
                                ?.toMutableList()
                        }

                    }

                    MediaId.ARTISTS_FAVORITES -> {
                        if (id == "") {
                            Database
                                .artists(ArtistSortBy.Name, SortOrder.Ascending)
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
                                                    println("Android Auto onGetchildren artist songs moreEndPointBrowseId $moreEndPointBrowseId")
                                                }
                                            }
                                            .also {
                                                if (moreEndPointBrowseId != null)
                                                    if (artist != null) {
                                                        EnvironmentExt.getArtistItemsPage(
                                                            BrowseEndpoint(
                                                                browseId = moreEndPointBrowseId!!,
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
                                                                    artistId = artist!!.id
                                                                )
                                                            }
                                                            ?.onEach(Database::insert)
                                                    }

                                            }

                                    }
                            }
                            songs
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ARTISTS_ONDEVICE -> {
                        if (id == "") {
                            Database
                                .artistsOnDevice(ArtistSortBy.Name, SortOrder.Ascending)
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
                            Timber.d("AndroidAutoService onLoadChildren inside artists in library id $id")
                            Database
                                .artistsInLibrary(ArtistSortBy.Name, SortOrder.Ascending)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ARTISTS_IN_LIBRARY) }
                                .toMutableList()
                        } else {
                            Timber.d("AndroidAutoService onLoadChildren inside artist single in library id $id")
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
                                                    println("AndroidAutoService onLoadChildren artist in library songs moreEndPointBrowseId $moreEndPointBrowseId")
                                                }
                                            }
                                            .also {
                                                if (moreEndPointBrowseId != null)
                                                    if (artist != null) {
                                                        EnvironmentExt.getArtistItemsPage(
                                                            BrowseEndpoint(
                                                                browseId = moreEndPointBrowseId!!,
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
                                                                    artistId = artist!!.id
                                                                )
                                                            }
                                                            ?.onEach(Database::insert)
                                                    }

                                            }

                                    }
                            }
                            Timber.d("AndroidAutoService onLoadChildren inside artist single in library id $id with songs size ${songs.size}")
                            songs
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_FAVORITES -> {
                        if (id == "") {
                            Timber.d("AndroidAutoService onLoadChildren inside albums id $id")
                            Database
                                .albums(AlbumSortBy.Title, SortOrder.Ascending)
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
                            Timber.d("AndroidAutoService onLoadChildren inside albums SONGS id $id")
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
                            Timber.d("AndroidAutoService onLoadChildren inside albums on device id $id")
                            Database
                                .albumsOnDevice(AlbumSortBy.Title, SortOrder.Ascending)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_ON_DEVICE) }
                                .toMutableList()
                        } else {
                            Timber.d("AndroidAutoService onLoadChildren inside albums SONGS id $id")
                            Database.albumSongs(id)
                                .first()
                                .also { lastSongs = it }
                                .map { it.asBrowserMediaItem }
                                .toMutableList()
                        }
                    }

                    MediaId.ALBUMS_IN_LIBRARY -> {
                        if (id == "") {
                            Timber.d("AndroidAutoService onLoadChildren inside albums on device id $id")
                            Database
                                .albumsInLibrary(AlbumSortBy.Title, SortOrder.Ascending)
                                .first()
                                .map { it.asBrowserMediaItem(MediaId.ALBUMS_IN_LIBRARY) }
                                .toMutableList()
                        } else {
                            Timber.d("AndroidAutoService onLoadChildren inside albums SONGS id $id")
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
                    MediaId.SHUFFLE -> lastSongs.shuffled().map { it?.asBrowserMediaItem }.toMutableList()
                    MediaId.FAVORITES -> Database
                        .favorites()
                        .first()
                        .also { lastSongs = it }
                        .map { it.asBrowserMediaItem }
                        .toMutableList()
                    MediaId.TOP -> {
                        val maxTopSongs = preferences.getEnum(MaxTopPlaylistItemsKey,
                            MaxTopPlaylistItems.`10`).number.toInt()

                        Database.trending(maxTopSongs)
                            .first()
                            .also { lastSongs = it }
                            .map { it.asBrowserMediaItem }.toMutableList()
                    }
                    MediaId.ONDEVICE -> Database
                        .songsOnDevice()
                        .first()
                        .also { lastSongs = it }
                        .map { it.asBrowserMediaItem }
                        .toMutableList()

                    // End Browsable and playable items

                    else -> mutableListOf()
                }
            )
        }
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
                //it.asBrowserMediaItem
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
                .setSubtitle("$songCount ${(this@AndroidAutoServiceExperimental as Context).resources.getString(R.string.songs)}")
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
                    if (description.title.toString().startsWith("1:")) getTitleMonthlyPlaylist(description.title.toString().substringAfter("1:"), this@AndroidAutoServiceExperimental) else description.title.toString())
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
                .setIconUri(uriFor(R.drawable.playlist))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val albumsFavoritesBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ALBUMS_FAVORITES)
                .setTitle((this as Context).resources.getString(R.string.albums))
                .setIconUri(uriFor(R.drawable.album))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val albumsInLibraryBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ALBUMS_IN_LIBRARY)
                .setTitle((this as Context).resources.getString(R.string.library))
                .setIconUri(uriFor(R.drawable.album))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val albumsOnDeviceBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ALBUMS_ON_DEVICE)
                .setTitle((this as Context).resources.getString(R.string.on_device))
                .setIconUri(uriFor(R.drawable.album))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val artistsFavoritesBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ARTISTS_FAVORITES)
                .setTitle((this as Context).resources.getString(R.string.artists))
                .setIconUri(uriFor(R.drawable.artists))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val artistsInLibraryBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ARTISTS_IN_LIBRARY)
                .setTitle((this as Context).resources.getString(R.string.library))
                .setIconUri(uriFor(R.drawable.artists))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

    private val artistsOnDeviceBrowserMediaItem
        inline get() = MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.ARTISTS_ONDEVICE)
                .setTitle((this as Context).resources.getString(R.string.on_device))
                .setIconUri(uriFor(R.drawable.artists))
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



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onServiceConnected(className: ComponentName?, service: IBinder?) {

        if (service !is LocalBinder && service !is LocalPlayerService.Binder) return

        isAppRunning = isAppRunning()

        Timber.d("AndroidAutoService onServiceConnected service ${service.javaClass}")

        if (service is LocalBinder) {
            if (internalMediaSession?.sessionToken != null) {
                sessionToken = internalMediaSession?.sessionToken
                Timber.d("AndroidAutoService onServiceConnected set sessionToken $sessionToken")
            }
            updateOnlineNotification()
        }

        if (service is LocalPlayerService.Binder) {
            internalLocalPlayerBinder = service
            //service.player.addListener(this@AndroidAutoService)
            localMediaItem = service.player.currentMediaItem

            if (!isAppRunning) {
                initializeBitmapProvider()
                initializeOnlinePlayer()

                //internalOnlinePlayer.value?.let{ initializeMediaSession(service, it, service.player.currentMediaItem) }
            }

            updateOnlineNotification()
        }

        //if (isNotifyAndroidAutoTipsEnabled())
            //startNotification()

        Timber.d("AndroidAutoService onServiceConnected")
    }

    override fun onServiceDisconnected(name: ComponentName) = Unit


    private fun initializeBitmapProvider() {
        runCatching {
            internalBitmapProvider = BitmapProvider(
                bitmapSize = (512 * resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) Color.BLACK else Color.WHITE
                }
            )
        }.onFailure {
            Timber.e("Failed init bitmap provider in PlayerService ${it.stackTraceToString()}")
        }
    }

    private fun initializeOnlinePlayer() {

        internalOnlinePlayerView.value = null

        internalOnlinePlayerView.value = LayoutInflater.from(appContext())
            .inflate(R.layout.youtube_player, null, false)
                as YouTubePlayerView

        if (internalOnlinePlayerView.value == null) return

        internalOnlinePlayerView.value.apply {
            this?.enableAutomaticInitialization = false


            this!!.enableBackgroundPlayback(true)

            keepScreenOn = false

            val iFramePlayerOptions = IFramePlayerOptions.Builder(appContext())
                .controls(0) // show/hide controls
                .listType("playlist")
                .origin(resources.getString(R.string.env_fqqhBZd0cf))
                .build()

            val listener = object : AbstractYouTubePlayerListener() {

                override fun onReady(youTubePlayer: YouTubePlayer) {
                    super.onReady(youTubePlayer)
                    internalOnlinePlayer.value = youTubePlayer
                    Timber.d("AndroidAutoService onlinePlayerView onReady")
                    //youTubePlayer.loadVideo("JEJNFu9bbqo", 0f)

                    val customUiController =
                        CustomDefaultPlayerUiController(
                            context,
                            internalOnlinePlayerView.value!!,
                            youTubePlayer,
                            onTap = {}
                        )
                    customUiController.showUi(false) // disable all default controls and buttons
                    customUiController.showMenuButton(false)
                    customUiController.showVideoTitle(false)
                    customUiController.showPlayPauseButton(false)
                    customUiController.showDuration(false)
                    customUiController.showCurrentTime(false)
                    customUiController.showSeekBar(false)
                    customUiController.showBufferingProgress(false)
                    customUiController.showYouTubeButton(false)
                    customUiController.showFullscreenButton(false)
                    internalOnlinePlayerView.value!!.setCustomPlayerUi(customUiController.rootView)

                    Timber.d("AndroidAutoService onlinePlayerView: onReady shouldBePlaying: $isPlayingNow internaleOnlinePlayer $internalOnlinePlayer")
                    if (localMediaItem != null) {
                        if (!load)
                            youTubePlayer.cueVideo(localMediaItem!!.mediaId, playFromSecond)
                        else
                            youTubePlayer.loadVideo(localMediaItem!!.mediaId, playFromSecond)
                    }
                    updateOnlineNotification()
                }

                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    super.onCurrentSecond(youTubePlayer, second)
                    currentSecond.value = second
                    //Timber.d("AndroidAutoService onlinePlayerView: onCurrentSecond $second")
                }

                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                    super.onVideoDuration(youTubePlayer, duration)
                    currentDuration.value = duration
                    updateOnlineNotification()
                }

                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    super.onStateChange(youTubePlayer, state)

//                    val fadeDisabled = getPlaybackFadeAudioDuration() == DurationInMilliseconds.Disabled
//                    val duration = getPlaybackFadeAudioDuration().milliSeconds
//                    if (!fadeDisabled)
//                        startFadeAnimator(
//                            player = player,
//                            duration = duration,
//                            fadeIn = true
//                        )

                    internalOnlinePlayerState = state
                    isPlayingNow = state == PlayerConstants.PlayerState.PLAYING
                    updateOnlineNotification()
                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    super.onError(youTubePlayer, error)

                    localMediaItem?.isLocal?.let { if (it) return }

                    youTubePlayer.pause()
                    clearWebViewData()

                    Timber.e("AndroidAutoService: onError $error")
                    val errorString = when (error) {
                        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> "Content not playable, recovery in progress, try to click play but if the error persists try to log in"
                        PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Content not found, perhaps no longer available"
                        else -> null
                    }

                    if (errorString != null && lastError != error) {
                        SmartMessage(
                            errorString,
                            PopupType.Error,
                            //durationLong = true,
                            context = context()
                        )
                        localMediaItem?.let { youTubePlayer.cueVideo(it.mediaId, 0f) }

                    }

                    lastError = error

                    if (!isSkipMediaOnErrorEnabled()) return
                    val prev = internalLocalPlayerBinder?.player?.currentMediaItem ?: return

                    internalLocalPlayerBinder!!.player.playNext()

                    SmartMessage(
                        message = context().getString(
                            R.string.skip_media_on_error_message,
                            prev.mediaMetadata.title
                        ),
                        context = context(),
                    )

                }

            }

            initialize(listener, iFramePlayerOptions)

        }

    }


    private fun initializePlayerListener() {
        /*
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Timber.d("AndroidAutoService Player.Listener onIsPlayingChanged isPlaying $isPlaying")
                isPlayingNow = isPlaying
                updateMediasessionData()
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                Timber.d("AndroidAutoService Player.Listener onMediaItemTransition mediaItem $mediaItem reason $reason")

                mediaItem?.let {
                    //if (it.isLocal) return // todo check if local can be player here

                    currentSecond.value = 0F
                    localMediaItem = it
                    //lastVideoId.value = it.mediaId
                    internalOnlinePlayer.value?.loadVideo(it.mediaId, 0f)
                    updateOnlineHistory(it)
                    internalBitmapProvider?.load(it.mediaMetadata.artworkUri) {}

                    updateOnlineNotification()

                }

            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                updateOnlineNotification()
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
                updateOnlineNotification()
            }

        }

        internalLocalPlayerBinder?.player?.addListener(listener)

         */

        //internalLocalPlayerBinder?.player?.addListener(this@AndroidAutoService)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Timber.d("AndroidAutoService Player.Listener onIsPlayingChanged isPlaying $isPlaying")
        isPlayingNow = isPlaying
        updateMediasessionData()
    }
    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        Timber.d("AndroidAutoService Player.Listener onMediaItemTransition mediaItem $mediaItem reason $reason")

        mediaItem?.let {
            //if (it.isLocal) return // todo check if local can be player here

            currentSecond.value = 0F
            localMediaItem = it
            //lastVideoId.value = it.mediaId
            internalOnlinePlayer.value?.loadVideo(it.mediaId, 0f)
            updateOnlineHistory(it)
            internalBitmapProvider?.load(it.mediaMetadata.artworkUri) {}

            updateOnlineNotification()

        }

    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        updateOnlineNotification()
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        updateOnlineNotification()
    }


    // MEDIASESSION
    private fun initializeMediaSession(
        serviceBinder: LocalPlayerService.Binder,
        sessionOnlinePlayer: YouTubePlayer,
        localMediaItem: androidx.media3.common.MediaItem? = null
    ) {

        isAppRunning = isAppRunning()

        if (internalMediaSession == null)
            internalMediaSession = MediaSessionCompat(this, "OnlinePlayerAA")

        //sessionToken = internalMediaSession?.sessionToken

        if (!isAppRunning) // if app is running, mediasession is passed by activity
            internalMediaSession?.setCallback(MediaSessionCallback(serviceBinder, sessionOnlinePlayer, localMediaItem))

        val repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        internalMediaSession?.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        internalMediaSession?.setRepeatMode(repeatMode)
        internalMediaSession?.isActive = true

        updateOnlineNotification()

        Timber.d("AndroidAutoService initializeMediaSession internalMediaSession $internalMediaSession isAppRunning $isAppRunning")
    }

    @UnstableApi
    fun updateOnlineNotification() {

        /*

        val currentMediaItem = internalLocalPlayerBinder?.player?.currentMediaItem
        Timber.d("AndroidAutoService updateOnlineNotification currentMediaItem ${currentMediaItem?.mediaId}")
        if (currentMediaItem?.isLocal == true) return

        //if (bitmapProvider?.bitmap == null)
        //    runBlocking {
        internalBitmapProvider?.load(currentMediaItem?.mediaMetadata?.artworkUri) {}
        //    }


        updateMediasessionData()

        createNotificationChannel()

        val forwardAction = NotificationCompat.Action.Builder(
            R.drawable.play_skip_forward,
            "next",
            Action.next.pendingIntent
        ).build()

        val playPauseAction = NotificationCompat.Action.Builder(
            if (isPlayingNow) R.drawable.pause else R.drawable.play,
            if (isPlayingNow) "pause" else "play",
            if (isPlayingNow) Action.pause.pendingIntent
            else Action.play.pendingIntent,
        ).build()

        val previousAction = NotificationCompat.Action.Builder(
            R.drawable.play_skip_back,
            "prev",
            Action.previous.pendingIntent
        ).build()


        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        val firstCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerFirstIcon }
            .map {
                NotificationCompat.Action.Builder(
                    it.getStateIcon(
                        it,
                        internalLocalPlayerBinder?.currentMediaItemAsSong?.likedAt,
                        internalLocalPlayerBinder?.player?.repeatMode ?: 0,
                        internalLocalPlayerBinder?.player?.shuffleModeEnabled ?: false
                    ),
                    it.name,
                    it.pendingIntentOnline,
                ).build()
            }.first()


        val secondCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerSecondIcon }
            .map {
                NotificationCompat.Action.Builder(
                    it.getStateIcon(
                        it,
                        internalLocalPlayerBinder?.currentMediaItemAsSong?.likedAt,
                        internalLocalPlayerBinder?.player?.repeatMode ?: 0,
                        internalLocalPlayerBinder?.player?.shuffleModeEnabled ?: false
                    ),
                    it.name,
                    it.pendingIntentOnline,
                ).build()
            }.first()


        val notification = if (isAtLeastAndroid8) {
            NotificationCompat.Builder(this, ONLINEPLAYER_NOTIFICATION_CHANNEL)
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle(currentMediaItem?.mediaMetadata?.title)
            .setContentText(currentMediaItem?.mediaMetadata?.artist)
            //.setSubText(currentMediaItem?.mediaMetadata?.artist)
            .setContentInfo(currentMediaItem?.mediaMetadata?.albumTitle)
            .setSmallIcon(R.drawable.app_icon)
            .setLargeIcon(internalBitmapProvider?.bitmap)
            .setShowWhen(false)
            .setSilent(true)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(firstCustomAction)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(forwardAction)
            .addAction(secondCustomAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1, 2, 3)
                    .setMediaSession(internalMediaSession?.sessionToken)

            )
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java)
                        .putExtra("expandPlayerBottomSheet", true),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()

        //workaround for android 12+
//        runCatching {
//            notification.let {
//                ServiceCompat.startForeground(
//                    toolsService,
//                    NOTIFICATION_ID,
//                    it,
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
//                    } else {
//                        0
//                    }
//                )
//            }
//        }.onFailure {
//            Timber.e("PlayerService oncreate startForeground ${it.stackTraceToString()}")
//        }

        NotificationManagerCompat.from(this@AndroidAutoServiceExperimental).notify(NOTIFICATION_ID, notification)

         */
    }


    private fun updateMediasessionData() {
        Timber.d("AndroidAutoService initializeMediasession")
        val currentMediaItem = internalLocalPlayerBinder?.player?.currentMediaItem
        //val queueLoopType = preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)

        internalMediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    currentMediaItem?.mediaId
                )
                .putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    internalBitmapProvider?.bitmap
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    currentMediaItem?.mediaMetadata?.title.toString()
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    currentMediaItem?.mediaMetadata?.artist.toString()
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM,
                    currentMediaItem?.mediaMetadata?.albumTitle.toString()
                )
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (currentDuration.value * 1000).toLong())
                .build()
        )

        Timber.d("AndroidAutoService updateMediasessionData onlineplayer playing ${isPlayingNow} localplayer playing ${internalLocalPlayerBinder?.player?.isPlaying}")

        val actions =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO

        // todo Improve custom actions in online player notification
        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Repeat)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        val firstCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerFirstIcon }
            .map {
                PlaybackStateCompat.CustomAction.Builder(
                    it.action,
                    it.name,
                    it.getStateIcon(
                        it,
                        internalLocalPlayerBinder?.currentMediaItemAsSong?.likedAt,
                        internalLocalPlayerBinder?.player?.repeatMode ?: 0,
                        internalLocalPlayerBinder?.player?.shuffleModeEnabled ?: false
                    ),
                ).build()
            }.first()


        val secondCustomAction = NotificationButtons.entries
            .filter { it == notificationPlayerSecondIcon }
            .map {
                PlaybackStateCompat.CustomAction.Builder(
                    it.action,
                    it.name,
                    it.getStateIcon(
                        it,
                        internalLocalPlayerBinder?.currentMediaItemAsSong?.likedAt,
                        internalLocalPlayerBinder?.player?.repeatMode ?: 0,
                        internalLocalPlayerBinder?.player?.shuffleModeEnabled ?: false
                    ),
                ).build()
            }.first()


        internalMediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder().setActions(actions.let {
                if (isAtLeastAndroid12) it or PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED else it
            })
                .apply {
                    addCustomAction(firstCustomAction)
                    addCustomAction(secondCustomAction)
                    setState(
                        if (isPlayingNow || internalLocalPlayerBinder?.player?.isPlaying == true)
                            PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        (currentSecond.value * 1000).toLong(), //currentPlaybackPosition.value,
                        1f
                    )
                }
                .build()
        )

    }

    private inner class MediaSessionCallback(
        private val sessionBinder: LocalPlayerService.Binder,
        private val sessionOnlinePlayer: YouTubePlayer,
        private var sessionLocalMediaItem: androidx.media3.common.MediaItem? = null,
    ) : MediaSessionCompat.Callback() {
        val sessionCurrentMediaItem = sessionBinder.player.currentMediaItem
        val queueLoopType = appContext().preferences.getEnum(queueLoopTypeKey, defaultValue = QueueLoopType.Default)
        override fun onPlay() {
            Timber.d("AndroidAutoService MediaSessionCallback onPlay() localMediaItem ${sessionCurrentMediaItem?.mediaId}")
            if (sessionLocalMediaItem?.isLocal == true) {
                sessionOnlinePlayer.pause()
                sessionBinder.player.play()
            }
            else if (sessionLocalMediaItem != null) {
                sessionBinder.player.pause()
                sessionOnlinePlayer.play()
            }
        }
        override fun onPause() {
            Timber.d("AndroidAutoService MediaSessionCallback onPause()")
            sessionBinder.player.pause()
            sessionOnlinePlayer.pause()
        }
        override fun onSkipToNext() {
            Timber.d("AndroidAutoService MediaSessionCallback onSkipToNext()")
            sessionBinder.player.playNext()
        }
        override fun onSkipToPrevious() {
            Timber.d("AndroidAutoService MediaSessionCallback onSkipToPrevious()")
            sessionBinder.player.playPrevious()
        }
        override fun onStop() {
            Timber.d("AndroidAutoService MediaSessionCallback onStop()")
            sessionBinder.player.stop()
            sessionOnlinePlayer.pause()
        }
        override fun onSeekTo(pos: Long) {
            Timber.d("AndroidAutoService MediaSessionCallback onSeekTo()")
            val newPosition = (pos / 1000).toFloat()
            sessionOnlinePlayer.seekTo(newPosition)
            currentSecond.value = newPosition
        }
        override fun onCustomAction(action: String?, extras: Bundle?) {
            Timber.d("AndroidAutoService MediaSessionCallback onCustomAction()")
            sessionBinder.let {
                when (action) {
                    NotificationButtons.Favorites.action -> {
                        if (sessionCurrentMediaItem != null)
                            mediaItemToggleLike(sessionCurrentMediaItem)
                    }

                    NotificationButtons.Repeat.action -> {
                        appContext().preferences.edit(commit = true) {
                            putEnum(
                                queueLoopTypeKey,
                                setQueueLoopState(queueLoopType)
                            )
                        }
                    }

                    NotificationButtons.Shuffle.action -> {
                        it.player.shuffleQueue()
                    }

                    NotificationButtons.Radio.action -> {
                        if (sessionCurrentMediaItem != null) {
                            it.stopRadio()
                            it.player.seamlessQueue(sessionCurrentMediaItem)
                            sessionOnlinePlayer.play()
                            it.setupRadio(
                                NavigationEndpoint.Endpoint.Watch(videoId = sessionCurrentMediaItem.mediaId)
                            )
                        }
                    }

                    NotificationButtons.Search.action -> {
                        it.actionSearch()
                    }
                }
            }
            updateOnlineNotification()
        }
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Timber.d("AndroidAutoService MediaSessionCallback onPlayFromMediaId()")
            val data = mediaId?.split('/') ?: return
            var index = 0
            //var mediaItemSelected: MediaItem? = null

            Timber.d("AndroidAutoService onPlayFromMediaId mediaId ${mediaId} data $data processing")

            CoroutineScope(Dispatchers.IO).launch {
                val mediaItems = when (data.getOrNull(0)) {

                    MediaId.SONGS ->  data
                        .getOrNull(1)
                        ?.let { songId ->
                            index = lastSongs.indexOfFirst { it.id == songId }

                            if (index < 0) return@launch // index not found

                            sessionLocalMediaItem = lastSongs[index].asMediaItem
                            lastSongs
                        }
                        .also { Timber.d("MediaSessionCallback onPlayFromMediaId processing songs, mediaId ${mediaId} index $index songs ${it?.size}") }

                    MediaId.SEARCHED -> data
                        .getOrNull(1)
                        ?.let { songId ->
                            index = searchedSongs.indexOfFirst { it.id == songId }

                            if (index < 0) return@launch // index not found

                            //mediaItemSelected = searchedSongs[index].asMediaItem
                            searchedSongs

                        }

                    // Maybe it needed in the future
                    /*
                    AndroidAutoService.MediaId.shuffle -> lastSongs.shuffled()

                    AndroidAutoService.MediaId.favorites -> Database
                        .favorites()
                        .first()

                    AndroidAutoService.MediaId.ondevice -> Database
                        .songsOnDevice()
                        .first()

                    AndroidAutoService.MediaId.top -> {
                        val maxTopSongs = context().preferences.getEnum(MaxTopPlaylistItemsKey,
                            MaxTopPlaylistItems.`10`).number.toInt()

                        Database.trending(maxTopSongs)
                            .first()
                    }

                    AndroidAutoService.MediaId.playlists -> data
                        .getOrNull(1)
                        ?.toLongOrNull()
                        ?.let(Database::playlistWithSongs)
                        ?.first()
                        ?.songs

                    AndroidAutoService.MediaId.albums -> data
                        .getOrNull(1)
                        ?.let(Database::albumSongs)
                        ?.first()

                    AndroidAutoService.MediaId.artists -> {
                        data
                            .getOrNull(1)
                            ?.let(Database::artistSongsByname)
                            ?.first()
                    }


                    */

                    else -> emptyList()
                }?.map(Song::asMediaItem) ?: return@launch

                withContext(Dispatchers.Main) {
                    sessionBinder.player.stop()
                }
                sessionOnlinePlayer.pause()

                if (sessionLocalMediaItem?.isLocal == true) {
                    withContext(Dispatchers.Main) {
                        Timber.d("AndroidAutoService onPlayFromMediaId LOCAL mediaId ${mediaId} mediaItems ${mediaItems.size} localMediaItem ${sessionLocalMediaItem?.mediaId} ready to play from index $index")
                        sessionBinder.stopRadio()
                        Timber.d("AndroidAutoService onPlayFromMediaId mediaId pre launch ")
                        sessionBinder.player.forcePlayAtIndex(mediaItems, index)
                        Timber.d("AndroidAutoService onPlayFromMediaId mediaId post launch ")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        sessionBinder.player.setMediaItems(
                            mediaItems.map { it.cleaned },
                            index,
                            C.TIME_UNSET
                        )
                        initializeOnlinePlayer()
                        //internalOnlinePlayer.value?.loadVideo(mediaId,0f)
                    }

                }
            }

            // END PROCESSING
        }

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

    companion object {

        var lastSongs: List<Song> = emptyList()
        var searchedSongs: List<Song> = emptyList()

        private const val NOTIFICATION_ID = 20 // The id of the notification
        //private const val CHANNEL_ID = "AAServiceChannel" // The id of the channel

    }

}

private const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
private const val CONTENT_STYLE_LIST = 1
private const val CONTENT_STYLE_GRID = 2