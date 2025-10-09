package it.fast4x.riplay.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
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
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.util.UnstableApi
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.BrowseEndpoint
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.completed
import it.fast4x.environment.utils.from
import it.fast4x.riplay.Database
import it.fast4x.riplay.MODIFIED_PREFIX
import it.fast4x.riplay.MONTHLY_PREFIX
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.UNIFIED_NOTIFICATION_CHANNEL
import it.fast4x.riplay.PINNED_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.appContext
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.removePrefix
import it.fast4x.riplay.enums.MaxTopPlaylistItems
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.extensions.preferences.MaxTopPlaylistItemsKey
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.isAppRunning
import it.fast4x.riplay.isNotifyAndroidAutoTipsEnabled
import it.fast4x.riplay.models.Album
import it.fast4x.riplay.models.Artist
import it.fast4x.riplay.models.PlaylistPreview
import it.fast4x.riplay.models.Song
import it.fast4x.riplay.models.SongAlbumMap
import it.fast4x.riplay.models.SongArtistMap
import it.fast4x.riplay.utils.BitmapProvider
import it.fast4x.riplay.utils.getTitleMonthlyPlaylistFromContext
import it.fast4x.riplay.utils.intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.isAtLeastAndroid12
import kotlin.math.roundToInt
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.isAtLeastAndroid11
import it.fast4x.riplay.utils.isAtLeastAndroid6
import it.fast4x.riplay.utils.isAtLeastAndroid8


@UnstableApi
class AndroidAutoService : MediaBrowserServiceCompat(), ServiceConnection {

    //private var mNotificationManager: NotificationManager? = null

    var tmpLocalPlayerBinder: LocalPlayerService.Binder? = null
        set(value) {
            localPlayerBinder = value
        }
//    var tmpOnlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)
//        set(value) {
//            onlinePlayer = value
//        }

    var tmpMediaSessionCompat: MediaSessionCompat? = null
        set(value) {
            mediaSession = value
        }

    companion object {

        var mediaSession: MediaSessionCompat? = null
        var localPlayerBinder: LocalPlayerService.Binder? = null
        //var onlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)
        var bitmapProvider: BitmapProvider? = null
        var isPlaying: Boolean = false
        var lastSongs: List<Song> = emptyList()
        var searchedSongs: List<Song> = emptyList()



        val actions =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO
        val stateBuilder =
            PlaybackStateCompat.Builder().setActions(actions.let {
                if (isAtLeastAndroid12) it or PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED else it
            })
        var playbackDuration = 0L
        var playbackPosition = 0L



        private fun updateMediaSessionData() {
            Timber.d("AndroidAutoService updateMediaSessionPlaybackState")
            val mediaItem = localPlayerBinder?.player?.currentMediaItem ?: return
            bitmapProvider?.load(mediaItem.mediaMetadata.artworkUri) {}
            mediaSession?.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                        mediaItem.mediaId
                    )
                    .putBitmap(
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                        bitmapProvider?.bitmap
                    )
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        mediaItem.mediaMetadata.title.toString()
                    )
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        mediaItem.mediaMetadata.artist.toString()
                    )
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM,
                        mediaItem.mediaMetadata.albumTitle.toString()
                    )
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, playbackDuration)
                    .build()
            )

            updatePlaybackState()
        }

        private fun updatePlaybackState() {
            mediaSession?.setPlaybackState(
                stateBuilder
                    .setState(
                        if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        playbackPosition,
                        1f
                    )
                    .build()
            )
        }


        private const val NOTIFICATION_ID = 20 // The id of the notification
        //private const val CHANNEL_ID = "AAServiceChannel" // The id of the channel

    }

    var isRunning = false

    var currentMediaItem: androidx.media3.common.MediaItem? = null


    /**
     * Returns the instance of the service
     */
    inner class LocalBinder : Binder() {
        val serviceInstance: AndroidAutoService
            get() = this@AndroidAutoService

        var mediaSession: MediaSessionCompat? = null
            set(value) {
                this@AndroidAutoService.tmpMediaSessionCompat = value
            }

        var localPlayerBinder: LocalPlayerService.Binder? = null
            set(value) {
                this@AndroidAutoService.tmpLocalPlayerBinder = value
            }
//        var onlinePlayer: MutableState<YouTubePlayer?> = mutableStateOf(null)
//            set(value) {
//                this@AndroidAutoService.tmpOnlinePlayer = value
//            }

    }

    private val mBinder: IBinder = LocalBinder() // IBinder

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("AndroidAutoService onBind called with intent ${intent?.action}")
        if (SERVICE_INTERFACE == intent!!.action) {
            return super.onBind(intent)
        }
        Timber.d("AndroidAutoService onBind process intent ${intent?.action}")

        // start when client is connected
//        if (isNotifyAndroidAutoTipsEnabled())
//            startNotification()

        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        // not necessary because mediasession is passed by mainactivity
//        val sessionActivityPendingIntent =
//            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
//                PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
//            }
//
//        mediaSession = MediaSessionCompat(this, "AndroidAutoService")
//        mediaSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
//        mediaSession?.setRatingType(RatingCompat.RATING_NONE)
//        mediaSession?.setSessionActivity(sessionActivityPendingIntent)
////        if (mediaSession?.sessionToken != null)
////            setSessionToken(mediaSession?.sessionToken)
//        mediaSession?.isActive = true


        runCatching {
            bitmapProvider = BitmapProvider(
                bitmapSize = (512 * resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) Color.BLACK else Color.WHITE
                }
            )
        }.onFailure {
            Timber.e("Failed init bitmap provider in PlayerService ${it.stackTraceToString()}")
        }

        Timber.d("AndroidAutoService onCreate")


    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("AndroidAutoService onStartCommand")
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        //mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        //NotificationManagerCompat.from(this@AndroidAutoService).notify(NOTIFICATION_ID, notification)

        //startNotification() not start when service start

        isRunning = true
        return START_STICKY // If the service is killed, it will be automatically restarted
    }

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
            .setName(UNIFIED_NOTIFICATION_CHANNEL)
            .setShowBadge(false)
            .build()

        NotificationManagerCompat.from(this@AndroidAutoService).createNotificationChannel(channel)
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
                NotificationCompat.Builder(appContext(), UNIFIED_NOTIFICATION_CHANNEL)
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
        Timber.d("AndroidAutoService onGetRoot $clientPackageName but app is running ? ${isAppRunning()} mediaSession $mediaSession but service is running ? $isRunning")
        bindService(intent<AndroidAutoService>(), this, Context.BIND_AUTO_CREATE)

//        if(!isAppRunning())
//            return BrowserRoot(MediaId.FAULT, Bundle().apply { putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST) })

        return BrowserRoot(
            //if(isAppRunning()) MediaId.root else MediaId.fault,
            MediaId.ROOT,
            //bundleOf("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT" to 1)
            Bundle().apply {
                putBoolean(MEDIA_SEARCH_SUPPORTED, true)
                putBoolean(CONTENT_STYLE_SUPPORTED, true)
                putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
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
                            if (isNotEmpty()) add(0, shuffleBrowserMediaItem)
                        }

                    MediaId.PLAYLISTS -> {
                        if (id == "") {
                            Database
                                .playlistPreviewsByNameAsc()
                                .first()
                                .map { it.asBrowserMediaItem }
                                .sortedBy { it.description.title.toString() }
                                .map { it.asCleanMediaItem }
                                .toMutableList()
                                .apply {
                                    add(0, favoritesBrowserMediaItem)
                                    //add(1, offlineBrowserMediaItem)
                                    //add(2, downloadedBrowserMediaItem)
                                    add(1, topBrowserMediaItem)
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
                                    add(0, artistsInLibraryBrowserMediaItem)
                                    add(1, artistsOnDeviceBrowserMediaItem)
                                }
                        } else {
//                            Database.artistSongsByname(id)
//                                .first()
//                                .also { lastSongs = it }
//                                .map { it.asBrowserMediaItem }
//                                .toMutableList()
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
//                            Database.artistSongsByname(id)
//                                .first()
//                                .also { lastSongs = it }
//                                .map { it.asBrowserMediaItem }
//                                .toMutableList()
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
                                    add(0, albumsInLibraryBrowserMediaItem)
                                    add(1, albumsOnDeviceBrowserMediaItem)
                                }
                        } else {
                            Timber.d("AndroidAutoService onLoadChildren inside albums SONGS id $id")
//                            Database.albumSongs(id)
//                                .first()
//                                .also { lastSongs = it }
//                                .map { it.asBrowserMediaItem }
//                                .toMutableList()
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
//                            Database.albumSongs(id)
//                                .first()
//                                .also { lastSongs = it }
//                                .map { it.asBrowserMediaItem }
//                                .toMutableList()
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
                        MONTHLY_PREFIX,"1:",true) else playlist.name)
                .setSubtitle("$songCount ${(this@AndroidAutoService as Context).resources.getString(R.string.songs)}")
                .setIconUri(uriFor(if (playlist.name.startsWith(PINNED_PREFIX)) R.drawable.pin else
                    if (playlist.name.startsWith(MONTHLY_PREFIX)) R.drawable.stat_month else R.drawable.playlist))
                .build(),
            MediaItem.FLAG_BROWSABLE
        )

//    private val Album.asBrowserMediaItem
//        inline get() = MediaItem(
//            MediaDescriptionCompat.Builder()
//                .setMediaId(MediaId.forAlbumFavorites(id))
//                .setTitle(title?.removePrefix())
//                .setSubtitle(authorsText)
//                .setIconUri(thumbnailUrl?.toUri())
//                .build(),
//            MediaItem.FLAG_BROWSABLE
//        )

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

//    private val Artist.asBrowserMediaItem
//        inline get() = MediaItem(
//            MediaDescriptionCompat.Builder()
//                .setMediaId(MediaId.forArtistFavorites(name ?: ""))
//                .setTitle(name?.removePrefix())
//                //.setSubtitle()
//                .setIconUri(thumbnailUrl?.toUri())
//                .build(),
//            MediaItem.FLAG_BROWSABLE
//        )

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
                    if (description.title.toString().startsWith("1:")) getTitleMonthlyPlaylistFromContext(description.title.toString().substringAfter("1:"), this@AndroidAutoService) else description.title.toString())
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
    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {

        Timber.d("AndroidAutoService onServiceConnected isAppRunning ${isAppRunning()}")

        if (isNotifyAndroidAutoTipsEnabled())
            startNotification()

//        if(!isAppRunning()) {
//            startService(intent<AndroidAutoService>())
//            startNotification()
//            Timber.d("AndroidAutoService onServiceConnected started AndroidAutoService")
//        }



//        val intent = Intent(this, MainActivity::class.java)
//        intent.action = Intent.ACTION_MAIN
//        intent.addCategory(Intent.CATEGORY_LAUNCHER)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//
//        runCatching {
//            startActivity(intent)
//            Timber.d("AndroidAutoService onServiceConnected started MainActivity with intent")
//        }.onFailure {
//            Timber.d("AndroidAutoService onServiceConnected failed to start MainActivity with intent ${it.stackTraceToString()}")
//        }

        val service = p1
        if (service is LocalBinder) {
            if (mediaSession?.sessionToken != null) {
                sessionToken = mediaSession?.sessionToken
            }

//            localPlayerBinder?.let{
//                mediaSession?.setCallback(
//                    MediaSessionCallback(
//                        it,
//                        {
//                            Timber.d("AndroidAutoservice MediaSessionCallback onPlayClick")
//                            onlinePlayer.value?.play()
//                            isPlaying = true
//                            updateMediaSessionData()
//                        },
//                        {
//                            Timber.d("AndroidAutoservice MediaSessionCallback onPauseClick")
//                            onlinePlayer.value?.pause()
//                            isPlaying = false
//                            updateMediaSessionData()
//                        },
//                        { second ->
//                            val newPosition = (second / 1000).toFloat()
//                            Timber.d("AndroidAutoservice MediaSessionCallback onSeekPosTo ${newPosition}")
//                            onlinePlayer.value?.seekTo(newPosition)
//                            updateMediaSessionData()
//                        },
//                        {
//                            updateMediaSessionData()
//                        },
//                        {
//                            updateMediaSessionData()
//                        }
//                    )
//                )
//            }


            updateMediaSessionData()
        }
        Timber.d("AndroidAutoService onServiceConnected")
    }

    override fun onServiceDisconnected(name: ComponentName) = Unit

    //Used also as media button receiver
//    private inner class SessionCallback() :
//        MediaSessionCompat.Callback() {
//
//            override fun onStop() {
//                super.onStop()
//                onlinePlayer.value?.pause()
//                isPlaying = false
//                updateMediaSessionData()
//                Timber.d("AndroidAutoService MediaSessionCompat.Callback onStop")
//            }
//            override fun onPlay() {
//                super.onPlay()
//                onlinePlayer.value?.play()
//                isPlaying = true
//                updateMediaSessionData()
//                Timber.d("AndroidAutoService MediaSessionCompat.Callback onPlay")
//            }
//            override fun onPause() {
//                super.onPause()
//                onlinePlayer.value?.pause()
//                isPlaying = false
//                updateMediaSessionData()
//                Timber.d("AndroidAutoService MediaSessionCompat.Callback onPause")
//            }
//            override fun onSkipToNext() {
//                super.onSkipToNext()
//                localPlayerBinder?.player?.playNext()
//                updateMediaSessionData()
//            }
//
//            override fun onSkipToPrevious() {
//                super.onSkipToPrevious()
//                localPlayerBinder?.player?.playPrevious()
//                updateMediaSessionData()
//            }
//
//            @OptIn(UnstableApi::class)
//            override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
//                Timber.d("AndroidAutoService onPlayFromMediaId mediaId ${mediaId} called")
//                val data = mediaId?.split('/') ?: return
//                val id = data.getOrNull(1)
//                //var index = 0
//
//                Timber.d("AndroidAutoService onPlayFromMediaId mediaId ${mediaId} data $data elaborated")
//
//                coroutineScope.launch {
//                    val mediaItem = Database.song(id).first()?.asMediaItem ?: return@launch
//                    withContext(Dispatchers.Main) {
//                        fastPlay(
//                            mediaItem,
//                            localPlayerBinder
//                        )
//                        isPlaying = true
//                        updateMediaSessionData()
//                    }
//                }
//            }
//        }


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

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
private const val CONTENT_STYLE_LIST = 1
private const val CONTENT_STYLE_GRID = 2