package it.fast4x.riplay.data

import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.ExternalApp
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.GenericMenuItem
import it.fast4x.riplay.enums.PlaylistSongSortBy
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SongSortBy
import it.fast4x.environment.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Compatibilità Android per il database v68 spostato in commonMain.
 * Ogni extension qui preserva l'API originale usata dai call sites di androidApp.
 */

// ---------------------------------------------------------------------------
// Accesso al singleton (compat: DatabaseInitializer.Instance / .dao)
// ---------------------------------------------------------------------------

val DatabaseInitializer.Companion.Instance: DatabaseInitializer
    get() = getDatabaseInitializer()

val DatabaseInitializer.Companion.dao: Database
    get() = getDatabaseDao()

// ---------------------------------------------------------------------------
// Serializzazione MediaItem <-> ByteArray (ex TypeConverters, ora helper Android)
// ---------------------------------------------------------------------------

@OptIn(UnstableApi::class)
fun MediaItem.toMediaItemBytes(): ByteArray? {
    if (this == null) return null
    val parcel = android.os.Parcel.obtain()
    return try {
        @Suppress("DEPRECATION")
        val bundle = this.toBundle()
        parcel.writeBundle(bundle)
        parcel.marshall()
    } catch (e: Exception) {
        null
    } finally {
        parcel.recycle()
    }
}

@OptIn(UnstableApi::class)
fun ByteArray.toMediaItemCompat(): MediaItem? {
    val parcel = android.os.Parcel.obtain()
    return try {
        parcel.unmarshall(this, 0, this.size)
        parcel.setDataPosition(0)
        val classLoader = MediaItem::class.java.classLoader
            ?: Thread.currentThread().contextClassLoader
        val bundle = parcel.readBundle(classLoader)
        @Suppress("DEPRECATION")
        bundle?.let(MediaItem::fromBundle)
    } catch (e: Exception) {
        null
    } finally {
        parcel.recycle()
    }
}

@OptIn(UnstableApi::class)
val QueuedMediaItem.mediaItemCompat: MediaItem?
    get() = mediaItemBytes.toMediaItemCompat()

// ---------------------------------------------------------------------------
// DAO helpers con tipi Android (ex metodi dell'interfaccia Database)
// ---------------------------------------------------------------------------

fun Database.Companion.insert(mediaItem: MediaItem, block: (Song) -> Song = { it }) {
    CoroutineScope(Dispatchers.IO).launch {
    var title = mediaItem.mediaMetadata.title.toString()
    if (!title.startsWith(EXPLICIT_PREFIX, true) && mediaItem.isExplicit) {
        title = EXPLICIT_PREFIX + title
    }
    val isVideo = mediaItem.mediaMetadata.extras?.getBoolean("isVideo") == true
    val isPodcast = mediaItem.mediaMetadata.extras?.getBoolean("isPodcast") == true
    val song = Song(
        id = mediaItem.mediaId,
        title = title,
        artistsText = mediaItem.mediaMetadata.artist?.toString(),
        durationText = mediaItem.mediaMetadata.extras?.getString("durationText"),
        thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
        isAudioOnly = if (!isVideo) 1 else 0,
        isPodcast = if (isPodcast) 1 else 0
    ).let(block).also { song ->
        if (insert(song) == -1L) return@launch
    }

    mediaItem.mediaMetadata.extras?.getString("albumId")?.let { albumId ->
        insert(
            it.fast4x.riplay.data.models.Album(id = albumId, title = mediaItem.mediaMetadata.albumTitle?.toString()),
            it.fast4x.riplay.data.models.SongAlbumMap(songId = song.id, albumId = albumId, position = null)
        )
    }

    mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.let { artistNames ->
        mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artistIds ->
            if (artistNames.size == artistIds.size) {
                insert(
                    artistNames.mapIndexed { index, artistName ->
                        it.fast4x.riplay.data.models.Artist(id = artistIds[index], name = artistName)
                    },
                    artistIds.map { artistId ->
                        it.fast4x.riplay.data.models.SongArtistMap(songId = song.id, artistId = artistId)
                    }
                )
            }
        }
    }

    }}

fun Database.Companion.update(playlist: Playlist, playlistItem: Environment.PlaylistItem) {
    CoroutineScope(Dispatchers.IO).launch {
    update(playlist.copy(
        name = playlistItem.title ?: "",
        browseId = playlistItem.key
    ))

    }}

val ExternalApp.componentName: ComponentName
    get() = ComponentName(packageName, activityName)

// ---------------------------------------------------------------------------
// Sorting enums: UI helpers (ex members dell'enum, ora extensions con R Android)
// ---------------------------------------------------------------------------

val SongSortBy.textId: Int
    get() = when (this) {
        SongSortBy.PlayTime, SongSortBy.RelativePlayTime -> R.string.sort_listening_time
        SongSortBy.Title -> R.string.sort_title
        SongSortBy.DateAdded -> R.string.sort_date_added
        SongSortBy.DatePlayed -> R.string.sort_date_played
        SongSortBy.DateLiked -> R.string.sort_date_liked
        SongSortBy.Artist -> R.string.sort_artist
        SongSortBy.Duration -> R.string.sort_duration
        SongSortBy.AlbumName -> R.string.sort_album
    }

val SongSortBy.iconId: Int
    get() = when (this) {
        SongSortBy.PlayTime, SongSortBy.RelativePlayTime -> R.drawable.trending
        SongSortBy.Title -> R.drawable.text
        SongSortBy.DateAdded -> R.drawable.time
        SongSortBy.DatePlayed -> R.drawable.calendar
        SongSortBy.DateLiked -> R.drawable.heart
        SongSortBy.Artist -> R.drawable.artist
        SongSortBy.Duration -> R.drawable.time
        SongSortBy.AlbumName -> R.drawable.music_album
    }

val SongSortBy.titleId: Int
    get() = this.textId

val SongSortBy.icon: Painter
    @Composable
    get() = painterResource(this.iconId)

val SongSortBy.menuItem: GenericMenuItem
    @Composable
    get() = GenericMenuItem(this.ordinal, textId, iconId)

val AlbumSortBy.textId: Int
    get() = when (this) {
        AlbumSortBy.Title -> R.string.sort_album
        AlbumSortBy.Year -> R.string.sort_album_year
        AlbumSortBy.DateAdded -> R.string.sort_date_added
        AlbumSortBy.Artist -> R.string.sort_artist
        AlbumSortBy.Songs -> R.string.sort_songs_number
        AlbumSortBy.Duration -> R.string.sort_duration
    }

val AlbumSortBy.iconId: Int
    get() = when (this) {
        AlbumSortBy.Title -> R.drawable.text
        AlbumSortBy.Year -> R.drawable.calendar
        AlbumSortBy.DateAdded -> R.drawable.time
        AlbumSortBy.Artist -> R.drawable.artist
        AlbumSortBy.Songs -> R.drawable.medical
        AlbumSortBy.Duration -> R.drawable.time
    }

val AlbumSortBy.titleId: Int
    get() = this.textId

val AlbumSortBy.icon: Painter
    @Composable
    get() = painterResource(this.iconId)

val AlbumSortBy.menuItem: GenericMenuItem
    @Composable
    get() = GenericMenuItem(this.ordinal, textId, iconId)

val ArtistSortBy.textId: Int
    get() = when (this) {
        ArtistSortBy.Name -> R.string.sort_artist
        ArtistSortBy.DateAdded -> R.string.sort_date_added
    }

val ArtistSortBy.iconId: Int
    get() = when (this) {
        ArtistSortBy.Name -> R.drawable.text
        ArtistSortBy.DateAdded -> R.drawable.time
    }

val ArtistSortBy.titleId: Int
    get() = this.textId

val ArtistSortBy.icon: Painter
    @Composable
    get() = painterResource(this.iconId)

val ArtistSortBy.menuItem: GenericMenuItem
    @Composable
    get() = GenericMenuItem(this.ordinal, textId, iconId)

val PlaylistSortBy.textId: Int
    get() = when (this) {
        PlaylistSortBy.MostPlayed -> R.string.sort_listening_time
        PlaylistSortBy.Name -> R.string.sort_name
        PlaylistSortBy.DateAdded -> R.string.sort_date_added
        PlaylistSortBy.SongCount -> R.string.sort_songs_number
    }

val PlaylistSortBy.iconId: Int
    get() = when (this) {
        PlaylistSortBy.MostPlayed -> R.drawable.trending
        PlaylistSortBy.Name -> R.drawable.text
        PlaylistSortBy.DateAdded -> R.drawable.calendar
        PlaylistSortBy.SongCount -> R.drawable.medical
    }

val PlaylistSortBy.titleId: Int
    get() = this.textId

val PlaylistSortBy.icon: Painter
    @Composable
    get() = painterResource(this.iconId)

val PlaylistSortBy.menuItem: GenericMenuItem
    @Composable
    get() = GenericMenuItem(this.ordinal, textId, iconId)

val PlaylistSongSortBy.textId: Int
    get() = when (this) {
        PlaylistSongSortBy.Album -> R.string.sort_album
        PlaylistSongSortBy.AlbumYear -> R.string.sort_album_year
        PlaylistSongSortBy.Artist -> R.string.sort_artist
        PlaylistSongSortBy.ArtistAndAlbum -> -1
        PlaylistSongSortBy.DatePlayed -> R.string.sort_date_played
        PlaylistSongSortBy.PlayTime, PlaylistSongSortBy.RelativePlayTime -> R.string.sort_listening_time
        PlaylistSongSortBy.Position -> R.string.sort_position
        PlaylistSongSortBy.Title -> R.string.sort_title
        PlaylistSongSortBy.Duration -> R.string.sort_duration
        PlaylistSongSortBy.DateLiked -> R.string.sort_date_liked
        PlaylistSongSortBy.DateAdded -> R.string.sort_date_added
    }

val PlaylistSongSortBy.iconId: Int
    get() = when (this) {
        PlaylistSongSortBy.Album -> R.drawable.music_album
        PlaylistSongSortBy.AlbumYear -> R.drawable.calendar
        PlaylistSongSortBy.Artist, PlaylistSongSortBy.ArtistAndAlbum -> R.drawable.artist
        PlaylistSongSortBy.DatePlayed -> R.drawable.up_right_arrow
        PlaylistSongSortBy.PlayTime, PlaylistSongSortBy.RelativePlayTime -> R.drawable.trending
        PlaylistSongSortBy.Position -> R.drawable.position
        PlaylistSongSortBy.Title -> R.drawable.text
        PlaylistSongSortBy.Duration -> R.drawable.time
        PlaylistSongSortBy.DateLiked -> R.drawable.heart
        PlaylistSongSortBy.DateAdded -> R.drawable.time
    }

val PlaylistSongSortBy.titleId: Int
    get() {
        // Due to the requirement of 2 separated strings,
        // this needs to be handled separately
        if (this == PlaylistSongSortBy.ArtistAndAlbum)
            throw UnsupportedOperationException()
        return this.textId
    }

val PlaylistSongSortBy.icon: Painter
    @Composable
    get() = painterResource(this.iconId)
