package it.fast4x.riplay.utils

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.zionhuang.innertube.pages.LibraryPage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import it.fast4x.environment.Environment
import it.fast4x.environment.EnvironmentExt.addToPlaylist
import it.fast4x.environment.EnvironmentExt.likeVideoOrSong
import it.fast4x.environment.EnvironmentExt.removelikeVideoOrSong
import it.fast4x.environment.models.bodies.ContinuationBody
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.playlistPage
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.requests.song
import it.fast4x.environment.utils.from
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.Database.Companion.getLikedAt
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.lastfm.sendLoveTrack
import it.fast4x.riplay.extensions.lastfm.sendUnloveTrack
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.ui.screens.settings.isYtSyncEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.absoluteValue
import kotlin.random.Random

val Environment.AlbumItem.asAlbum: Album
    get() = Album (
        id = key,
        title = info?.name,
        thumbnailUrl = thumbnail?.url,
        year = year,
        authorsText = authors?.joinToString(", ") { it.name ?: "" },
        //shareUrl =
    )

fun Environment.Podcast.toPlaylist(browseId: String): Playlist
        = Playlist (
    browseId = browseId,
    name = title,
    isPodcast = true
)

val Environment.PlaylistItem.asPlaylist: Playlist
    get() = Playlist (
        browseId = key,
        name = info?.name.toString(),
        isPodcast = false
    )

val Environment.Podcast.EpisodeItem.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaId(videoId)
        .setUri(videoId)
        .setCustomCacheKey(videoId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(author.toString())
                .setAlbumTitle(title)
                .setArtworkUri(thumbnail.firstOrNull()?.url?.toUri())
                .setExtras(
                    bundleOf(
                        //"albumId" to album?.endpoint?.browseId,
                        "durationText" to durationString,
                        "artistNames" to author,
                        //"artistIds" to authors?.mapNotNull { it.endpoint?.browseId },
                        "isPodcast" to true
                    )
                )

                .build()
        )
        .build()

val Environment.SongItem.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.filter {it.name?.matches(Regex("\\s*([,&])\\s*")) == false }?.joinToString(", ") { it.name ?: "" })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "albumId" to album?.endpoint?.browseId,
                        "durationText" to durationText,
                        "artistNames" to authors?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name },
                        "artistIds" to authors?.mapNotNull { it.endpoint?.browseId },
                        EXPLICIT_BUNDLE_TAG to explicit,
                        "setVideoId" to setVideoId,
                        "isOfficialMusicVideo" to isOfficialMusicVideo,
                        "isOfficialUploadByArtistContent" to isOfficialUploadByArtistContent,
                        "isUserGeneratedContent" to isUserGeneratedContent,
                        "isVideo" to !isAudioOnly,
                    )
                )
                .build()
        )
        .build()

val Environment.SongItem.asVideoMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.filter {it.name?.matches(Regex("\\s*([,&])\\s*")) == false }?.joinToString(", ") { it.name ?: "" })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "albumId" to album?.endpoint?.browseId,
                        "durationText" to durationText,
                        "artistNames" to authors?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name },
                        "artistIds" to authors?.mapNotNull { it.endpoint?.browseId },
                        EXPLICIT_BUNDLE_TAG to explicit,
                        "setVideoId" to setVideoId,
                        "isOfficialMusicVideo" to isOfficialMusicVideo,
                        "isOfficialUploadByArtistContent" to isOfficialUploadByArtistContent,
                        "isUserGeneratedContent" to isUserGeneratedContent,
                        "isVideo" to true,
                    )
                )
                .build()
        )
        .build()

val Environment.SongItem.asSong: Song
    @UnstableApi
    get() = Song (
        id = key,
        title = info?.name ?: "",
        artistsText = authors?.joinToString(", ") { it.name ?: "" },
        durationText = durationText,
        thumbnailUrl = thumbnail?.url
    )


val Environment.VideoItem.asMediaItem: MediaItem
    @UnstableApi
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString(", ") { it.name ?: "" })
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "artistNames" to authors?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name },
                        "artistIds" to authors?.mapNotNull { it.endpoint?.browseId },
                        "isOfficialMusicVideo" to isOfficialMusicVideo,
                        "isOfficialUploadByArtistContent" to isOfficialUploadByArtistContent,
                        "isUserGeneratedContent" to isUserGeneratedContent,
                        "isVideo" to true,
                        // "artistNames" to if (isOfficialMusicVideo) authors?.filter { it.endpoint != null }?.mapNotNull { it.name } else null,
                        // "artistIds" to if (isOfficialMusicVideo) authors?.mapNotNull { it.endpoint?.browseId } else null,
                    )
                )
                .build()
        )
        .build()

@ExperimentalSerializationApi
@JvmName("ResultInnertubeItemsPageCompleted")
suspend fun Result<Environment.ItemsPage<Environment.SongItem>?>.completed(
    maxDepth: Int =  Int.MAX_VALUE
): Result<Environment.ItemsPage<Environment.SongItem>?> = runCatching {
    val page = getOrThrow()
    val songs = page?.items.orEmpty().toMutableList()
    var continuation = page?.continuation

    var depth = 0
    var continuationsList = arrayOf<String>()
    //continuationsList += continuation.orEmpty()

    println("mediaItem playlist completed() continuation? $continuation")

    while (continuation != null && depth++ < maxDepth) {
        val newSongs = Environment
            .playlistPage(
                body = ContinuationBody(continuation = continuation)
            )
            ?.getOrNull()
            ?.takeUnless { it.items.isNullOrEmpty() } ?: break

        newSongs.items?.let { songs += it.filter { it !in songs } }
        continuation = newSongs.continuation

        //println("mediaItem loop $depth continuation founded ${continuationsList.contains(continuation)} $continuation")
        if (continuationsList.contains(continuation)) break

        continuationsList += continuation.orEmpty()
        //println("mediaItem loop continuationList size ${continuationsList.size}")
    }

    page?.copy(items = songs, continuation = null)
}.also { it.exceptionOrNull()?.printStackTrace() }

@ExperimentalSerializationApi
@JvmName("ResultInnertubePlaylistOrAlbumPageCompleted")
suspend fun Result<Environment.PlaylistOrAlbumPage>.completed(
    maxDepth: Int =  Int.MAX_VALUE
): Result<Environment.PlaylistOrAlbumPage> = runCatching {
    val page = getOrThrow()
    val songsPage = runCatching {
        page.songsPage
    }.onFailure {
        println("Innertube songsPage PlaylistOrAlbumPage>.completed ${it.stackTraceToString()}")
    }
    val itemsPage = songsPage.completed(maxDepth).getOrThrow()
    page.copy(songsPage = itemsPage)
}.onFailure {
    println("Innertube PlaylistOrAlbumPage>.completed ${it.stackTraceToString()}")
}

suspend fun Result<LibraryPage?>.completed(): Result<LibraryPage> = runCatching {
    val page = getOrThrow()
    val items = page?.items?.toMutableList()
    var continuation = page?.continuation
    while (continuation != null) {
        val continuationPage = Environment.libraryContinuation(continuation).getOrNull()
        if (continuationPage != null)
            if (items != null) {
                items += continuationPage.items
            }

        continuation = continuationPage?.continuation
    }
    LibraryPage(
        items = items ?: emptyList(),
        continuation = page?.continuation
    )
}

@ExperimentalSerializationApi
@OptIn(UnstableApi::class)
suspend fun getAlbumVersionFromVideo(song: Song,playlistId : Long, position : Int, playlist : Playlist?) {
    val isExtPlaylist = (song.thumbnailUrl == "") && (song.durationText != "0:00")
    var songNotFound: Song
    var random4Digit  = Random.nextInt(1000, 10000)
    fun filteredText(text : String): String{
        val filteredText = text
            .lowercase()
            .replace("(", " ")
            .replace(")", " ")
            .replace("-", " ")
            .replace("lyrics", "")
            .replace("vevo", "")
            .replace(" hd", "")
            .replace("official video", "")
            .filter {it.isLetterOrDigit() || it.isWhitespace() || it == '\'' || it == ',' }
            .replace(Regex("\\s+"), " ")
        return filteredText
    }

    val searchQuery = Environment.searchPage(
        body = SearchBody(
            query = filteredText("${cleanPrefix(song.title)} ${song.artistsText}"),
            params = Environment.SearchFilter.Song.value
        ),
        fromMusicShelfRendererContent = Environment.SongItem.Companion::from
    )

    val searchResults = searchQuery?.getOrNull()?.items

    val sourceSongWords = filteredText(cleanPrefix(song.title))
        .split(" ").filter { it.isNotEmpty() }
    val lofi = sourceSongWords.contains("lofi")
    val rock = sourceSongWords.contains("rock")
    val reprise = sourceSongWords.contains("reprise")
    val unplugged = sourceSongWords.contains("unplugged")
    val instrumental = sourceSongWords.contains("instrumental")
    val remix = sourceSongWords.contains("remix")
    val acapella = sourceSongWords.contains("acapella")
    val acoustic = sourceSongWords.contains("acoustic")
    val live = sourceSongWords.contains("live")
    val concert = sourceSongWords.contains("concert")
    val tour = sourceSongWords.contains("tour")
    val redux = sourceSongWords.contains("redux")

    fun shuffle(word: String): String {
        val chars = word.toCharArray()
        for (i in chars.indices) {
            val randomIndex = Random.nextInt(chars.size)
            chars[i] = chars[randomIndex]
        }
        return String(chars)
    }

    fun findSongIndex() : Int {
        for (i in 0..4) {
            val requiredSong = searchResults?.getOrNull(i)
            val requiredSongWords = filteredText(cleanPrefix(requiredSong?.title ?: ""))
                .split(" ").filter { it.isNotEmpty() }

            val songMatched = (requiredSong != null)
                    && (requiredSongWords.any { it in sourceSongWords })
                    && (if (lofi) (requiredSongWords.any { it == "lofi" }) else requiredSongWords.all { it != "lofi" })
                    && (if (rock) (requiredSongWords.any { it == "rock" }) else requiredSongWords.all { it != "rock" })
                    && (if (reprise) (requiredSongWords.any { it == "reprise" }) else requiredSongWords.all { it != "reprise" })
                    && (if (unplugged) (requiredSongWords.any { it == "unplugged" }) else requiredSongWords.all { it != "unplugged" })
                    && (if (instrumental) (requiredSongWords.any { it == "instrumental" }) else requiredSongWords.all { it != "instrumental" })
                    && (if (remix) (requiredSongWords.any { it == "remix" }) else requiredSongWords.all { it != "remix" })
                    && (if (acapella) (requiredSongWords.any { it == "acapella" }) else requiredSongWords.all { it != "acapella" })
                    && (if (acoustic) (requiredSongWords.any { it == "acoustic" }) else requiredSongWords.all { it != "acoustic" })
                    && (if (live) (requiredSongWords.any { it == "live" }) else requiredSongWords.all { it != "live" })
                    && (if (concert) (requiredSongWords.any { it == "concert" }) else requiredSongWords.all { it != "concert" })
                    && (if (tour) (requiredSongWords.any { it == "tour" }) else requiredSongWords.all { it != "tour" })
                    && (if (redux) (requiredSongWords.any { it == "redux" }) else requiredSongWords.all { it != "redux" })
                    && (if (song.asMediaItem.isExplicit) {requiredSong.asMediaItem.isExplicit} else {true})
                    && (if (isExtPlaylist) {(durationTextToMillis(
                requiredSong.durationText ?: ""
            ) - durationTextToMillis(song.durationText ?: "")).absoluteValue <= 2000}
            else {true})

            if (songMatched) return i
        }
        return -1
    }

    val matchedSong = searchResults?.getOrNull(findSongIndex())
    val artistsNames = matchedSong?.authors?.filter { it.endpoint != null }?.map { it.name }
    val artistNameString = matchedSong?.asMediaItem?.mediaMetadata?.artist?.toString() ?: ""
    val artistsIds = matchedSong?.authors?.filter { it.endpoint != null }?.map { it.endpoint?.browseId }

    Database.asyncTransaction {
        if (findSongIndex() != -1) {
            if (isYtSyncEnabled() && playlist?.isYoutubePlaylist == true && playlist.isEditable){
                Database.asyncTransaction {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (removeYTSongFromPlaylist(
                                song.id,
                                playlist.browseId ?: "",
                                playlist.id
                            )
                        )
                            deleteSongFromPlaylist(song.id, playlist.id)

                    }
                }

            } else {
                deleteSongFromPlaylist(song.id, playlist?.id ?: 0L)
            }
            if (matchedSong != null) {
                if (songExist(matchedSong.asSong.id) == 0) {
                    Database.insert(matchedSong.asMediaItem)
                }
                insert(
                    SongPlaylistMap(
                        songId = matchedSong.asMediaItem.mediaId,
                        playlistId = playlistId,
                        position = position
                    ).default()
                )
                insert(
                    Album(id = matchedSong.album?.endpoint?.browseId ?: "", title = matchedSong.asMediaItem.mediaMetadata.albumTitle?.toString()),
                    SongAlbumMap(songId = matchedSong.asMediaItem.mediaId, albumId = matchedSong.album?.endpoint?.browseId ?: "", position = null)
                )
                CoroutineScope(Dispatchers.IO).launch {
                    val album = Database.album(matchedSong.album?.endpoint?.browseId ?: "").firstOrNull()
                    album?.copy(thumbnailUrl = matchedSong.thumbnail?.url)?.let { update(it) }

                    if (isYtSyncEnabled() && playlist?.isYoutubePlaylist == true && playlist.isEditable){
                        addToPlaylist(playlist.browseId ?: "", matchedSong.asMediaItem.mediaId)
                    }
                }
                if ((artistsNames != null) && (artistsIds != null)) {
                    artistsNames.let { artistNames ->
                        artistsIds.let { artistIds ->
                            if (artistNames.size == artistIds.size) {
                                insert(
                                    artistNames.mapIndexed { index, artistName ->
                                        Artist(id = (artistIds[index]) ?: "", name = artistName)
                                    },
                                    artistIds.map { artistId ->
                                        SongArtistMap(songId = matchedSong.asMediaItem.mediaId, artistId = (artistId) ?: "")
                                    }
                                )
                            }
                        }
                    }
                }
                Database.updateSongArtist(matchedSong.asMediaItem.mediaId, artistNameString)
                if (song.thumbnailUrl == "") Database.delete(song)
            }
        } else if (song.id == ((cleanPrefix(song.title) +song.artistsText).filter {it.isLetterOrDigit()})){
            songNotFound = song.copy(id = shuffle(song.artistsText+random4Digit+ cleanPrefix(song.title) +"56Music").filter{it.isLetterOrDigit()})
            Database.delete(song)
            Database.insert(songNotFound)
            Database.insert(
                SongPlaylistMap(
                    songId = songNotFound.id,
                    playlistId = playlistId,
                    position = position
                ).default()
            )
        }
    }
}

suspend fun updateLocalPlaylist(song: Song){

    val matchedSong = Environment.song(song.id)?.getOrNull()
    val artistsNames = matchedSong?.authors?.filter { it.endpoint != null }?.map { it.name }
    val artistNameString = matchedSong?.asMediaItem?.mediaMetadata?.artist?.toString() ?: ""
    val artistsIds = matchedSong?.authors?.filter { it.endpoint != null }?.map { it.endpoint?.browseId }

    Database.asyncTransaction {
        if (matchedSong != null && song.id == matchedSong.asMediaItem.mediaId) {
            insert(
                Album(id = matchedSong.album?.endpoint?.browseId ?: "", title = matchedSong.asMediaItem.mediaMetadata.albumTitle?.toString()),
                SongAlbumMap(songId = matchedSong.asMediaItem.mediaId, albumId = matchedSong.album?.endpoint?.browseId ?: "", position = null)
            )
            CoroutineScope(Dispatchers.IO).launch {
                val album = Database.album(matchedSong.album?.endpoint?.browseId ?: "").firstOrNull()
                album?.copy(thumbnailUrl = matchedSong.thumbnail?.url)?.let { update(it) }
            }

            if ((artistsNames != null) && (artistsIds != null)) {
                artistsNames.let { artistNames ->
                    artistsIds.let { artistIds ->
                        if (artistNames.size == artistIds.size) {
                            insert(
                                artistNames.mapIndexed { index, artistName ->
                                    Artist(id = (artistIds[index]) ?: "", name = artistName)
                                },
                                artistIds.map { artistId ->
                                    SongArtistMap(songId = song.id, artistId = (artistId) ?: "")
                                }
                            )
                        }
                    }
                }
            }
            Database.updateSongArtist(matchedSong.asMediaItem.mediaId, artistNameString)
        }
    }
}

@ExperimentalSerializationApi
suspend fun addToYtPlaylist(localPlaylistId: Long, position: Int, ytplaylistId: String, mediaItems: List<MediaItem>){
    val mediaItemsChunks = mediaItems.chunked(50)
    mediaItemsChunks.forEachIndexed { index, items ->
        if (mediaItems.size <= 50) {}
        else if (index == 0) {
            SmartMessage(
                "${mediaItems.size} "+appContext().resources.getString(R.string.songs_adding_in_yt),
                context = appContext(),
                durationLong = true
            )
        } else {
            delay(2000)
        }
        addToPlaylist(ytplaylistId, items.map { it.mediaId })
            .onSuccess {
                items.forEachIndexed { index, item ->
                    Database.asyncTransaction {
                        if (songExist(item.mediaId) == 0){
                            Database.insert(item)
                        }
                        Database.insert(
                            SongPlaylistMap(
                                songId = item.mediaId,
                                playlistId = localPlaylistId,
                                position = position + index
                            ).default()
                        )
                    }
                }
                if (items.size == 50) {
                    SmartMessage(
                        "${mediaItems.size - (index + 1) * 50} Songs Remaining",
                        context = appContext(),
                        durationLong = false
                    )
                }
            }
            .onFailure {
                println("YtMusic addToPlaylist (list of size ${items.size}) error: ${it.stackTraceToString()}")
                if(it is ClientRequestException && it.response.status == HttpStatusCode.BadRequest) {
                    SmartMessage(
                        appContext().resources.getString(R.string.adding_yt_to_pl_failed),
                        context = appContext(),
                        durationLong = false
                    )
                    items.forEach { item ->
                        delay(500)
                        addToPlaylist(ytplaylistId, item.mediaId)
                            .onFailure {
                                println("YtMusic addToPlaylist (list insert backup) error: ${it.stackTraceToString()}")
                                SmartMessage(
                                    appContext().resources.getString(R.string.songs_add_yt_failed)+"${item.mediaMetadata.title} - ${item.mediaMetadata.artist}",
                                    type = PopupType.Error,
                                    context = appContext(),
                                    durationLong = false
                                )
                            }.onSuccess {
                                Database.asyncTransaction {
                                    if (songExist(item.mediaId) == 0){
                                        Database.insert(item)
                                    }
                                    insert(
                                        SongPlaylistMap(
                                            songId = item.mediaId,
                                            playlistId = localPlaylistId,
                                            position = position
                                        ).default()
                                    )
                                }
                                SmartMessage(
                                    "${items.size - (index + 1)} Songs Remaining",
                                    context = appContext(),
                                    durationLong = false
                                )
                            }
                    }
                }
            }
    }
    SmartMessage(
        "${mediaItems.size} "+ appContext().resources.getString(R.string.songs_added_in_yt),
        context = appContext(),
        durationLong = true
    )
}

@ExperimentalSerializationApi
suspend fun addSongToYtPlaylist(localPlaylistId: Long, position: Int, ytplaylistId: String, mediaItem: MediaItem){
    if (isYtSyncEnabled()) {
        addToPlaylist(ytplaylistId,mediaItem.mediaId)
            .onSuccess {
                Database.asyncTransaction {
                    if (songExist(mediaItem.mediaId) == 0) {
                        Database.insert(mediaItem)
                    }
                    insert(
                        SongPlaylistMap(
                            songId = mediaItem.mediaId,
                            playlistId = localPlaylistId,
                            position = position
                        ).default()
                    )
                }
                SmartMessage(
                    appContext().resources.getString(R.string.songs_add_yt_success),
                    context = appContext(),
                    durationLong = true
                )
            }
            .onFailure {
                SmartMessage(
                    appContext().resources.getString(R.string.songs_add_yt_failed),
                    context = appContext(),
                    durationLong = true
                )
            }

    }
}

@ExperimentalSerializationApi
@OptIn(UnstableApi::class)
suspend fun addToOnlineLikedSong(mediaItem: MediaItem){

    if(isEnabledLastFm()) {
        sendLoveTrack(mediaItem.mediaMetadata.artist as String,
            mediaItem.mediaMetadata.title as String
        )
        SmartMessage(
            appContext().resources.getString(R.string.song_liked_lastfm),
            context = appContext(),
            durationLong = false
        )
    }

    if (isYtSyncEnabled()) {
        if (getLikedAt(mediaItem.mediaId) in listOf(-1L, null)) {
            likeVideoOrSong(mediaItem.mediaId)
                .onSuccess {
                    Database.asyncTransaction {
                        if (songExist(mediaItem.mediaId) == 0) {
                            Database.insert(mediaItem)
                        }
                        like(mediaItem.mediaId, System.currentTimeMillis())

                    }
                    SmartMessage(
                        appContext().resources.getString(R.string.songs_liked_yt),
                        context = appContext(),
                        durationLong = false
                    )
                }
                .onFailure {
                    SmartMessage(
                        appContext().resources.getString(R.string.songs_liked_yt_failed),
                        context = appContext(),
                        durationLong = false
                    )
                }
        } else {
            removeFromOnlineLikedSong(mediaItem)
        }
    }
}

@ExperimentalSerializationApi
@OptIn(UnstableApi::class)
suspend fun removeFromOnlineLikedSong(mediaItem: MediaItem){

    if(isEnabledLastFm()) {
        sendUnloveTrack(mediaItem.mediaMetadata.artist as String,
            mediaItem.mediaMetadata.title as String
        )
        SmartMessage(
            appContext().resources.getString(R.string.song_unliked_lastfm),
            context = appContext(),
            durationLong = false
        )
    }

    if(isYtSyncEnabled()){
        removelikeVideoOrSong(mediaItem.mediaId)
            .onSuccess {
                Database.asyncTransaction {
                    if(songExist(mediaItem.mediaId) == 0){
                        insert(mediaItem)
                    }
                    like(mediaItem.mediaId, null)

                }
                SmartMessage(
                    appContext().resources.getString(R.string.song_unliked_yt),
                    context = appContext(),
                    durationLong = false
                )
            }
            .onFailure {
                SmartMessage(
                    appContext().resources.getString(R.string.songs_unliked_yt_failed),
                    context = appContext(),
                    durationLong = false
                )
            }
    }
}

@ExperimentalSerializationApi
@OptIn(UnstableApi::class)
suspend fun addToYtLikedSongs(mediaItems: List<MediaItem>){
    if (isYtSyncEnabled()) {
        mediaItems.forEachIndexed { index, item ->
            delay(1000)
            likeVideoOrSong(item.mediaId).onSuccess {
                Database.asyncTransaction {
                    if (songExist(item.mediaId) == 0) {
                        Database.insert(item)
                    }
                    like(item.mediaId, System.currentTimeMillis())

                }
                SmartMessage(
                    "${index + 1}/${mediaItems.size} " + appContext().resources.getString(R.string.songs_liked_yt),
                    context = appContext(),
                    durationLong = false
                )
            }.onFailure {
                SmartMessage(
                    "${index + 1}/${mediaItems.size} " + appContext().resources.getString(R.string.songs_liked_yt_failed),
                    context = appContext(),
                    durationLong = false
                )
            }
        }
    }
}