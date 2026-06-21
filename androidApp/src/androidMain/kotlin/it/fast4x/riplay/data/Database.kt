package it.fast4x.riplay.data

import android.content.ContentValues
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.os.Parcel
import androidx.core.database.getFloatOrNull
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import it.fast4x.environment.Environment
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.commonutils.MONTHLY_PREFIX
import it.fast4x.riplay.commonutils.PINNED_PREFIX
import it.fast4x.riplay.commonutils.PIPED_PREFIX
import it.fast4x.riplay.data.dao.ArtistDao
import it.fast4x.riplay.data.dao.SongArtistCrossRefDao
import it.fast4x.riplay.data.dao.SongDao
import it.fast4x.riplay.enums.AlbumSortBy
import it.fast4x.riplay.enums.ArtistSortBy
import it.fast4x.riplay.enums.BuiltInPlaylist
import it.fast4x.riplay.enums.PlaylistSongSortBy
import it.fast4x.riplay.enums.PlaylistSortBy
import it.fast4x.riplay.enums.SongSortBy
import it.fast4x.riplay.enums.SortOrder
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.ArtistDiscography
import it.fast4x.riplay.data.models.ArtistRelation
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.EventWithSong
import it.fast4x.riplay.data.models.ExternalApp
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.Info
import it.fast4x.riplay.data.models.KeywordWeight
import it.fast4x.riplay.data.models.Lyrics
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.PlaylistWithSongs
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.Recommendation
import it.fast4x.riplay.data.models.SearchQuery
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SongArtistCrossRef
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.data.models.SongWithContentLength
import it.fast4x.riplay.data.models.SortedSongPlaylistMap
import it.fast4x.riplay.data.models.UserArtistAffinity
import it.fast4x.riplay.data.models.UserEraAffinity
import it.fast4x.riplay.data.models.UserKeywordAffinity
import it.fast4x.riplay.enums.AlbumNature
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.extensions.musicbrainz.models.ExternalLink
import it.fast4x.riplay.musicvault.MusicVaultState
import it.fast4x.riplay.extensions.rewind.data.AlbumMostListened
import it.fast4x.riplay.extensions.rewind.data.AlbumsListenedCount
import it.fast4x.riplay.extensions.rewind.data.ArtistMostListened
import it.fast4x.riplay.extensions.rewind.data.ArtistsListenedCount
import it.fast4x.riplay.extensions.rewind.data.PlaylistMostListened
import it.fast4x.riplay.extensions.rewind.data.PlaylistsListenedCount
import it.fast4x.riplay.extensions.rewind.data.SongMostListened
import it.fast4x.riplay.extensions.rewind.data.SongsListenedCount
import it.fast4x.riplay.utils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.isExplicit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.MagicConstant
import kotlin.collections.sortedBy

@Dao
interface Database {
    private val _internal: RoomDatabase
        get() = DatabaseInitializer.Instance

    val getInstance: RoomDatabase
        get() = _internal

    // Proxy
    companion object : Database by DatabaseInitializer.createProxy() {
        // Lista Dao esposti
        fun songArtistCrossRefDao(): SongArtistCrossRefDao {
            return (DatabaseInitializer.Instance).songArtistCrossRefDao()
        }
        fun songDao(): SongDao {
            return (DatabaseInitializer.Instance).songDao()
        }
        fun artistDao(): ArtistDao {
            return (DatabaseInitializer.Instance).artistDao()
        }
    }


    //**********************************************
    @Transaction
    @Query("SELECT * FROM Blacklist WHERE type = :type")
    fun blacklists(type: String): Flow<List<Blacklist>>

    @Transaction
    @Query("SELECT * FROM Blacklist")
    fun blacklists(): Flow<List<Blacklist>>

    @Transaction
    @Query("SELECT id FROM Blacklist WHERE type = :type AND path = :path")
    fun blacklist(type: String, path: String): Long

    @Transaction
    @Query("SELECT COUNT(id) FROM Blacklist WHERE path = :path AND enabled = 1")
    fun blacklisted(path: String): Long

    @Transaction
    @Query("SELECT * FROM Blacklist WHERE enabled = 1 AND type IN (:types)")
    fun blacklisted(types: List<String>): Flow<List<Blacklist>>

    @Transaction
    @Query("SELECT * FROM Blacklist WHERE enabled = 1 AND type IN (:types)")
    fun blacklistedN(types: List<String>): List<Blacklist>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song")
    fun listAllSongsAsFlow(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY totalPlayTimeMs")
    fun songsOfflineByPlayTimeAsc(): Flow<List<SongEntity>>

    fun songsOfflineByRelativePlayTimeAsc(): Flow<List<SongEntity>>{
        val songs = songsOfflineByPlayTimeAsc()
        songs.map { it }
        return songs.map {
            it.sortedBy { se ->
                se.relativePlayTime()
            }
        }
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY totalPlayTimeMs DESC")
    fun songsOfflineByPlayTimeDesc(): Flow<List<SongEntity>>

    fun songsOfflineByRelativePlayTimeDesc(): Flow<List<SongEntity>>{
        val songs = songsOfflineByPlayTimeDesc()
        songs.map { it }
        return songs.map {
            it.sortedBy { se ->
                se.relativePlayTime()
            }
        }
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.title")
    fun songsOfflineByTitleAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.title DESC")
    fun songsOfflineByTitleDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.ROWID")
    fun songsOfflineByRowIdAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.ROWID DESC")
    fun songsOfflineByRowIdDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.likedAt")
    fun songsOfflineByLikedAtAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.likedAt DESC")
    fun songsOfflineByLikedAtDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.artistsText")
    fun songsOfflineByArtistAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.artistsText DESC")
    fun songsOfflineByArtistDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.durationText")
    fun songsOfflineByDurationAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song INNER JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.durationText DESC")
    fun songsOfflineByDurationDesc(): Flow<List<SongEntity>>

    fun songsOffline(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<SongEntity>> {
        return when (sortBy) {
            SongSortBy.PlayTime, SongSortBy.DatePlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByPlayTimeAsc()
                SortOrder.Descending -> songsOfflineByPlayTimeDesc()
            }
            SongSortBy.Title, SongSortBy.AlbumName -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByTitleAsc()
                SortOrder.Descending -> songsOfflineByTitleDesc()
            }
            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByRowIdAsc()
                SortOrder.Descending -> songsOfflineByRowIdDesc()
            }
            SongSortBy.DateLiked -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByLikedAtAsc()
                SortOrder.Descending -> songsOfflineByLikedAtDesc()
            }
            SongSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByArtistAsc()
                SortOrder.Descending -> songsOfflineByArtistDesc()
            }
            SongSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByDurationAsc()
                SortOrder.Descending -> songsOfflineByDurationDesc()
            }

            SongSortBy.RelativePlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsOfflineByRelativePlayTimeAsc()
                SortOrder.Descending -> songsOfflineByRelativePlayTimeDesc()
            }
        }
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY artistsText")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByArtistAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY artistsText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByArtistDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY totalPlayTimeMs")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByPlayTimeAsc(): Flow<List<SongEntity>>

    fun songsFavoritesByRelativePlayTimeAsc(): Flow<List<SongEntity>> {
        val songs = songsFavoritesByPlayTimeAsc()
        songs.map { it }
        return songs.map {
            it.sortedBy { se ->
                se.relativePlayTime()
            }
        }
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY totalPlayTimeMs DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByPlayTimeDesc(): Flow<List<SongEntity>>

    fun songsFavoritesByRelativePlayTimeDesc(): Flow<List<SongEntity>> {
        val songs = songsFavoritesByPlayTimeDesc()
        songs.map { it }
        return songs.map {
            it.sortedByDescending { se ->
                se.relativePlayTime()
            }
        }
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY title COLLATE NOCASE ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByTitleAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY title COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByTitleDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY ROWID")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByRowIdAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByRowIdDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY likedAt")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByLikedAtAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY likedAt DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByLikedAtDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        "SELECT DISTINCT S.* FROM Song S LEFT JOIN Event E ON E.songId=S.id " +
                "WHERE likedAt IS NOT NULL AND likedAt > 0 " +
                "ORDER BY E.timestamp DESC"
    )
    fun songsFavoritesByDatePlayedDesc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        "SELECT DISTINCT S.* FROM Song S LEFT JOIN Event E ON E.songId=S.id " +
                "WHERE likedAt IS NOT NULL AND likedAt > 0 " +
                "ORDER BY E.timestamp"
    )
    fun songsFavoritesByDatePlayedAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY durationText")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByDurationAsc(): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL AND likedAt > 0 ORDER BY durationText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsFavoritesByDurationDesc(): Flow<List<SongEntity>>

    fun songsFavorites(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<SongEntity>> {
        return when (sortBy) {
            SongSortBy.PlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByPlayTimeAsc()
                SortOrder.Descending -> songsFavoritesByPlayTimeDesc()
            }
            SongSortBy.RelativePlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByRelativePlayTimeAsc()
                SortOrder.Descending -> songsFavoritesByRelativePlayTimeDesc()
            }
            SongSortBy.Title, SongSortBy.AlbumName -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByTitleAsc()
                SortOrder.Descending -> songsFavoritesByTitleDesc()
            }
            SongSortBy.DateLiked -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByLikedAtAsc()
                SortOrder.Descending -> songsFavoritesByLikedAtDesc()
            }
            SongSortBy.DatePlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByDatePlayedAsc()
                SortOrder.Descending -> songsFavoritesByDatePlayedDesc()
            }
            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByRowIdAsc()
                SortOrder.Descending -> songsFavoritesByRowIdDesc()
            }
            SongSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByArtistAsc()
                SortOrder.Descending -> songsFavoritesByArtistDesc()
            }
            SongSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> songsFavoritesByDurationAsc()
                SortOrder.Descending -> songsFavoritesByDurationDesc()
            }
        }
    }

    @Transaction
    @Query("SELECT DISTINCT Song.*, Format.contentLength, Album.title FROM Song " +
            "LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "LEFT JOIN Format ON Format.songId = Song.id " +
            "WHERE likedAt IS NOT NULL AND likedAt = -1 " +
            " ORDER BY "+
            "    CASE :sortOrder WHEN 'ASC' THEN totalPlayTimeMs END ASC," +
            "    CASE :sortOrder WHEN 'DESC' THEN totalPlayTimeMs END DESC")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun songsDislikedByPlayTime(sortOrder: String): Flow<List<SongEntity>>

    fun songsDislikedByRelativePlayTime(sortOrder: SortOrder): Flow<List<SongEntity>> {
        val songs = songsDislikedByPlayTime(sortOrder.toSQLString())
        songs.map { it }
        return songs.map {
            when(sortOrder) {
               SortOrder.Ascending -> it.sortedBy { se -> se.relativePlayTime() }
                else -> it.sortedByDescending { se -> se.relativePlayTime() }
            }
        }
    }

    @Transaction
    @Query("SELECT DISTINCT Song.*, Format.contentLength, Album.title FROM Song " +
            "LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "LEFT JOIN Format ON Format.songId = Song.id " +
            "WHERE likedAt IS NOT NULL AND likedAt = -1 " +
            " ORDER BY "+
            "    CASE :sortOrder WHEN 'ASC' THEN Song.title END COLLATE NOCASE ASC," +
            "    CASE :sortOrder WHEN 'DESC' THEN Song.title END COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun songsDislikedByTitle(sortOrder: String): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT DISTINCT Song.*, Format.contentLength, Album.title FROM Song " +
            "LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "LEFT JOIN Format ON Format.songId = Song.id " +
            "WHERE likedAt IS NOT NULL AND likedAt = -1 " +
            " ORDER BY "+
            "    CASE :sortOrder WHEN 'ASC' THEN Song.ROWID END ASC," +
            "    CASE :sortOrder WHEN 'DESC' THEN Song.ROWID END DESC")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun songsDislikedByRowId(sortOrder: String): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT DISTINCT Song.*, Format.contentLength, Album.title FROM Song " +
            "LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "LEFT JOIN Format ON Format.songId = Song.id " +
            "LEFT JOIN Event E ON E.songId=Song.id " +
            "WHERE likedAt IS NOT NULL AND likedAt = -1" +
            " ORDER BY "+
            "    CASE :sortOrder WHEN 'ASC' THEN E.timestamp END ASC," +
            "    CASE :sortOrder WHEN 'DESC' THEN E.timestamp END DESC")
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun songsDislikedByDatePlayed(sortOrder: String): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT DISTINCT Song.*, Format.contentLength, Album.title FROM Song " +
            "LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "LEFT JOIN Format ON Format.songId = Song.id " +
            "WHERE likedAt IS NOT NULL AND likedAt = -1" +
            " ORDER BY " +
            "    CASE :sortOrder WHEN 'ASC' THEN artistsText END ASC," +
            "    CASE :sortOrder WHEN 'DESC' THEN artistsText END DESC")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun songsDislikedByArtist(sortOrder: String): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT DISTINCT Song.*, Format.contentLength, Album.title FROM Song " +
            "LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "LEFT JOIN Format ON Format.songId = Song.id " +
            "WHERE likedAt IS NOT NULL AND likedAt = -1 "+
            "ORDER BY " +
            "    CASE :sortOrder WHEN 'ASC' THEN durationText END ASC," +
            "    CASE :sortOrder WHEN 'DESC' THEN durationText END DESC")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun songsDislikedByDuration(sortOrder: String): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT DISTINCT Song.*, Format.contentLength, Album.title FROM Song " +
            "LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "LEFT JOIN Format ON Format.songId = Song.id " +
            "WHERE Song.likedAt = -1 AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' " +
            "ORDER BY " +
            "    CASE :sortOrder WHEN 'ASC' THEN Album.title END COLLATE NOCASE ASC," +
            "    CASE :sortOrder WHEN 'DESC' THEN Album.title END COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun songsDislikedByAlbumName(sortOrder: String): Flow<List<SongEntity>>

    fun SortOrder.toSQLString(): String {
        return when(this){
            SortOrder.Ascending -> "ASC"
            SortOrder.Descending -> "DESC"
        }
    }

    fun songsDisliked(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<SongEntity>> {
        return when(sortBy){
            SongSortBy.PlayTime -> songsDislikedByPlayTime(sortOrder.toSQLString())
            SongSortBy.RelativePlayTime -> songsDislikedByRelativePlayTime(sortOrder)
            SongSortBy.Title -> songsDislikedByTitle(sortOrder.toSQLString())
            SongSortBy.DateAdded -> songsDislikedByRowId(sortOrder.toSQLString())
            SongSortBy.DatePlayed -> songsDislikedByDatePlayed(sortOrder.toSQLString())
            SongSortBy.DateLiked -> flowOf(emptyList()) // TODO correct text, method
            SongSortBy.Artist -> songsDislikedByArtist(sortOrder.toSQLString())
            SongSortBy.Duration -> songsDislikedByDuration(sortOrder.toSQLString())
            SongSortBy.AlbumName -> songsDislikedByAlbumName(sortOrder.toSQLString())
        }
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT Song.*, Album.title as albumTitle FROM Song " +
            "LEFT JOIN Event E ON E.songId=Song.id LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' " +
            "ORDER BY E.timestamp DESC")
    fun songsByDatePlayedDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT DISTINCT Song.*, Album.title as albumTitle FROM Song " +
            "LEFT JOIN Event E ON E.songId=Song.id LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' " +
            "ORDER BY E.timestamp")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDatePlayedAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.likedAt ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByLikedAtAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.likedAt DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByLikedAtDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.artistsText ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByArtistAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.artistsText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByArtistDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.durationText ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDurationAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.durationText DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDurationDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Album.title COLLATE NOCASE ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByAlbumNameAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Album.title COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByAlbumNameDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.ROWID ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRowIdAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRowIdDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.title COLLATE NOCASE ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.title COLLATE NOCASE DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.totalPlayTimeMs ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    fun songsByRelativePlayTimeAsc(showHiddenSongs: Int = 0): Flow<List<SongEntity>> {
        val songs = songsByPlayTimeAsc(showHiddenSongs)
        songs.map { it }
        return songs.map {
            it.sortedBy { se ->
                se.relativePlayTime()
            }
        }
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT Song.*, Album.title as albumTitle FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
            "LEFT JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.totalPlayTimeMs DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    fun songsByRelativePlayTimeDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>{
        val songs = songsByPlayTimeDesc(showHiddenSongs)
        songs.map { it }
        return songs.map {
            it.sortedByDescending { se ->
                se.relativePlayTime()
            }
        }
    }

    fun songs(sortBy: SongSortBy, sortOrder: SortOrder, showHiddenSongs: Int): Flow<List<SongEntity>> {
        return when (sortBy) {
            SongSortBy.AlbumName -> when (sortOrder) {
                SortOrder.Ascending -> songsByAlbumNameAsc(showHiddenSongs)
                SortOrder.Descending -> songsByAlbumNameDesc(showHiddenSongs)
            }
            SongSortBy.PlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsByPlayTimeAsc(showHiddenSongs)
                SortOrder.Descending -> songsByPlayTimeDesc(showHiddenSongs)
            }
            SongSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> songsByTitleAsc(showHiddenSongs)
                SortOrder.Descending -> songsByTitleDesc(showHiddenSongs)
            }
            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsByRowIdAsc(showHiddenSongs)
                SortOrder.Descending -> songsByRowIdDesc(showHiddenSongs)
            }
            SongSortBy.DatePlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsByDatePlayedAsc(showHiddenSongs)
                SortOrder.Descending -> songsByDatePlayedDesc(showHiddenSongs)
            }
            SongSortBy.DateLiked -> when (sortOrder) {
                SortOrder.Ascending -> songsByLikedAtAsc(showHiddenSongs)
                SortOrder.Descending -> songsByLikedAtDesc(showHiddenSongs)
            }
            SongSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> songsByArtistAsc(showHiddenSongs)
                SortOrder.Descending -> songsByArtistDesc(showHiddenSongs)
            }
            SongSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> songsByDurationAsc(showHiddenSongs)
                SortOrder.Descending -> songsByDurationDesc(showHiddenSongs)
            }

            SongSortBy.RelativePlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsByRelativePlayTimeAsc(showHiddenSongs)
                SortOrder.Descending -> songsByRelativePlayTimeDesc(showHiddenSongs)
            }
        }
    }
    //**********************************************



    @Transaction
    @Query("SELECT * FROM Format WHERE songId = :songId ORDER BY bitrate DESC LIMIT 1")
    fun getBestFormat(songId: String): Flow<Format>

    @Transaction
    @Query("SELECT * FROM Format ORDER BY lastModified DESC LIMIT 1")
    fun getLastBestFormat(): Flow<Format?>

    @Transaction
    @Query("SELECT * FROM Song WHERE id in (SELECT songId FROM Format ORDER BY lastModified DESC LIMIT 1)")
    fun getLastSongPlayed(): Flow<Song?>

    @Transaction
    @Query("SELECT COUNT(id) from Song WHERE id = :id and title LIKE '${EXPLICIT_PREFIX}%'")
    fun isSongExplicit(id: String): Int

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT DISTINCT (timestamp / 86400000) as timestampDay, event.* FROM event ORDER BY rowId DESC")
    fun events(): Flow<List<EventWithSong>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT Event.* FROM Event JOIN Song ON Song.id = songId WHERE " +
            "Event.timestamp / 86400000 = :date / 86400000 LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun eventWithSongByPeriod(date: Long, limit:Long = Long.MAX_VALUE): Flow<List<EventWithSong>>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE isYoutubePlaylist = 1")
    fun ytmPrivatePlaylists(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT * FROM Song WHERE totalPlayTimeMs > 0 ORDER BY totalPlayTimeMs DESC LIMIT :count")
    @RewriteQueriesToDropUnusedColumns
    fun topSongs(count: Int = 10): Flow<List<Song>>

    @Transaction
    @Query("SELECT count(playlistId) FROM SongPlaylistMap WHERE songId = :id")
    fun songUsedInPlaylists(id: String): Int

    @Transaction
    @Query("SELECT count(playlistId) FROM SongPlaylistMap WHERE songId = :id")
    fun songUsedInPlaylistsAsFlow(id: String): Flow<Int>

    data class PlayListIdPosition(val playlistId: Long, val position: Int)
    @Transaction
    @Query("SELECT playlistId, position FROM SongPlaylistMap WHERE songId = :id")
    fun playlistsUsedForSong(id: String): List<PlayListIdPosition>

    @Transaction
    @Query("SELECT position FROM SongPlaylistMap WHERE playlistId = :playlistId AND songId = :id")
    fun positionInPlaylist(id: String, playlistId: Long): Int

    @Query("SELECT COUNT(1) FROM Song WHERE likedAt IS NOT NULL")
    fun likedSongsCount(): Flow<Int>

    @Query("SELECT COUNT(1) FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%'")
    fun onDeviceSongsCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM Song WHERE artistsText = :name ORDER BY title COLLATE NOCASE ASC")
    fun artistSongsByname(name: String): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song")
    fun flowListAllSongs(): Flow<List<Song>>

    @Query("SELECT id FROM Playlist WHERE name = :playlistName")
    fun playlistExistByName(playlistName: String): Long

    @Query("UPDATE Playlist SET name = :playlistName WHERE id = :playlistId")
    fun updatePlaylistName(playlistName: String, playlistId: Long): Int

    @Transaction
    @Query("UPDATE Song SET title = :title WHERE id = :id")
    fun updateSongTitle(id: String, title: String): Int

    @Transaction
    @Query("UPDATE Song SET thumbnailUrl = :url WHERE id = :id")
    fun updateSongThumbnail(id: String, url: String): Int

    @Transaction
    @Query("UPDATE Song SET artistsText = :artist WHERE id = :id")
    fun updateSongArtist(id: String, artist: String): Int

    @Query("UPDATE Album SET thumbnailUrl = :thumb WHERE id = :id")
    fun updateAlbumCover(id: String, thumb: String): Int

    @Query("UPDATE Album SET authorsText = :artist WHERE id = :id")
    fun updateAlbumAuthors(id: String, artist: String): Int

    @Query("UPDATE Album SET title = :title WHERE id = :id")
    fun updateAlbumTitle(id: String, title: String): Int

    @Query("UPDATE Artist SET name = :name WHERE id = :id")
    fun updateArtistName(id: String, name: String): Int

    @Transaction
    @Query("SELECT * FROM Artist WHERE id in (:idsList)")
    @RewriteQueriesToDropUnusedColumns
    fun getArtistsList(idsList: List<String>): Flow<List<Artist>>

    @Transaction
    @Query("SELECT * FROM Artist")
    @RewriteQueriesToDropUnusedColumns
    fun getArtistsList(): Flow<List<Artist>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id in (:idsList) ")
    @RewriteQueriesToDropUnusedColumns
    fun getSongsList(idsList: List<String>): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id in (:idsList) ")
    @RewriteQueriesToDropUnusedColumns
    fun getSongsListNoFlow(idsList: List<String>): List<Song>

    @Query("SELECT thumbnailUrl FROM Song WHERE id in (:idsList) ")
    fun getSongsListThumbnailUrls(idsList: List<String>): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM Song WHERE ROWID='wooowww' ")
    @RewriteQueriesToDropUnusedColumns
    fun fakeSongsList(): Flow<List<Song>>

    @Query("SELECT SUM(totalPlayTimeMs) FROM Song WHERE id in (:idsList) ")
    fun getSongsTotalPlaytime(idsList: List<String>): Flow<Long>



    @Transaction
    //@Query("SELECT Playlist.*, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = Playlist.id) as songCount " +
    //        "FROM Song JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
    //        "JOIN Event ON Song.id = Event.songId JOIN Playlist ON Playlist.id = SongPlaylistMap.playlistId " +
    //        "WHERE Event.timestamp BETWEEN :from AND :to GROUP BY Playlist.id ORDER BY Event.timestamp DESC LIMIT :limit")
    @Query("SELECT Playlist.*, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = Playlist.id) as songCount, 0 as isOnDevice " +
            "FROM Song JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "JOIN Event ON Song.id = Event.songId JOIN Playlist ON Playlist.id = SongPlaylistMap.playlistId " +
            "WHERE (:to - Event.timestamp) <= :from GROUP BY Playlist.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun playlistsMostPlayedByPeriod(from: Long,to: Long, limit:Int): Flow<List<PlaylistPreview>>

    @Transaction
    //@Query("SELECT Album.* FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
    //        "JOIN Event ON Song.id = Event.songId JOIN Album ON Album.id = SongAlbumMap.albumId " +
    //        "WHERE Event.timestamp BETWEEN :from AND :to GROUP BY Album.id ORDER BY Event.timestamp DESC LIMIT :limit")
    @Query("SELECT Album.* FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId " +
            "JOIN Event ON Song.id = Event.songId JOIN Album ON Album.id = SongAlbumMap.albumId " +
            "WHERE (:to - Event.timestamp) <= :from GROUP BY Album.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun albumsMostPlayedByPeriod(from: Long,to: Long, limit:Int): Flow<List<Album>>

    @Transaction
    //@Query("SELECT Artist.* FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId " +
    //        "JOIN Event ON Song.id = Event.songId JOIN Artist ON Artist.id = SongArtistMap.artistId " +
    //        "WHERE Event.timestamp BETWEEN :from AND :to GROUP BY Artist.id ORDER BY Event.timestamp DESC LIMIT :limit")
    @Query("SELECT Artist.* FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId " +
            "JOIN Event ON Song.id = Event.songId JOIN Artist ON Artist.id = SongArtistMap.artistId " +
            "WHERE (:to - Event.timestamp) <= :from GROUP BY Artist.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun artistsMostPlayedByPeriod(from: Long,to: Long, limit:Int): Flow<List<Artist>>

    @Transaction
    //@Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE timestamp " +
    //        "BETWEEN :from AND :to GROUP BY songId  ORDER BY timestamp DESC LIMIT :limit")
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE " +
            "(:to - Event.timestamp) <= :from GROUP BY songId  ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun songsMostPlayedByPeriod(from: Long, to: Long, limit:Long = Long.MAX_VALUE): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE " +
            "CAST(strftime('%m',timestamp / 1000,'unixepoch') AS INTEGER) = :month AND CAST(strftime('%Y',timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY songId  ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun songsMostPlayedByYearMonth(year: Long, month: Long, limit:Long = Long.MAX_VALUE): Flow<List<Song>>

    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE " +
            "CAST(strftime('%m',timestamp / 1000,'unixepoch') AS INTEGER) = :month AND CAST(strftime('%Y',timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY songId  ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun songsMostPlayedByYearMonthNoFlow(year: Long, month: Long, limit:Long = Long.MAX_VALUE): List<Song>


    @Query("SELECT COALESCE(SUM(playTime) / 60000, 0) as totalPlayTime FROM Event WHERE " +
            "CAST(strftime('%m', timestamp / 1000, 'unixepoch') AS INTEGER) = :month AND " +
            "CAST(strftime('%Y', timestamp / 1000, 'unixepoch') AS INTEGER) = :year")
    fun minutesListenedByYearMonth(year: Int, month: Int): Flow<Long>

    @Query("SELECT COALESCE(SUM(playTime) / 60000, 0) as totalPlayTime FROM Event WHERE " +
            "CAST(strftime('%Y',timestamp / 1000,'unixepoch') as INTEGER) = :year " )
    fun minutesListenedByYear(year: Int): Flow<Long>


    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%'")
    @RewriteQueriesToDropUnusedColumns
    fun songsOnDevice(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE mediaId = :mediaId")
    @RewriteQueriesToDropUnusedColumns
    fun songOnDevice(mediaId: String): Flow<Song?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%'")
    @RewriteQueriesToDropUnusedColumns
    fun songsEntityOnDevice(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY artistsText
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByArtist(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY totalPlayTimeMs
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByPlayTime(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY 
            CASE
                WHEN Song.title LIKE "${EXPLICIT_PREFIX}%" THEN SUBSTR(Song.title, LENGTH('${EXPLICIT_PREFIX}') + 1)
                ELSE Song.title
            END
        COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByTitle(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY Song.ROWID
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByRowId(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY likedAt
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByLikedAt(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        LEFT JOIN Event E ON E.songId = Song.id 
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY E.timestamp
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByDatePlayed(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY durationText
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByDuration(): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.likedAt IS NOT NULL 
        ORDER BY Album.title
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortFavoriteSongsByAlbum(): Flow<List<SongEntity>>

    /**
     * Fetch all songs that are liked by the user
     * from the database and sort them according to
     * [sortBy] and [sortOrder].
     *
     * [sortBy] sorts all based on each song's property
     * such as [SongSortBy.Title], [SongSortBy.PlayTime], etc.
     * While [sortOrder] arranges order of sorted songs
     * to follow alphabetical order A to Z, or numerical order 0 to 9, etc.
     *
     *
     * @param sortBy which song's property is used to sort
     * @param sortOrder what order should results be in
     *
     * @return a **SORTED** list of [SongEntity]'s that are continuously
     * updated to reflect changes within the database - wrapped by [Flow]
     *
     * @see SongSortBy
     * @see SortOrder
     */
    fun listFavoriteSongs(
        sortBy: SongSortBy,
        sortOrder: SortOrder
    ): Flow<List<SongEntity>> = when (sortBy) {
        SongSortBy.PlayTime -> sortFavoriteSongsByPlayTime()
        SongSortBy.Title -> sortFavoriteSongsByTitle()
        SongSortBy.DateAdded -> sortFavoriteSongsByRowId()
        SongSortBy.DatePlayed -> sortFavoriteSongsByDatePlayed()
        SongSortBy.DateLiked -> sortFavoriteSongsByLikedAt()
        SongSortBy.Artist -> sortFavoriteSongsByArtist()
        SongSortBy.Duration -> sortFavoriteSongsByDuration()
        SongSortBy.AlbumName -> sortFavoriteSongsByAlbum()
        SongSortBy.RelativePlayTime -> TODO()
    }.map(sortOrder::applyTo)

    @Query("SELECT thumbnailUrl FROM Song WHERE likedAt IS NOT NULL AND id NOT LIKE '$LOCAL_KEY_PREFIX%'  LIMIT 4")
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun preferitesThumbnailUrls(): Flow<List<String>>

    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song LEFT JOIN Format ON id = songId WHERE songId = :songId")
    fun songCached(songId: String): Flow<SongWithContentLength?>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId
        WHERE SongArtistMap.artistId = :artistId 
        ORDER BY totalPlayTimeMs
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByPlayTime(artistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SongArtistMap.artistId = :artistId 
        ORDER BY 
            CASE
                WHEN Song.title LIKE "${EXPLICIT_PREFIX}%" THEN SUBSTR(Song.title, LENGTH('${EXPLICIT_PREFIX}') + 1)
                ELSE Song.title
            END
        COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByTitle(artistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId         
        WHERE SongArtistMap.artistId = :artistId 
        ORDER BY Song.ROWID
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByRowId(artistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId         
        WHERE SongArtistMap.artistId = :artistId 
        ORDER BY Song.likedAt
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByLikedAt(artistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId         
        WHERE SongArtistMap.artistId = :artistId 
        ORDER BY Song.artistsText
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByArtist(artistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId         
        WHERE SongArtistMap.artistId = :artistId
        ORDER BY Song.durationText
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByDuration(artistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN Event E ON E.songId=Song.id 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SongArtistMap.artistId = :artistId
        ORDER BY E.timestamp
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByDatePlayed(artistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, null, Album.title
        FROM Song 
        JOIN SongArtistMap ON Song.id = SongArtistMap.songId
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        WHERE SongArtistMap.artistId = :artistId
        ORDER BY Album.title COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listArtistLibrarySongsByAlbum(artistId: String): Flow<List<SongEntity>>


    fun listArtistLibrarySongs(
        artistId: String,
        sortBy: SongSortBy,
        sortOrder: SortOrder
    ): Flow<List<SongEntity>> = when (sortBy) {
        SongSortBy.PlayTime, SongSortBy.RelativePlayTime -> listArtistLibrarySongsByPlayTime(artistId)
        SongSortBy.Title -> listArtistLibrarySongsByTitle(artistId)
        SongSortBy.DateAdded -> listArtistLibrarySongsByRowId(artistId)
        SongSortBy.DatePlayed -> listArtistLibrarySongsByDatePlayed(artistId)
        SongSortBy.DateLiked -> listArtistLibrarySongsByLikedAt(artistId)
        SongSortBy.Artist -> listArtistLibrarySongsByArtist(artistId)
        SongSortBy.Duration -> listArtistLibrarySongsByDuration(artistId)
        SongSortBy.AlbumName -> listArtistLibrarySongsByAlbum(artistId)
    }.map( sortOrder::applyTo )

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY Song.ROWID
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByRowId(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY Song.ROWID
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByRowId_Filtered(filterList: List<String>): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY 
            CASE
                WHEN Song.title LIKE "${EXPLICIT_PREFIX}%" THEN SUBSTR(Song.title, LENGTH('${EXPLICIT_PREFIX}') + 1)
                ELSE Song.title
            END
        COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByTitle(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY 
            CASE
                WHEN Song.title LIKE "${EXPLICIT_PREFIX}%" THEN SUBSTR(Song.title, LENGTH('${EXPLICIT_PREFIX}') + 1)
                ELSE Song.title
            END
        COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByTitle_Filtered(filterList: List<String>): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY Song.totalPlayTimeMs
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByPlayTime(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY Song.totalPlayTimeMs
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByPlayTime_Filtered(
        filterList: List<String>): Flow<List<SongEntity>>

    fun sortAllSongsByRelativePlayTime(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>{
        val songs = sortAllSongsByPlayTime(showHidden)
        return songs.map {
                it.sortedBy { se ->
                    val totalPlayTimeMs = se.song.totalPlayTimeMs
                    if(totalPlayTimeMs > 0) se.contentLength?.div(totalPlayTimeMs) ?: 0L else 0L
            }
        }
    }

    fun sortAllSongsByRelativePlayTime_Filtered(
        filterList: List<String>): Flow<List<SongEntity>>{
        val songs = sortAllSongsByPlayTime_Filtered(filterList)
        return songs.map {
            it.sortedBy { se ->
                val totalPlayTimeMs = se.song.totalPlayTimeMs
                if(totalPlayTimeMs > 0) se.contentLength?.div(totalPlayTimeMs) ?: 0L else 0L
            }
        }
    }

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN Event E ON E.songId=Song.id 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY E.timestamp
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByDatePlayed(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN Event E ON E.songId=Song.id 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY E.timestamp
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByDatePlayed_Filtered(filterList: List<String>): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY Song.likedAt
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByLikedAt(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY Song.likedAt
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByLikedAt_Filtered(filterList: List<String>): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY Song.artistsText COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByArtist(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY Song.artistsText COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByArtist_Filtered(filterList: List<String>): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY Song.durationText
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByDuration(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY Song.durationText
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByDuration_Filtered(filterList: List<String>): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden 
        ORDER BY Album.title COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByAlbum(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.id in (:filterList)
        ORDER BY Album.title COLLATE NOCASE
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun sortAllSongsByAlbum_Filtered(filterList: List<String>): Flow<List<SongEntity>>

    /**
     * Fetch all songs from the database and sort them
     * according to [sortBy] and [sortOrder]. It also
     * excludes songs if condition of [showHidden] is met.
     *
     * [sortBy] sorts all based on each song's property
     * such as [SongSortBy.Title], [SongSortBy.PlayTime], etc.
     * While [sortOrder] arranges order of sorted songs
     * to follow alphabetical order A to Z, or numerical order 0 to 9, etc.
     *
     * [showHidden] is an optional parameter that indicates
     * whether the final results contain songs that are hidden
     * (in)directly by the user.
     * `-1` shows hidden while `0` does not.
     *
     * @param sortBy which song's property is used to sort
     * @param sortOrder what order should results be in
     * @param showHidden include hidden songs to final results or not
     *
     * @return a **SORTED** list of [SongEntity]'s that are continuously
     * updated to reflect changes within the database - wrapped by [Flow]
     *
     * @see SongSortBy
     * @see SortOrder
     */
    fun listAllSongs(
        sortBy: SongSortBy,
        sortOrder: SortOrder,
        @MagicConstant(intValues = [1, 0]) showHidden: Int,
        filterList: List<String>,
        playList: BuiltInPlaylist
    ): Flow<List<SongEntity>> = when( sortBy ) {
        // Due to the unknown amount of songs, letting SQLite handle
        // the sorting is a better idea
        SongSortBy.PlayTime -> if (filterList.isEmpty()) sortAllSongsByPlayTime( showHidden )
        else sortAllSongsByPlayTime_Filtered(filterList )
        SongSortBy.RelativePlayTime -> if (filterList.isEmpty()) sortAllSongsByRelativePlayTime(showHidden)
        else sortAllSongsByRelativePlayTime_Filtered(filterList)
        SongSortBy.Title -> if (filterList.isEmpty()) sortAllSongsByTitle( showHidden )
        else sortAllSongsByTitle_Filtered(filterList )
        SongSortBy.DateAdded -> if (filterList.isEmpty()) sortAllSongsByRowId( showHidden )
        else sortAllSongsByRowId_Filtered(filterList )
        SongSortBy.DatePlayed -> if (filterList.isEmpty()) sortAllSongsByDatePlayed( showHidden )
        else sortAllSongsByDatePlayed_Filtered(filterList )
        SongSortBy.DateLiked -> if (filterList.isEmpty()) sortAllSongsByLikedAt( showHidden )
        else sortAllSongsByLikedAt_Filtered(filterList )
        SongSortBy.Artist -> if (filterList.isEmpty()) sortAllSongsByArtist( showHidden )
        else sortAllSongsByArtist_Filtered(filterList )
        SongSortBy.Duration -> if (filterList.isEmpty()) sortAllSongsByDuration( showHidden )
        else sortAllSongsByDuration_Filtered(filterList )
        SongSortBy.AlbumName -> if (filterList.isEmpty()) sortAllSongsByAlbum( showHidden )
        else sortAllSongsByAlbum_Filtered(filterList )
    }.map( sortOrder::applyTo )

    /**
     * Fetch all songs from the database, 
     * excludes songs if condition of [showHidden] is met.
     *
     * [showHidden] is an optional parameter that indicates
     * whether the final results contain songs that are hidden
     * (in)directly by the user.
     * `-1` shows hidden while `0` does not.
     *
     * @param showHidden include hidden songs to final results or not
     *
     * @return an **UNSORTED** list of [SongEntity]'s that are continuously
     * updated to reflect changes within the database - wrapped by [Flow]
     */
    @Query("""
        SELECT DISTINCT Song.*, Format.contentLength, Album.title
        FROM Song
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId
        LEFT JOIN Format ON Format.songId = Song.id
        WHERE Song.totalPlayTimeMs >= :showHidden
    """)
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun listAllSongs(
        @MagicConstant(intValues = [1, 0]) showHidden: Int
    ): Flow<List<SongEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM Song
        WHERE id NOT LIKE '$LOCAL_KEY_PREFIX%'
        ORDER BY totalPlayTimeMs DESC
        LIMIT :limit
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeWithLimitDesc(limit: Int = -1): Flow<List<Song>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        """
        SELECT * FROM Song
        WHERE id NOT LIKE '$LOCAL_KEY_PREFIX%'
        ORDER BY totalPlayTimeMs DESC
        LIMIT :limit
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun songsEntityByPlayTimeWithLimitDesc(limit: Int = -1): Flow<List<SongEntity>>

//    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
//    @Transaction
//    @Query("SELECT Song.*, Album.title as albumTitle FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId  " +
//            "JOIN Album ON Album.id = SongAlbumMap.albumId " +
//            "WHERE (Song.totalPlayTimeMs > :showHiddenSongs OR Song.likedAt NOT NULL) AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY Song.artistsText DESC")
//    @RewriteQueriesToDropUnusedColumns
//    fun songsWithAlbumByPlayTimeDesc(showHiddenSongs: Int = 0): Flow<List<SongEntity>>

    @Query("SELECT thumbnailUrl FROM Song JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0  LIMIT 4")
    fun offlineThumbnailUrls(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM Song WHERE likedAt IS NOT NULL ORDER BY likedAt DESC")
    @RewriteQueriesToDropUnusedColumns
    fun favorites(): Flow<List<Song>>

    @Query("SELECT * FROM QueuedMediaItem")
    fun queuedMediaItems(): List<QueuedMediaItem>

    @Query("DELETE FROM QueuedMediaItem")
    fun clearQueuedMediaItems()

    @Query("DELETE FROM QueuedMediaItem WHERE mediaItem IS NULL")
    fun clearOldEmptyQueuedMediaItems()

    @Query("SELECT * FROM SearchQuery WHERE `query` LIKE :query ORDER BY id DESC")
    fun queries(query: String): Flow<List<SearchQuery>>

    @Query("SELECT COUNT (*) FROM SearchQuery")
    fun queriesCount(): Flow<Int>

    @Query("DELETE FROM SearchQuery")
    fun clearQueries()

    @Query("UPDATE Playlist SET name = '${PINNED_PREFIX}'||name WHERE id = :playlistId")
    fun pinPlaylist(playlistId: Long): Int
    @Query("UPDATE Playlist SET name = REPLACE(name,'${PINNED_PREFIX}','') WHERE id = :playlistId")
    fun unPinPlaylist(playlistId: Long): Int

    @Query("SELECT count(id) FROM Song WHERE id = :songId and likedAt IS NOT NULL and likedAt > 0")
    fun songliked(songId: String): Int

    @Query("SELECT * FROM Song WHERE id = :id")
    fun song(id: String?): Flow<Song?>

    @Query("SELECT * FROM Song WHERE id = :id")
    suspend fun getSong(id: String): Song?

    @Query("SELECT * FROM Song WHERE id = :id")
    fun songNoFlow(id: String?): Song?

    @Query("SELECT count(id) FROM Song WHERE id = :id")
    fun songExist(id: String): Int

    @Query("SELECT likedAt FROM Song WHERE id = :songId")
    fun likedAt(songId: String): Flow<Long?>

    @Query("SELECT likedAt FROM Song WHERE id = :songId")
    fun getLikedAt(songId: String): Long?

    @Query("SELECT id FROM Song WHERE id = :songId AND likedAt < 0")
    fun songDisliked(songId: String): String?

    @Query("UPDATE Album SET bookmarkedAt = :bookmarkedAt WHERE id = :id")
    fun bookmarkAlbum(id: String, bookmarkedAt: Long?): Int

    @Query("UPDATE Song SET likedAt = :likedAt WHERE id = :songId")
    fun like(songId: String, likedAt: Long?): Int

    @Query("UPDATE Song SET durationText = :durationText WHERE id = :songId")
    fun updateDurationText(songId: String, durationText: String): Int

    @Query("SELECT * FROM Lyrics WHERE songId = :songId")
    fun lyrics(songId: String): Flow<Lyrics?>

    @Query("SELECT * FROM Artist WHERE id = :id")
    fun artist(id: String): Flow<Artist?>

    @Query("SELECT * FROM Artist WHERE name = :name")
    fun artistByName(name: String): Flow<Artist?>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY name COLLATE NOCASE ASC")
    fun preferitesArtistsByName(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY name COLLATE NOCASE DESC")
    fun artistsByNameDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY name COLLATE NOCASE ASC")
    fun artistsByNameAsc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt DESC")
    fun artistsByRowIdDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt ASC")
    fun artistsByRowIdAsc(): Flow<List<Artist>>

    fun artists(sortBy: ArtistSortBy, sortOrder: SortOrder): Flow<List<Artist>> {
        return when (sortBy) {
            ArtistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> artistsByNameAsc()
                SortOrder.Descending -> artistsByNameDesc()
            }
            ArtistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> artistsByRowIdAsc()
                SortOrder.Descending -> artistsByRowIdDesc()
            }
        }
    }

    @Query("SELECT * FROM Artist WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY name DESC")
    fun artistsOnDeviceByNameDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY name ASC")
    fun artistsOnDeviceByNameAsc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY id DESC")
    fun artistsOnDeviceByRowIdDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY id ASC")
    fun artistsOnDeviceByRowIdAsc(): Flow<List<Artist>>

    fun artistsOnDevice(sortBy: ArtistSortBy, sortOrder: SortOrder): Flow<List<Artist>> {
        return when (sortBy) {
            ArtistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> artistsOnDeviceByNameAsc()
                SortOrder.Descending -> artistsOnDeviceByNameDesc()
            }
            ArtistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> artistsOnDeviceByRowIdAsc()
                SortOrder.Descending -> artistsOnDeviceByRowIdDesc()
            }
        }
    }

    @Query("SELECT * FROM Artist A WHERE A.id in ( " +
            "SELECT DISTINCT artistId FROM SongArtistMap INNER JOIN Song " +
            "ON Song.id = SongArtistMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.name ASC")
    fun artistsInLibraryByNameAsc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist A WHERE A.id in ( " +
            "SELECT DISTINCT artistId FROM SongArtistMap INNER JOIN Song " +
            "ON Song.id = SongArtistMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.name DESC")
    fun artistsInLibraryByNameDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist A WHERE A.id in ( " +
            "SELECT DISTINCT artistId FROM SongArtistMap INNER JOIN Song " +
            "ON Song.id = SongArtistMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.bookmarkedAt ASC")
    fun artistsInLibraryByRowIdAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        "SELECT DISTINCT S.* FROM Song S INNER JOIN SongArtistMap SM ON S.id=SM.songId INNER JOIN " +
        "(SELECT * FROM Artist A WHERE A.id IN (:artists) AND A.id in "+
            "(SELECT DISTINCT artistId FROM SongArtistMap INNER JOIN Song " +
                "ON Song.id = SongArtistMap.songId " +
                "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
                "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ")"+
        ") A on A.id=SM.artistId")
    fun songsInLibraryArtistsFiltered(artists: List<String>): Flow<List<Song>>

    @Transaction
    @Query(
        "SELECT DISTINCT S.* FROM Song S INNER JOIN SongArtistMap SM ON S.id=SM.songId INNER JOIN " +
                "(SELECT * FROM Artist A WHERE A.id LIKE '$LOCAL_KEY_PREFIX%' AND A.id IN (:artists) AND A.id in "+
                "(SELECT DISTINCT artistId FROM SongArtistMap INNER JOIN Song " +
                "ON Song.id = SongArtistMap.songId " +
                "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
                "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
                ")"+
                ") A on A.id=SM.artistId")
    fun songsOnDeviceArtistsFiltered(artists: List<String>): Flow<List<Song>>

    @Query("SELECT * FROM Artist A WHERE A.id in ( " +
            "SELECT DISTINCT artistId FROM SongArtistMap INNER JOIN Song " +
            "ON Song.id = SongArtistMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.bookmarkedAt DESC")
    fun artistsInLibraryByRowIdDesc(): Flow<List<Artist>>

    fun artistsInLibrary(sortBy: ArtistSortBy, sortOrder: SortOrder): Flow<List<Artist>> {
        return when (sortBy) {
            ArtistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> artistsInLibraryByNameAsc()
                SortOrder.Descending -> artistsInLibraryByNameDesc()
            }
            ArtistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> artistsInLibraryByRowIdAsc()
                SortOrder.Descending -> artistsInLibraryByRowIdDesc()
            }
        }
    }

    @Query("SELECT * FROM Artist A " +
            "WHERE A.id in ( SELECT DISTINCT artistId FROM SongArtistMap ) " +
            "ORDER BY A.name ASC")
    fun artistsWithSongsSavedByNameAsc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist A " +
            "WHERE A.id in ( SELECT DISTINCT artistId FROM SongArtistMap ) " +
            "ORDER BY A.name DESC")
    fun artistsWithSongsSavedByNameDesc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist A " +
            "WHERE A.id in ( SELECT DISTINCT artistId FROM SongArtistMap ) " +
            "ORDER BY A.bookmarkedAt ASC")
    fun artistsWithSongsSavedByRowIdAsc(): Flow<List<Artist>>

    @Query("SELECT * FROM Artist A " +
            "WHERE A.id in ( SELECT DISTINCT artistId FROM SongArtistMap ) " +
            "ORDER BY A.bookmarkedAt DESC")
    fun artistsWithSongsSavedByRowIdDesc(): Flow<List<Artist>>

    fun artistsWithSongsSaved(sortBy: ArtistSortBy, sortOrder: SortOrder): Flow<List<Artist>> {
        return when (sortBy) {
            ArtistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> artistsWithSongsSavedByNameAsc()
                SortOrder.Descending -> artistsWithSongsSavedByNameDesc()
            }
            ArtistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> artistsWithSongsSavedByRowIdAsc()
                SortOrder.Descending -> artistsWithSongsSavedByRowIdDesc()
            }
        }
    }

    @Query("SELECT * FROM Album WHERE id = :id")
    fun album(id: String): Flow<Album?>

    @Query("SELECT * FROM Album")
    fun getAlbumsList(): Flow<List<Album>>

    @Query("SELECT timestamp FROM Album WHERE id = :id")
    fun albumTimestamp(id: String): Long?

    @Query("SELECT bookmarkedAt FROM Album WHERE id = :id")
    fun albumBookmarkedAt(id: String): Flow<Long?>

    @Query("SELECT count(id) FROM Album WHERE id = :id and bookmarkedAt IS NOT NULL")
    fun albumBookmarked(id: String): Int

    @Query("SELECT count(id) FROM Album WHERE id = :id")
    fun albumExist(id: String): Int

    @Transaction
    @Query("SELECT DISTINCT * FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = :albumId ORDER BY position")
    @RewriteQueriesToDropUnusedColumns
    fun albumSongsList(albumId: String): List<Song>

    @Transaction
    @Query("SELECT DISTINCT * FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = :albumId AND position IS NOT NULL ORDER BY position")
    @RewriteQueriesToDropUnusedColumns
    fun albumSongs(albumId: String): Flow<List<Song>>

    @Transaction
    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY totalDuration ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsByTotalDurationAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY totalDuration DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsByTotalDurationDesc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = Album.id) as songCount " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY songCount ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsBySongsCountAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = Album.id) as songCount " +
            "FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY songCount DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsBySongsCountDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY authorsText COLLATE NOCASE ASC")
    fun albumsByArtistAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY authorsText COLLATE NOCASE DESC")
    fun albumsByArtistDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY title COLLATE NOCASE ASC")
    fun albumsByTitleAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY year ASC")
    fun albumsByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt ASC")
    fun albumsByRowIdAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY title COLLATE NOCASE DESC")
    fun albumsByTitleDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY year DESC")
    fun albumsByYearDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt DESC")
    fun albumsByRowIdDesc(): Flow<List<Album>>

    fun albums(sortBy: AlbumSortBy, sortOrder: SortOrder): Flow<List<Album>> {
        return when (sortBy) {
            AlbumSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> albumsByTitleAsc()
                SortOrder.Descending -> albumsByTitleDesc()
            }
            AlbumSortBy.Year -> when (sortOrder) {
                SortOrder.Ascending -> albumsByYearAsc()
                SortOrder.Descending -> albumsByYearDesc()
            }
            AlbumSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> albumsByRowIdAsc()
                SortOrder.Descending -> albumsByRowIdDesc()
            }
            AlbumSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> albumsByArtistAsc()
                SortOrder.Descending -> albumsByArtistDesc()
            }
            AlbumSortBy.Songs -> when (sortOrder) {
                SortOrder.Ascending -> albumsBySongsCountAsc()
                SortOrder.Descending -> albumsBySongsCountDesc()
            }
            AlbumSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> albumsByTotalDurationAsc()
                SortOrder.Descending -> albumsByTotalDurationDesc()
            }
        }
    }

    @Transaction
    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY totalDuration ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsOnDeviceByTotalDurationAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY totalDuration DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsOnDeviceByTotalDurationDesc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = Album.id) as songCount " +
            "FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY songCount ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsOnDeviceBySongsCountAsc(): Flow<List<Album>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = Album.id) as songCount " +
            "FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY songCount DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsOnDeviceBySongsCountDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY authorsText COLLATE NOCASE ASC")
    fun albumsOnDeviceByArtistAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY authorsText COLLATE NOCASE DESC")
    fun albumsOnDeviceByArtistDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY id ASC")
    fun albumsOnDeviceByRowIdAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY id DESC")
    fun albumsOnDeviceByRowIdDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY year ASC")
    fun albumsOnDeviceByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY year DESC")
    fun albumsOnDeviceByYearDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY title COLLATE NOCASE ASC")
    fun albumsOnDeviceByTitleDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY title COLLATE NOCASE ASC")
    fun albumsOnDeviceByTitleAsc(): Flow<List<Album>>


    fun albumsOnDevice(sortBy: AlbumSortBy, sortOrder: SortOrder): Flow<List<Album>> {
        return when (sortBy) {
            AlbumSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> albumsOnDeviceByTitleAsc()
                SortOrder.Descending -> albumsOnDeviceByTitleDesc()
            }
            AlbumSortBy.Year -> when (sortOrder) {
                SortOrder.Ascending -> albumsOnDeviceByYearAsc()
                SortOrder.Descending -> albumsOnDeviceByYearDesc()
            }
            AlbumSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> albumsOnDeviceByRowIdAsc()
                SortOrder.Descending -> albumsOnDeviceByRowIdDesc()
            }
            AlbumSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> albumsOnDeviceByArtistAsc()
                SortOrder.Descending -> albumsOnDeviceByArtistDesc()
            }
            AlbumSortBy.Songs -> when (sortOrder) {
                SortOrder.Ascending -> albumsOnDeviceBySongsCountAsc()
                SortOrder.Descending -> albumsOnDeviceBySongsCountDesc()
            }
            AlbumSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> albumsOnDeviceByTotalDurationAsc()
                SortOrder.Descending -> albumsOnDeviceByTotalDurationDesc()
            }
        }
    }

    @Query("SELECT * FROM Album A WHERE A.id in ( " +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song ON Song.id = SongAlbumMap.songId " +
            "INNER JOIN SongArtistMap ON Song.id = SongArtistMap.songId " +
            "AND SongArtistMap.artistId = :artistId " +
            ") " +
            "ORDER BY A.title COLLATE NOCASE ASC")
    fun artistAlbums(artistId: String): Flow<List<Album>>


    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.title COLLATE NOCASE ASC")
    fun albumsInLibraryByTitleAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.title COLLATE NOCASE DESC")
    fun albumsInLibraryByTitleDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.year ASC")
    fun albumsInLibraryByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.year DESC")
    fun albumsInLibraryByYearDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.bookmarkedAt ASC")
    fun albumsInLibraryByRowIdAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.bookmarkedAt DESC")
    fun albumsInLibraryByRowIdDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.authorsText COLLATE NOCASE ASC")
    fun albumsInLibraryByArtistAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY A.authorsText COLLATE NOCASE DESC")
    fun albumsInLibraryByArtistDesc(): Flow<List<Album>>

    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = A.id) as songCount FROM Album A " +
            "WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
                "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
                "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
                ") " +
            "ORDER BY songCount ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsInLibraryBySongsCountAsc(): Flow<List<Album>>

    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = A.id) as songCount FROM Album A " +
            "WHERE A.id in (" +
            "SELECT DISTINCT SongAlbumMap.albumId FROM SongAlbumMap INNER JOIN Song " +
            "ON Song.id = SongAlbumMap.songId " +
            "LEFT JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE (Song.totalPlayTimeMs > 0 AND Song.likedAt > 0) OR SongPlaylistMap.playlistId IS NOT NULL " +
            ") " +
            "ORDER BY songCount DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsInLibraryBySongsCountDesc(): Flow<List<Album>>

    fun albumsInLibrary(sortBy: AlbumSortBy, sortOrder: SortOrder): Flow<List<Album>> {
        return when (sortBy) {
            AlbumSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> albumsInLibraryByTitleAsc()
                SortOrder.Descending -> albumsInLibraryByTitleDesc()
            }
            AlbumSortBy.Year -> when (sortOrder) {
                SortOrder.Ascending -> albumsInLibraryByYearAsc()
                SortOrder.Descending -> albumsInLibraryByYearDesc()
            }
            AlbumSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> albumsInLibraryByRowIdAsc()
                SortOrder.Descending -> albumsInLibraryByRowIdDesc()
            }
            AlbumSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> albumsInLibraryByArtistAsc()
                SortOrder.Descending -> albumsInLibraryByArtistDesc()
            }
            AlbumSortBy.Songs -> when (sortOrder) {
                SortOrder.Ascending -> albumsInLibraryBySongsCountAsc()
                SortOrder.Descending -> albumsInLibraryBySongsCountDesc()
            }
            AlbumSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> albumsByTotalDurationAsc()
                SortOrder.Descending -> albumsByTotalDurationDesc()
            }
        }
    }



    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.title COLLATE NOCASE ASC")
    fun albumsWithSongsSavedByTitleAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.title COLLATE NOCASE DESC")
    fun albumsWithSongsSavedByTitleDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.year ASC")
    fun albumsWithSongsSavedByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.year DESC")
    fun albumsWithSongsSavedByYearDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.bookmarkedAt ASC")
    fun albumsWithSongsSavedByRowIdAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.bookmarkedAt DESC")
    fun albumsWithSongsSavedByRowIdDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.authorsText COLLATE NOCASE ASC")
    fun albumsWithSongsSavedByArtistAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY A.authorsText COLLATE NOCASE DESC")
    fun albumsWithSongsSavedByArtistDesc(): Flow<List<Album>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = A.id) as songCount  FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY songCount ASC")
    fun albumsWithSongsSavedBySongsCountAsc(): Flow<List<Album>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId = A.id) as songCount  FROM Album A" +
            " WHERE A.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            " ORDER BY songCount DESC")
    fun albumsWithSongsSavedBySongsCountDesc(): Flow<List<Album>>

    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE Album.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            "ORDER BY totalDuration ASC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsWithSongsSavedByTotalDurationAsc(): Flow<List<Album>>

    @Query("SELECT *, (SELECT SUM(CAST(REPLACE(durationText, ':', '') AS INTEGER)) FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = Album.id AND position IS NOT NULL) as totalDuration " +
            "FROM Album WHERE Album.id in ( SELECT DISTINCT albumId FROM SongAlbumMap ) " +
            "ORDER BY totalDuration DESC" )
    @RewriteQueriesToDropUnusedColumns
    fun albumsWithSongsSavedByTotalDurationDesc(): Flow<List<Album>>

    fun albumsWithSongsSaved(sortBy: AlbumSortBy, sortOrder: SortOrder): Flow<List<Album>> {
        return when (sortBy) {
            AlbumSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithSongsSavedByTitleAsc()
                SortOrder.Descending -> albumsWithSongsSavedByTitleDesc()
            }
            AlbumSortBy.Year -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithSongsSavedByYearAsc()
                SortOrder.Descending -> albumsWithSongsSavedByYearDesc()
            }
            AlbumSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithSongsSavedByRowIdAsc()
                SortOrder.Descending -> albumsWithSongsSavedByRowIdDesc()
            }
            AlbumSortBy.Artist -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithSongsSavedByArtistAsc()
                SortOrder.Descending -> albumsWithSongsSavedByArtistDesc()
            }
            AlbumSortBy.Songs -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithSongsSavedBySongsCountAsc()
                SortOrder.Descending -> albumsWithSongsSavedBySongsCountDesc()
            }
            AlbumSortBy.Duration -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithSongsSavedByTotalDurationAsc()
                SortOrder.Descending -> albumsWithSongsSavedByTotalDurationDesc()
            }
        }
    }

    @Query("UPDATE Song SET totalPlayTimeMs = 0 WHERE id = :id")
    fun resetTotalPlayTimeMs(id: String)

    @Query("UPDATE Song SET totalPlayTimeMs = totalPlayTimeMs + :addition WHERE id = :id")
    fun incrementTotalPlayTimeMs(id: String, addition: Long)

    @Transaction
    @Query("SELECT max(position) maxPos FROM SongPlaylistMap WHERE playlistId = :id")
    fun getSongMaxPositionToPlaylist(id: Long): Int

    @Transaction
    @Query("SELECT PM.playlistId FROM SongPlaylistMap PM WHERE PM.songId = :id")
    fun getPlaylistsWithSong(id: String): Flow<List<Long>>

    @Transaction
    @Query("SELECT max(position) maxPos FROM SongPlaylistMap WHERE playlistId = :id")
    fun updateSongMaxPositionToPlaylist(id: Long): Int

    @Transaction
    @Query("SELECT * FROM Playlist WHERE id = :id")
    fun playlistWithSongs(id: Long): Flow<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE browseId = :browseId")
    fun playlistWithBrowseId(browseId: String): Playlist?

    @Transaction
    @Query("SELECT * FROM Playlist WHERE trim(name) COLLATE NOCASE = trim(:name) COLLATE NOCASE")
    fun playlistWithSongsNoFlow(name: String): PlaylistWithSongs?

    @Transaction
    @Query("SELECT * FROM Playlist WHERE trim(name) COLLATE NOCASE = trim(:name) COLLATE NOCASE")
    fun playlistWithSongs(name: String): Flow<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE browseId = :browseId")
    fun playlistWithSongsByBrowseId(browseId: String): Flow<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE name LIKE '${MONTHLY_PREFIX}' || :name || '%'  ")
    fun monthlyPlaylists(name: String?): Flow<List<PlaylistWithSongs>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist WHERE name LIKE '${MONTHLY_PREFIX}' || :name || '%' ORDER BY ROWID DESC ")
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun monthlyPlaylistsPreview(name: String?): Flow<List<PlaylistPreview>>

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query(
        """
        SELECT * FROM SortedSongPlaylistMap SPLM
        INNER JOIN Song on Song.id = SPLM.songId
        WHERE playlistId = :id
        ORDER BY SPLM.position
        """
    )
    fun playlistSongs(id: Long): Flow<List<Song>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query(
        """
        SELECT * FROM SortedSongPlaylistMap SPLM
        INNER JOIN Song on Song.id = SPLM.songId
        WHERE playlistId = :id
        ORDER BY SPLM.position
        """
    )
    fun songsInPlaylist(id: Long): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN SongAlbumMap SM ON S.id=SM.songId " +
            "INNER JOIN Album A ON A.id=SM.albumId WHERE A.bookmarkedAt IS NOT NULL")
    fun songsInAllBookmarkedAlbums(): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN SongArtistMap SM ON S.id=SM.songId " +
            "INNER JOIN Artist A ON A.id=SM.artistId WHERE A.bookmarkedAt IS NOT NULL")
    fun songsInAllFollowedArtists(): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN SongArtistMap SM ON S.id=SM.songId " +
            "INNER JOIN Artist A ON A.id=SM.artistId WHERE A.bookmarkedAt IS NOT NULL AND A.id IN (:artists)")
    fun songsInAllFollowedArtistsFiltered(artists: List<String>): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId")
    fun songsInAllPlaylists(): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.isYoutubePlaylist = 1")
    fun songsInAllYTPrivatePlaylists(): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.name LIKE '${PIPED_PREFIX}' || '%'")
    fun songsInAllPipedPlaylists(): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.isPodcast = 1")
    fun songsInAllPodcastPlaylists(): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.name LIKE '${PINNED_PREFIX}' || '%'")
    fun songsInAllPinnedPlaylists(): Flow<List<Song>>

    @Transaction
    @Query("SELECT DISTINCT S.* FROM Song S INNER JOIN songplaylistmap SM ON S.id=SM.songId " +
            "INNER JOIN Playlist P ON P.id=SM.playlistId WHERE P.name LIKE '${MONTHLY_PREFIX}' || '%'")
    fun songsInAllMonthlyPlaylists(): Flow<List<Song>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY S.artistsText COLLATE NOCASE
    """)
    fun sortSongsFromPlaylistByArtist( id: Long ): Flow<List<SongEntity>>

    /**
     * Fetch all records from data that have playlist id matches [id]
     * and sort them by their titles.
     *
     * [EXPLICIT_PREFIX] is removed during the sort process to make
     * this sorting more accurate
     */
    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY 
            CASE
                WHEN S.title LIKE "${EXPLICIT_PREFIX}%" THEN SUBSTR(S.title, LENGTH('${EXPLICIT_PREFIX}') + 1)
                ELSE S.title
            END
        COLLATE NOCASE
    """)
    fun sortSongsFromPlaylistByTitle( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY SP.position
    """)
    fun sortSongsPlaylistByPosition( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY SP.position
    """)
    fun sortSongsPlaylistByPositionNoFlow( id: Long ): List<SongEntity>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY S.totalPlayTimeMs
    """)
    fun sortSongsFromPlaylistByPlaytime( id: Long ): Flow<List<SongEntity>>

    fun sortSongsFromPlaylistByRelativePlaytime( id: Long ): Flow<List<SongEntity>> {
        val songs = sortSongsFromPlaylistByPlaytime(id)
        songs.map { it }
        return songs.map {
            it.sortedBy { se ->
                se.relativePlayTime()
            }
        }
    }

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN Event E ON E.songId=S.id 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY E.timestamp
    """)
    fun sortSongsFromPlaylistByDatePlayed( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, A.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN songalbummap SA ON SA.songId=SP.songId 
        LEFT JOIN Album A ON A.Id=SA.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY CAST(A.year AS INTEGER)
    """)
    fun sortSongsFromPlaylistByYear( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY S.durationText
    """)
    fun sortSongsFromPlaylistByDuration( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, A.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN songalbummap SA ON SA.songId=SP.songId 
        LEFT JOIN Album A ON A.Id=SA.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY S.artistsText COLLATE NOCASE, A.title COLLATE NOCASE
    """)
    fun sortSongsFromPlaylistByArtistAndAlbum( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, A.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN songalbummap SA ON SA.songId=SP.songId 
        LEFT JOIN Album A ON A.Id=SA.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY A.title COLLATE NOCASE
    """)
    fun sortSongsFromPlaylistByAlbum( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY S.ROWID
    """)
    fun sortSongsFromPlaylistByRowId( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN SongPlaylistMap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY SP.dateAdded
    """)
    fun sortSongsFromPlaylistByDateAdded( id: Long ): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT S.*, Album.title as albumTitle, Format.contentLength as contentLength
        FROM Song S 
        INNER JOIN songplaylistmap SP ON S.id = SP.songId 
        LEFT JOIN SongAlbumMap ON SongAlbumMap.songId = S.id 
        LEFT JOIN Album ON Album.id = SongAlbumMap.albumId 
        LEFT JOIN Format ON Format.songId = S.id
        WHERE SP.playlistId = :id 
        ORDER BY S.LikedAt COLLATE NOCASE
    """)
    fun sortSongsFromPlaylistByLikedAt( id: Long ): Flow<List<SongEntity>>

    fun songsPlaylist(id: Long, sortBy: PlaylistSongSortBy, sortOrder: SortOrder): Flow<List<SongEntity>> =
        when( sortBy ) {
            PlaylistSongSortBy.Album -> sortSongsFromPlaylistByAlbum( id )
            PlaylistSongSortBy.AlbumYear -> sortSongsFromPlaylistByYear( id )
            PlaylistSongSortBy.Artist -> sortSongsFromPlaylistByArtist( id )
            PlaylistSongSortBy.ArtistAndAlbum -> sortSongsFromPlaylistByArtistAndAlbum( id )
            PlaylistSongSortBy.DatePlayed -> sortSongsFromPlaylistByDatePlayed( id )
            PlaylistSongSortBy.PlayTime -> sortSongsFromPlaylistByPlaytime( id )
            PlaylistSongSortBy.RelativePlayTime -> sortSongsFromPlaylistByRelativePlaytime( id )
            PlaylistSongSortBy.Position -> sortSongsPlaylistByPosition( id )
            PlaylistSongSortBy.Title -> sortSongsFromPlaylistByTitle( id )
            PlaylistSongSortBy.Duration -> sortSongsFromPlaylistByDuration( id )
            PlaylistSongSortBy.DateLiked -> sortSongsFromPlaylistByLikedAt( id )
            PlaylistSongSortBy.DateAdded -> sortSongsFromPlaylistByDateAdded( id )
        }.map {
            it.run {
                if( sortOrder == SortOrder.Descending )
                    reversed()
                else
                    this
            }
        }

    @Transaction
    @Query("SELECT S.* FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId WHERE SP.playlistId=:id ORDER BY SP.position LIMIT 4")
    fun songsPlaylistTop4Positions(id: Long): Flow<List<Song>>

    @Transaction
    @Query("SELECT SP.position FROM Song S INNER JOIN songplaylistmap SP ON S.id=SP.songId WHERE SP.playlistId=:id AND S.id NOT LIKE '$LOCAL_KEY_PREFIX%' ORDER BY SP.position")
    fun songsPlaylistMap(id: Long): Flow<List<Int>>

    @Transaction
    @Query("SELECT id FROM SONG WHERE likedAt IS NOT NULL AND likedAt < 0")
    fun dislikedSongsById(): Flow<List<String>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT * FROM Playlist WHERE browseId = :browseId")
    fun playlist(browseId: String): Flow<Playlist?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist WHERE id=:id")
    fun singlePlaylistPreview(id: Long): Flow<PlaylistPreview?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist WHERE name LIKE '${PINNED_PREFIX}%' ORDER BY name COLLATE NOCASE ASC")
    fun playlistPinnedPreviewsByNameAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist ORDER BY name COLLATE NOCASE ASC")
    fun playlistPreviewsByNameAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist ORDER BY ROWID ASC")
    fun playlistPreviewsByDateAddedAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist ORDER BY songCount ASC")
    fun playlistPreviewsByDateSongCountAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist ORDER BY name COLLATE NOCASE DESC")
    fun playlistPreviewsByNameDesc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist ORDER BY ROWID DESC")
    fun playlistPreviewsByDateAddedDesc(): Flow<List<PlaylistPreview>>
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, 0 as isOnDevice FROM Playlist ORDER BY songCount DESC")
    fun playlistPreviewsByDateSongCountDesc(): Flow<List<PlaylistPreview>>
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, " +
            "(SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, " +
            "(SELECT SUM(Song.totalPlayTimeMs) FROM Song " +
            "JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE SongPlaylistMap.playlistId = Playlist.id ) as TotPlayTime, 0 as isOnDevice " +
            "FROM Playlist " +
            "ORDER BY 6")
    fun playlistPreviewsByMostPlayedSongsAsc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("SELECT *, " +
            "(SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount, " +
            "(SELECT SUM(Song.totalPlayTimeMs) FROM Song " +
            "JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId " +
            "WHERE SongPlaylistMap.playlistId = Playlist.id ) as TotPlayTime, 0 as isOnDevice " +
            "FROM Playlist " +
            "ORDER BY 6 DESC")
    fun playlistPreviewsByMostPlayedSongsDesc(): Flow<List<PlaylistPreview>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    fun playlistPreviews(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ): Flow<List<PlaylistPreview>> {
        return when (sortBy) {
            PlaylistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByNameAsc()
                SortOrder.Descending -> playlistPreviewsByNameDesc()
            }
            PlaylistSortBy.SongCount -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByDateSongCountAsc()
                SortOrder.Descending -> playlistPreviewsByDateSongCountDesc()
            }
            PlaylistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByDateAddedAsc()
                SortOrder.Descending -> playlistPreviewsByDateAddedDesc()
            }
            PlaylistSortBy.MostPlayed -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByMostPlayedSongsAsc()
                SortOrder.Descending -> playlistPreviewsByMostPlayedSongsDesc()
            }
        }
    }

    @Query("SELECT thumbnailUrl FROM Song JOIN SongPlaylistMap ON id = songId WHERE playlistId = :id AND thumbnailUrl <>'' ORDER BY position LIMIT 4")
    fun playlistThumbnailUrls(id: Long): Flow<List<String>>


    @Query("SELECT * FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId WHERE SongArtistMap.artistId = :artistId ORDER BY Song.totalPlayTimeMs DESC LIMIT :count")
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun artistTopSongs(artistId: String, count: Int = 10): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId WHERE SongArtistMap.artistId = :artistId AND totalPlayTimeMs > 0 ORDER BY Song.ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun artistSongs(artistId: String): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId WHERE SongArtistMap.artistId = :artistId ORDER BY Song.ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun artistAllSongs(artistId: String): Flow<List<Song>>

    @Query("SELECT * FROM SongArtistMap WHERE artistId = :artistId")
    fun artistSongMap(artistId: String): Flow<List<SongArtistMap>>

    @Query("SELECT * FROM Format WHERE songId = :songId")
    fun format(songId: String): Flow<Format?>

    @Query("SELECT contentLength FROM Format WHERE songId = :songId")
    fun formatContentLength(songId: String): Long

    @Transaction
    @Query("UPDATE Format SET contentLength = 0 WHERE songId = :songId")
    fun resetFormatContentLength(songId: String)

    @Transaction
    @Query("DELETE FROM Song WHERE totalPlayTimeMs = 0")
    fun deleteHiddenSongs()
    // USEFUL FOR MAINTENANCE
    @Transaction
    @Query("DELETE FROM SongArtistMap WHERE songId NOT IN (SELECT DISTINCT id FROM Song)")
    fun cleanSongArtistMap()
    @Transaction
    @Query("DELETE FROM SongPlaylistMap WHERE songId NOT IN (SELECT DISTINCT id FROM Song)")
    fun cleanSongPlaylistMap()
    @Transaction
    @Query("DELETE FROM SongAlbumMap WHERE songId NOT IN (SELECT DISTINCT id FROM Song)")
    fun cleanSongAlbumMap()
    @Transaction
    @Query("DELETE FROM Format WHERE songId = :songId")
    fun deleteFormat(songId: String)
    @Transaction
    @Query("DELETE FROM SongAlbumMap WHERE albumId = :albumId")
    fun deleteAlbumMap(albumId: String)
    @Transaction
    @Query("DELETE FROM SongArtistMap WHERE artistId = :artistId")
    fun deleteArtistMap(artistId: String)

    @Transaction
    @Query("SELECT Song.*, contentLength FROM Song JOIN Format ON id = songId WHERE contentLength IS NOT NULL AND totalPlayTimeMs > 0 ORDER BY Song.ROWID DESC")
    fun songsWithContentLength(): Flow<List<SongWithContentLength>>

    @Transaction
    @Query("""
        UPDATE SongPlaylistMap SET position = 
          CASE 
            WHEN position < :fromPosition THEN position + 1
            WHEN position > :fromPosition THEN position - 1
            ELSE :toPosition
          END 
        WHERE playlistId = :playlistId AND position BETWEEN MIN(:fromPosition,:toPosition) and MAX(:fromPosition,:toPosition)
    """)
    fun move(playlistId: Long, fromPosition: Int, toPosition: Int)

    @Transaction
    @Query("UPDATE SongPlaylistMap SET position = :toPosition WHERE playlistId = :playlistId and songId = :songId")
    fun updateSongPosition(playlistId: Long, songId: String, toPosition: Int)

    @Query("DELETE FROM SongPlaylistMap WHERE playlistId = :id")
    fun clearPlaylist(id: Long)

    @Query("DELETE FROM SongPlaylistMap WHERE songId = :id")
    fun deleteSongFromPlaylists(id: String)

    @Query("DELETE FROM SongPlaylistMap WHERE songId = :id and playlistId = :playlistId")
    fun deleteSongFromPlaylist(id: String, playlistId: Long)

    @Query("SELECT setVideoId FROM SongPlaylistMap WHERE songId = :id and playlistId = :playlistId")
    fun getSetVideoIdFromPlaylist(id: String, playlistId: Long): Flow<String?>

    @Query("DELETE FROM SongAlbumMap WHERE albumId = :id")
    fun clearAlbum(id: String)

    @Query("SELECT loudnessDb FROM Format WHERE songId = :songId")
    fun loudnessDb(songId: String): Flow<Float?>

    @Query("SELECT * FROM Song WHERE title LIKE :query OR artistsText LIKE :query")
    fun search(query: String): Flow<List<Song>>

    @Query("SELECT albumId AS id, Album.title AS name, 0 AS size FROM SongAlbumMap LEFT JOIN Album ON id=albumId WHERE songId = :songId")
    fun songAlbumInfo(songId: String): Info?

    @Query("SELECT thumbnailUrl FROM Song LEFT JOIN SongAlbumMap ON id=songId WHERE SongAlbumMap.albumId = :albumId")
    fun albumThumbnailFromSong(albumId: String): String?

    @Query("SELECT id, name, 0 AS size FROM Artist LEFT JOIN SongArtistMap ON id = SongArtistMap.artistId WHERE songId = :songId")
    fun songArtistInfo(songId: String): List<Info>

    /*
        @Transaction
        @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY SUM(CAST(playTime AS REAL) / (((:now - timestamp) / 86400000) + 1)) DESC LIMIT 1")
    //    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY timestamp DESC LIMIT 1")
        @RewriteQueriesToDropUnusedColumns
        fun trending(now: Long = System.currentTimeMillis()): Flow<Song?>
    //    fun trending(): Flow<Song?>
     */

    @Transaction
    //@Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(CAST(playTime AS REAL) / (((:now - timestamp) / 86400000) + 1)) DESC LIMIT 1")
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY SUM(CAST(playTime AS REAL) / (((:now - timestamp) / 86400000) + 1)) DESC LIMIT 1")
    @RewriteQueriesToDropUnusedColumns
    fun trendingReal(now: Long = System.currentTimeMillis()): Flow<List<Song>>

    @Transaction
    //@Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trending(limit: Int = 3): Flow<List<Song>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    //@Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trendingSongEntity(limit: Int = 3): Flow<List<SongEntity>>

    @Transaction
    //@Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE (:now - Event.timestamp) <= :period AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE (:now - Event.timestamp) <= :period GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trending(
        limit: Int = 3,
        now: Long = System.currentTimeMillis(),
        period: Long
    ): Flow<List<Song>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    //@Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE (:now - Event.timestamp) <= :period AND Song.id NOT LIKE '$LOCAL_KEY_PREFIX%' GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId WHERE (:now - Event.timestamp) <= :period GROUP BY songId ORDER BY SUM(playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun trendingSongEntity(
        limit: Int = 3,
        now: Long = System.currentTimeMillis(),
        period: Long
    ): Flow<List<SongEntity>>


    //*****REWIND********
    @Transaction
    @Query("SELECT Song.*, ((SUM(Event.playTime) / 60) / 1000) as minutes FROM Event INNER JOIN Song ON Song.id = songId WHERE " +
            "CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY songId  ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun songMostListenedByYear(year: Int, limit: Int = 1): Flow<List<SongMostListened>>

    @Transaction
    @Query("SELECT Album.*, ((SUM(Event.playTime) / 60) / 1000) as minutes FROM Album " +
            "INNER JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId " +
            "INNER JOIN Song ON Song.id = SongAlbumMap.songId " +
            "INNER JOIN Event ON Event.songId = Song.id " +
            "WHERE CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY Album.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun albumMostListenedByYear(year: Int, limit: Int = 1): Flow<List<AlbumMostListened>>

    @Transaction
    @Query("SELECT Playlist.*, ((SUM(Event.playTime) / 60) / 1000) as minutes, COUNT(DISTINCT SongPlaylistMap.songId) AS songs FROM Playlist " +
            "INNER JOIN SongPlaylistMap ON Playlist.id = SongPlaylistMap.playlistId " +
            "INNER JOIN Song ON Song.id = SongPlaylistMap.songId " +
            "INNER JOIN Event ON Event.songId = Song.id " +
            "WHERE CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY Playlist.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun playlistMostListenedByYear(year: Int, limit: Int = 1): Flow<List<PlaylistMostListened>>

    @Transaction
    @Query("SELECT Artist.*, ((SUM(Event.playTime) / 60) / 1000) as minutes FROM Artist " +
            "INNER JOIN Song ON Artist.name LIKE '%' || Song.artistsText || '%'  " +
            "INNER JOIN Event ON Event.songId = Song.id " +
            "WHERE CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year " +
            "GROUP BY Artist.id ORDER BY SUM(Event.playTime) DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun artistMostListenedByYear(year: Int, limit: Int = 1): Flow<List<ArtistMostListened>>

    @Transaction
    @Query("SELECT COUNT(DISTINCT Song.id) AS songs, ((SUM(Event.playTime) / 60) / 1000) as minutes " +
            "FROM Event INNER JOIN Song ON Song.id = Event.songId " +
            "WHERE CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year ")
    @RewriteQueriesToDropUnusedColumns
    fun songsListenedCountByYear(year: Int): Flow<SongsListenedCount>

    @Transaction
    @Query("SELECT COUNT(DISTINCT Album.id) as albums, ((SUM(Event.playTime) / 60) / 1000) as minutes FROM Album " +
            "INNER JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId " +
            "INNER JOIN Song ON Song.id = SongAlbumMap.songId " +
            "INNER JOIN Event ON Event.songId = Song.id " +
            "WHERE CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year ")
    @RewriteQueriesToDropUnusedColumns
    fun albumsListenedCountByYear(year: Int): Flow<AlbumsListenedCount?>

    @Transaction
    @Query("SELECT COUNT(DISTINCT Artist.id) as artists, ((SUM(Event.playTime) / 60) / 1000) as minutes FROM Artist " +
            "INNER JOIN Song ON Artist.name LIKE '%' || Song.artistsText || '%'  " +
            "INNER JOIN Event ON Event.songId = Song.id " +
            "WHERE CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year ")
    @RewriteQueriesToDropUnusedColumns
    fun artistsListenedCountByYear(year: Int): Flow<ArtistsListenedCount?>

    @Transaction
    @Query("SELECT COUNT(DISTINCT Playlist.id) as playlists, ((SUM(Event.playTime) / 60) / 1000) as minutes FROM Playlist " +
            "INNER JOIN SongPlaylistMap ON Playlist.id = SongPlaylistMap.playlistId " +
            "INNER JOIN Song ON Song.id = SongPlaylistMap.songId " +
            "INNER JOIN Event ON Event.songId = Song.id " +
            "WHERE CAST(strftime('%Y',Event.timestamp / 1000,'unixepoch') as INTEGER) = :year ")
    @RewriteQueriesToDropUnusedColumns
    fun playlistsListenedCountByYear(year: Int): Flow<PlaylistsListenedCount?>

    @Transaction
    @Query("SELECT DISTINCT CAST(strftime('%Y',timestamp / 1000,'unixepoch') as INTEGER) as year FROM Event " +
            "WHERE playTime IS NOT NULL ORDER BY timestamp DESC LIMIT :limit")
    fun rewindYears(limit: Int = 5): Flow<List<Int>>


    //*************


    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun lastPlayed( limit: Int = 10 ): Flow<List<Song>>

    @Query("SELECT COUNT (*) FROM Event")
    fun eventsCount(): Flow<Int>

    @Query("DELETE FROM Event")
    fun clearEvents()

    @Query("DELETE FROM Event WHERE songId = :songId")
    fun clearEventsFor(songId: String)


    @Query("SELECT * FROM Queues ORDER BY position")
    fun queues(): Flow<List<Queues>>

    @Query("SELECT * FROM ExternalApp ORDER BY appName")
    fun externalApps(): Flow<List<ExternalApp>>

    @Query("UPDATE Queues SET isSelected = 0")
    fun clearSelectedQueue()

    @Query("DELETE FROM Queues")
    fun clearQueues()

    fun toggleSelectQueue(queue: Queues) {
        clearSelectedQueue()
        val isSelected = queue.isSelected ?: false
        update(queue.copy(isSelected = !isSelected))
    }

    @Query("DELETE FROM Queues WHERE id = :id")
    fun deleteQueue(id: Long)

    @Query("SELECT * FROM Queues WHERE isSelected = 1 LIMIT 1")
    fun selectedQueue(): Queues?

    @Query("SELECT * FROM Queues WHERE isSelected = 1 LIMIT 1")
    fun selectedQueueFlow(): Flow<Queues?>

    @Query("SELECT * FROM Queues WHERE id = :id")
    fun getQueue(id: Long): Queues?

    // MUSICVAULT
    @Query("UPDATE Song SET musicVaultState = :state WHERE id = :id")
    fun updateMusicVaultState(id: String, state: MusicVaultState)

    @Query("UPDATE Song SET musicVaultState = :state, musicVaultFileName = :fileName WHERE id = :id")
    fun updateMusicVaultState(id: String, state: MusicVaultState, fileName: String)

    @Query("""
    UPDATE Song 
    SET musicVaultState = :state, 
        musicVaultFileName = :fileName,
        musicVaultThumbnailFileName = :thumbnailFileName
    WHERE id = :id
""")
    suspend fun updateMusicVaultCompleted(
        id: String,
        state: MusicVaultState = MusicVaultState.COMPLETED,
        fileName: String,
        thumbnailFileName: String
    )

    @Query("SELECT * FROM Song WHERE musicVaultState = 'COMPLETED'")
    fun musicVaultSongs(): Flow<List<Song>>

    // ArtistDiscography
    @Query("SELECT * FROM ArtistDiscography WHERE id = :artistId")
    fun getArtistDiscography(artistId: String): Flow<ArtistDiscography?>


    // ********** Recommendation Strategy ******************
    @Query("SELECT * FROM user_artist_affinity WHERE userId = :userId ORDER BY score DESC LIMIT :limit")
    suspend fun getTopArtists(userId: String, limit: Int): List<UserArtistAffinity>

    @Query("SELECT artistId FROM user_artist_affinity WHERE userId = :userId AND score >= :minScore")
    suspend fun getTopArtistIds(userId: String, minScore: Float = 0.1f): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserArtistAffinity(items: List<UserArtistAffinity>)

    @Query("DELETE FROM user_artist_affinity WHERE userId = :userId")
    suspend fun deleteArtistForUser(userId: String)

    @Query("SELECT * FROM user_keyword_affinity WHERE userId = :userId ORDER BY weight DESC LIMIT :limit")
    suspend fun getTopKeywords(userId: String, limit: Int): List<UserKeywordAffinity>

    @Query("SELECT keyword, weight FROM user_keyword_affinity WHERE userId = :userId")
    suspend fun getKeywordVector(userId: String): List<KeywordWeight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserKeywordAffinity(items: List<UserKeywordAffinity>)

    @Query("DELETE FROM user_keyword_affinity WHERE userId = :userId")
    suspend fun deleteKeysForUser(userId: String)

    @Query("SELECT * FROM user_era_affinity WHERE userId = :userId ORDER BY weight DESC")
    suspend fun getAll(userId: String): List<UserEraAffinity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserEraAffinity(items: List<UserEraAffinity>)

    @Query("DELETE FROM user_era_affinity WHERE userId = :userId")
    suspend fun deleteEraAffinityAllForUser(userId: String)

    @Query("""
        SELECT r.* FROM recommendation r
        INNER JOIN song s ON s.id = r.songId
        WHERE r.userId = :userId
          AND r.strategyId = :strategyId
          AND r.rejectedAt IS NULL
          AND (r.consumed = 0 OR r.consumedAt IS NULL OR r.consumedAt < :consumedBefore)
        ORDER BY r.score DESC
        LIMIT :limit
    """)
    suspend fun getActiveByStrategy(
        userId: String,
        strategyId: String,
        consumedBefore: Long,
        limit: Int
    ): List<Recommendation>

    @Query("DELETE FROM recommendation WHERE userId = :userId AND strategyId = :strategyId")
    suspend fun deleteByStrategy(userId: String, strategyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecommendation(items: List<Recommendation>)

    @Query("UPDATE recommendation SET consumed = 1, consumedAt = :now WHERE userId = :userId AND songId = :songId AND strategyId = :strategyId")
    suspend fun markConsumed(userId: String, songId: String, strategyId: String, now: Long)

    @Query("UPDATE recommendation SET rejectedAt = :now WHERE userId = :userId AND songId = :songId")
    suspend fun markRejected(userId: String, songId: String, now: Long)

    @Query("""
        SELECT * FROM artist_relation
        WHERE fromArtistId = :artistId OR toArtistId = :artistId
    """)
    suspend fun getBidirectional(artistId: String): List<ArtistRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistRelation(items: List<ArtistRelation>)

    @Query("""
    SELECT * FROM song
    WHERE likedAt IS NOT NULL
      AND likedAt > 0
      AND isPodcast = 0
      AND totalPlayTimeMs > 0
    ORDER BY totalPlayTimeMs DESC
    LIMIT :limit
""")
    suspend fun getLikedSongs(limit: Int): List<Song>

    @Query("""
    SELECT * FROM song
    WHERE likedAt IS NOT NULL
      AND likedAt > 0
      AND isPodcast = 0
      AND id IN (
        SELECT songId FROM event
        WHERE timestamp < :olderThan
        GROUP BY songId
        ORDER BY MAX(timestamp) DESC
      )
    LIMIT :limit
""")
    suspend fun getLikedSongsNotPlayedSince(olderThan: Long, limit: Int): List<Song>

    @Query("""
    SELECT MAX(timestamp) FROM event WHERE songId = :songId
""")
    suspend fun getLastPlayedAt(songId: String): Long?

    @Query("""
    SELECT * FROM album
    WHERE rating >= :minRating
      AND ratingVotes >= :minVotes
      AND originalYear IS NOT NULL
    ORDER BY rating DESC, ratingVotes DESC
    LIMIT :limit
""")
    suspend fun getTopRatedAlbums(minRating: Float, minVotes: Int, limit: Int): List<Album>

    @Query("SELECT * FROM event WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getEventsSince(since: Long): List<Event>

    @Query("SELECT * FROM event ORDER BY timestamp ASC")
    suspend fun getAllEvents(): List<Event>


    @Query("SELECT COUNT(*) FROM artist")
    suspend fun countArtists(): Int?

    @Query("""
        SELECT COUNT(*) FROM artist
        WHERE genres LIKE '%' || :keyword || '%'
           OR tags LIKE '%' || :keyword || '%'
    """)
    suspend fun countArtistsByKeyword(keyword: String): Int?


    @Query("SELECT * FROM song WHERE id = :songId")
    suspend fun getById(songId: String): Song?

    // Per gating UI
    @Query("SELECT COUNT(*) FROM song WHERE totalPlayTimeMs > 0 AND isPodcast = 0")
    suspend fun countPlayedSongs(): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM event")
    suspend fun countDistinctPlayedSongs(): Int

    // SongDao.kt
    @Query("SELECT COUNT(*) FROM song")
    suspend fun countSongsTotal(): Int

    @Query("SELECT COUNT(*) FROM song WHERE genres IS NOT NULL AND genres != '[]' AND genres != ''")
    suspend fun countSongsWithGenres(): Int

    @Query("SELECT COUNT(*) FROM song WHERE likedAt IS NOT NULL AND likedAt > 0")
    suspend fun countSongsLiked(): Int

    @Query("""
    SELECT COUNT(DISTINCT e.songId) 
    FROM event e 
    LEFT JOIN song s ON s.id = e.songId 
    WHERE s.id IS NULL
""")
    suspend fun countOrphanEvents(): Int

    @Query("""
    SELECT * FROM song
    WHERE isPodcast = 0
      AND (genres IS NULL OR genres = '[]' OR genres = '')
    ORDER BY totalPlayTimeMs DESC
    LIMIT :limit
""")
    suspend fun getTopSongsWithoutGenres(limit: Int): List<Song>

    @Query("""
    SELECT s.* FROM song s
    INNER JOIN (
        SELECT songId, MAX(timestamp) as lastPlayed
        FROM event
        WHERE timestamp < :olderThan
        GROUP BY songId
    ) e ON e.songId = s.id
    WHERE s.isPodcast = 0
      AND s.totalPlayTimeMs > :minTotalPlayMs
    ORDER BY e.lastPlayed ASC
    LIMIT :limit
""")
    suspend fun getForgottenSongs(
        olderThan: Long,
        minTotalPlayMs: Long = 60_000L,  // almeno 1 minuto ascoltato cumulativo
        limit: Int
    ): List<Song>

    @Query("SELECT COUNT(*) FROM event WHERE playTime > 0")
    suspend fun countEventsWithPlayTime(): Int

    @Query("SELECT * FROM song WHERE genres IS NOT NULL AND genres != '[]' AND genres != '' LIMIT 1")
    suspend fun getFirstWithGenres(): Song?

     @Query("SELECT COUNT(*) FROM album")
     suspend fun countAlbumsTotal(): Int

     @Query("SELECT COUNT(*) FROM album WHERE rating IS NOT NULL")
     suspend fun countAlbumsWithRating(): Int

     @Query("SELECT COUNT(*) FROM album WHERE ratingVotes IS NOT NULL AND ratingVotes > 0")
     suspend fun countAlbumsWithVotes(): Int

     @Query("SELECT * FROM album LIMIT :limit")
     suspend fun getAlbums(limit: Int): List<Album>

    // In SongDao.kt

    @Query("""
    SELECT * FROM song 
    WHERE albumId = :albumId 
      AND isPodcast = 0
    ORDER BY id
    LIMIT :limit
""")
    suspend fun getSongsByAlbum(albumId: String, limit: Int): List<Song>

    @Query("""
    SELECT * FROM song 
    WHERE albumId = :albumId 
      AND isPodcast = 0
      AND totalPlayTimeMs = 0
    ORDER BY id
    LIMIT :limit
""")
    suspend fun getUnplayedSongsByAlbum(albumId: String, limit: Int): List<Song>

    @Query("""
    SELECT * FROM song
    WHERE albumId IS NULL 
      AND isPodcast = 0
      AND artistsText IS NOT NULL
      AND totalPlayTimeMs > 0
    ORDER BY totalPlayTimeMs DESC
    LIMIT :limit
""")
    suspend fun getTopSongsWithoutAlbum(limit: Int): List<Song>

    // SongDao.kt
    @Query("""
    SELECT * FROM song 
    WHERE artistsText IS NOT NULL 
      AND artistsText != ''
      AND isPodcast = 0
      AND id NOT IN (SELECT DISTINCT songId FROM song_artist_cross_ref)
    ORDER BY totalPlayTimeMs DESC
    LIMIT :limit
""")
    suspend fun getSongsWithArtistsText(limit: Int): List<Song>

    // In AlbumDao
    @Query("""
        SELECT COUNT(*) FROM album 
        WHERE rating IS NOT NULL 
           OR genres IS NOT NULL 
           OR originalYear IS NOT NULL
    """)
    suspend fun countAlbumsEnriched(): Int

    @Query("""
        SELECT * FROM album 
        WHERE rating IS NOT NULL 
           OR genres IS NOT NULL 
           OR originalYear IS NOT NULL
        LIMIT 5
    """)
    suspend fun getAlbumsEnrichedSample(): List<Album>

    // AlbumDao
    @Query("""
    SELECT * FROM album 
    WHERE (rating IS NULL OR genres IS NULL OR originalYear IS NULL)
      AND title IS NOT NULL 
      AND authorsText IS NOT NULL
      AND title NOT IN ('Audio', 'WhatsApp Audio', 'Unknown Album')
      AND title NOT LIKE 'Audio %'
    ORDER BY timestamp DESC
    LIMIT :limit
""")
    suspend fun getAlbumsToEnrich(limit: Int): List<Album>

    // In AlbumDao.kt
    @Query("""
    SELECT * FROM album 
    WHERE bookmarkedAt IS NOT NULL
    ORDER BY bookmarkedAt DESC
    LIMIT :limit
""")
    suspend fun getBookmarkedAlbums(limit: Int): List<Album>

    @Query("SELECT COUNT(*) FROM album WHERE bookmarkedAt IS NOT NULL")
    suspend fun countBookmarked(): Int

    //************* INIZIO MBALBUM *****************
    @Query("""
        SELECT * FROM mb_album
        WHERE
            (rating IS NOT NULL AND rating >= :minRating AND ratingVotes >= :minVotes)
            OR (tags IS NOT NULL AND (length(tags) - length(replace(tags, ',', '')) >= 4))
        ORDER BY popularityScore DESC, rating DESC, ratingVotes DESC
        LIMIT :limit
    """)
    suspend fun getQualityAlbums(
        minRating: Float = 3.5f,
        minVotes: Int = 1,
        limit: Int
    ): List<MBAlbum>

    @Query("""
        SELECT * FROM mb_album
        WHERE 
            -- Album con metadati MB popolati (anche senza rating)
            (genres IS NOT NULL AND genres != '[]')
            OR (tags IS NOT NULL AND tags != '[]')
            OR (rating IS NOT NULL)
        ORDER BY popularityScore DESC, originalYear DESC
        LIMIT :limit
    """)
    suspend fun getAlbumsWithMetadata(limit: Int): List<MBAlbum>

    @Query("SELECT * FROM mb_album WHERE id = :mbid")
    suspend fun getMBAlbumById(mbid: String): MBAlbum?

    @Query("""
        SELECT * FROM mb_album
        WHERE artistCredit LIKE '%' || :artistName || '%'
        ORDER BY originalYear ASC
    """)
    suspend fun findByArtistName(artistName: String): List<MBAlbum>

    @Query("SELECT * FROM mb_album WHERE matchedAlbumId IS NULL LIMIT :limit")
    suspend fun getUnmatched(limit: Int): List<MBAlbum>

    @Query("UPDATE mb_album SET matchedAlbumId = :albumId, matchScore = :score, matchedAt = :now WHERE id = :mbid")
    suspend fun setMatch(mbid: String, albumId: String?, score: Float, now: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MBAlbum)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MBAlbum>)

    @Query("SELECT COUNT(*) FROM mb_album")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM mb_album WHERE rating IS NOT NULL AND ratingVotes >= 1")
    suspend fun countWithRating(): Int

    @Query("SELECT COUNT(*) FROM mb_album WHERE matchedAlbumId IS NOT NULL AND matchedAlbumId != ''")
    suspend fun countMatched(): Int

     @Query("SELECT * FROM mb_album ORDER BY fetchedAt DESC LIMIT :limit")
     suspend fun getRecent(limit: Int): List<MBAlbum>

    @Query("""
        SELECT * FROM mb_album
        WHERE 
            -- Album con metadati minimi
            (genres IS NOT NULL AND genres != '[]' AND genres != '')
            OR (tags IS NOT NULL AND tags != '[]' AND tags != '')
            OR (rating IS NOT NULL)
        ORDER BY 
            popularityScore DESC,
            CASE WHEN rating IS NOT NULL THEN rating ELSE 2.5 END DESC,
            ratingVotes DESC
        LIMIT :limit
    """)
    suspend fun getQualityAlbumsV2(limit: Int): List<MBAlbum>

    @Query("DELETE FROM mb_album")
    suspend fun deleteMBAlbumsAll(): Int

    //************* FINE MBALBUM *****************

    // Artist dao
    // In ArtistDao.kt
    @Query("SELECT * FROM artist WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): Artist?

    @Query("SELECT * FROM artist WHERE lower(name) LIKE lower(:pattern) LIMIT 5")
    suspend fun findByNameLike(pattern: String): List<Artist>

    @Query("SELECT * FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt DESC LIMIT :limit")
    suspend fun getBookmarkedArtists(limit: Int): List<Artist>

    //************* RECOMENDATION STRATEGY ********************************************


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artistDiscography: ArtistDiscography)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blacklist: Blacklist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Throws(SQLException::class)
    fun insert(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(format: Format)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artist: Artist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(album: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(searchQuery: SearchQuery)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(songPlaylistMap: SongPlaylistMap): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(songArtistMap: SongArtistMap): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(song: Song, format: Format)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(queuedMediaItem: QueuedMediaItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(queuedMediaItems: List<QueuedMediaItem>)


//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    fun insert(songPlaylistMaps: List<SongPlaylistMap>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(album: Album, songAlbumMap: SongAlbumMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(artist: Artist, songArtistMap: SongArtistMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(artists: List<Artist>, songArtistMaps: List<SongArtistMap>)

    @Transaction
    fun insert(mediaItem: MediaItem, block: (Song) -> Song = { it }) {
        var title = mediaItem.mediaMetadata.title.toString()
        if(!title.startsWith(EXPLICIT_PREFIX, true) && mediaItem.isExplicit){
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
            isAudioOnly = if(!isVideo) 1 else 0,
            isPodcast = if (isPodcast) 1 else 0
        ).let(block).also { song ->
            if (insert(song) == -1L) return
        }


        mediaItem.mediaMetadata.extras?.getString("albumId")?.let { albumId ->
            insert(
                Album(id = albumId, title = mediaItem.mediaMetadata.albumTitle?.toString()),
                SongAlbumMap(songId = song.id, albumId = albumId, position = null)
            )
        }

        mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.let { artistNames ->
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artistIds ->
                if (artistNames.size == artistIds.size) {
                    insert(
                        artistNames.mapIndexed { index, artistName ->
                            Artist(id = artistIds[index], name = artistName)
                        },
                        artistIds.map { artistId ->
                            SongArtistMap(songId = song.id, artistId = artistId)
                        }
                    )
                }
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(queues: Queues)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(externalApp: ExternalApp)

    @Update
    fun update(blacklist: Blacklist)

    @Update
    fun update(artist: Artist)

    @Update
    fun update(album: Album)

    @Update
    fun update(playlist: Playlist)

    @Update
    fun update(queues: Queues)

    @Update
    fun update(playlist: Playlist, playlistItem: Environment.PlaylistItem) {
        update(playlist.copy(
            name = playlistItem.title ?: "",
            browseId = playlistItem.key
        ))
    }

    @Upsert
    fun upsert(blacklist: Blacklist)

    @Upsert
    fun upsert(lyrics: Lyrics)

    @Upsert
    @Transaction
    fun upsert(album: Album, songAlbumMaps: List<SongAlbumMap>)

    @Upsert
    fun upsert(song: Song, format: Format)

    @Upsert
    @Transaction
    fun upsert(album: Album, songAlbumMap: SongAlbumMap)

    @Upsert
    @Transaction
    fun upsert(album: Album)

//    @Upsert
//    @Transaction
//    fun upsert(songAlbumMaps: List<SongAlbumMap>)

    @Upsert
    fun upsert(artist: Artist, songArtistMap: SongArtistMap)

    @Upsert
    fun upsert(songAlbumMap: SongAlbumMap)

    @Upsert
    fun upsert(songPlaylistMap: SongPlaylistMap)

    @Upsert
    fun upsert(artist: Artist)

    @Upsert
    fun upsert(songArtistMaps: List<SongArtistMap>)

    @Upsert
    fun upsert(format: Format)

    @Upsert
    fun upsert(song: Song)

    @Upsert
    fun upsert(queue: Queues)

    @Delete
    fun delete(artistDiscography: ArtistDiscography)
    @Delete
    fun delete(blacklist: Blacklist)

    @Delete
    fun delete(searchQuery: SearchQuery)

    @Delete
    fun delete(playlist: Playlist)

    @Delete
    fun delete(songPlaylistMap: SongPlaylistMap)

    @Delete
    fun delete(song: Song)

    @Delete
    fun delete(album: Album)

    @Delete
    fun delete(artist: Artist)

    @Delete
    fun delete(externalApp: ExternalApp)

    /**
     * Reset [Format.contentLength] of provided song.
     *
     * This method is already wrapped by [Transaction] call,
     * therefore, it's unnecessary to wrap it with a coroutine
     * or other transaction call.
     *
     * To use it inside another [Transaction] wrapper, please
     * refer to [resetFormatContentLength].
     *
     * @param songId id of song to have its [Format.contentLength] reset
     */
    @Transaction
    fun resetContentLength( songId: String ) = asyncTransaction {
        resetFormatContentLength( songId )
    }

    /**
     * Commit statements in BULK. If anything goes wrong during the transaction,
     * other statements will be cancelled and reversed to preserve database's integrity.
     * [Read more](https://sqlite.org/lang_transaction.html)
     *
     * [asyncTransaction] runs all statements on non-blocking
     * thread to prevent UI from going unresponsive.
     *
     * ## Best use cases:
     * - Commit multiple write statements that require data integrity
     * - Processes that take longer time to complete
     *
     * > Do NOT use this to retrieve data from the database.
     * > Use [asyncQuery] to retrieve records.
     *
     * @param block of statements to write to database
     */
    fun asyncTransaction( block: Database.() -> Unit ) =
        _internal.transactionExecutor.execute {
            this.block()
        }


    /**
     * Access and retrieve records from database.
     *
     * [asyncQuery] runs all statements asynchronously to
     * prevent blocking UI thread from going unresponsive.
     *
     * ## Best use cases:
     * - Background data retrieval
     * - Non-immediate UI component update (i.e. count number of songs)
     *
     * > Do NOT use this method to write data to database
     * > because it offers no fail-safe during write.
     * > Use [asyncTransaction] to modify database.
     *
     * @param block of statements to retrieve data from database
     */
    fun asyncQuery( block: Database.() -> Unit ) =
        _internal.queryExecutor.execute {
            this.block()
        }

    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        //raw(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
        raw(SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE);"))
    }

    fun path() = _internal.openHelper.writableDatabase.path

    fun openHelper() = _internal.openHelper

    fun close() = _internal.close()
}

@androidx.room.Database(
    entities = [
        Song::class,
        SongPlaylistMap::class,
        Playlist::class,
        Artist::class,
        SongArtistMap::class,
        Album::class,
        SongAlbumMap::class,
        SearchQuery::class,
        QueuedMediaItem::class,
        Format::class,
        Event::class,
        Lyrics::class,
        Queues::class,
        ExternalApp::class,
        Blacklist::class,
        ArtistDiscography::class,
        UserArtistAffinity::class,
        UserKeywordAffinity::class,
        UserEraAffinity::class,
        Recommendation::class,
        ArtistRelation::class,
        MBAlbum::class,
        SongArtistCrossRef::class
    ],
    views = [
        SortedSongPlaylistMap::class
    ],
    version = 60,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = DatabaseInitializer.From3To4Migration::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8, spec = DatabaseInitializer.From7To8Migration::class),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 11, to = 12, spec = DatabaseInitializer.From11To12Migration::class),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21, spec = DatabaseInitializer.From20To21Migration::class),
        AutoMigration(from = 21, to = 22, spec = DatabaseInitializer.From21To22Migration::class),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 30, to = 31),
        AutoMigration(from = 32, to = 33),
        AutoMigration(from = 33, to = 34),
        AutoMigration(from = 34, to = 35),
        AutoMigration(from = 35, to = 36),
        AutoMigration(from = 36, to = 37),
        AutoMigration(from = 37, to = 38),
        AutoMigration(from = 38, to = 39),
        AutoMigration(from = 39, to = 40),
        AutoMigration(from = 40, to = 41),
        AutoMigration(from = 41, to = 42),
        AutoMigration(from = 42, to = 43),
        AutoMigration(from = 44, to = 45),
        AutoMigration(from = 45, to = 46),
        AutoMigration(from = 46, to = 47),
        AutoMigration(from = 47, to = 48),
        AutoMigration(from = 48, to = 49),
        AutoMigration(from = 49, to = 50),
        AutoMigration(from = 50, to = 51),
        AutoMigration(from = 51, to = 52),
        AutoMigration(from = 52, to = 53),
        AutoMigration(from = 53, to = 54),

    ],
)
@TypeConverters(Converters::class)
abstract class DatabaseInitializer protected constructor() : RoomDatabase() {
    abstract val database: Database

    companion object {

        lateinit var Instance: DatabaseInitializer

        fun createProxy(): Database = java.lang.reflect.Proxy.newProxyInstance(
            Database::class.java.classLoader,
            arrayOf(Database::class.java)
        ) { _, method, args ->
            method.invoke(Instance.database, *(args ?: arrayOf()))
        } as Database

        private fun getDatabase() = Room
            .databaseBuilder(appContext(), DatabaseInitializer::class.java, "data.db")
            .addMigrations(
                From8To9Migration(),
                From10To11Migration(),
                From14To15Migration(),
                From22To23Migration(),
                From23To24Migration(),
                From24To25Migration(),
                From25To26Migration(),
                From26To27Migration(),
                From31To32Migration(),
                From38To39Migration(),
                From40To41Migration(),
                From43To44Migration(),
                From45To46Migration(),
                From54To55Migration(),
                From55To56Migration(),
                From56To57Migration(),
                From57To58Migration(),
                From58To59Migration(),
                From59To60Migration(),
            )
            //.fallbackToDestructiveMigration(false)
            .addCallback(object : Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON")  // ← In automatico non viene attivato da room
                }
            })
            .build()


        operator fun invoke() {
            if (!::Instance.isInitialized) reload()
        }

        fun reload() = synchronized(this) {
            Instance = getDatabase()
        }
    }

    // Lista Dao dichiarati
    abstract fun songArtistCrossRefDao(): SongArtistCrossRefDao
    abstract fun songDao(): SongDao
    abstract fun artistDao(): ArtistDao


    // Crud da migrare in dao
    @DeleteTable.Entries(DeleteTable(tableName = "QueuedMediaItem"))
    class From3To4Migration : AutoMigrationSpec

    @RenameColumn.Entries(RenameColumn("Song", "albumInfoId", "albumId"))
    class From7To8Migration : AutoMigrationSpec

    class From8To9Migration : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.query(SimpleSQLiteQuery("SELECT DISTINCT browseId, text, Info.id FROM Info JOIN Song ON Info.id = Song.albumId;"))
                .use { cursor ->
                    val albumValues = ContentValues(2)
                    while (cursor.moveToNext()) {
                        albumValues.put("id", cursor.getString(0))
                        albumValues.put("title", cursor.getString(1))
                        db.insert("Album", CONFLICT_IGNORE, albumValues)

                        db.execSQL(
                            "UPDATE Song SET albumId = '${cursor.getString(0)}' WHERE albumId = ${
                                cursor.getLong(
                                    2
                                )
                            }"
                        )
                    }
                }

            db.query(SimpleSQLiteQuery("SELECT GROUP_CONCAT(text, ''), SongWithAuthors.songId FROM Info JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId GROUP BY songId;"))
                .use { cursor ->
                    val songValues = ContentValues(1)
                    while (cursor.moveToNext()) {
                        songValues.put("artistsText", cursor.getString(0))
                        db.update(
                            "Song",
                            CONFLICT_IGNORE,
                            songValues,
                            "id = ?",
                            arrayOf(cursor.getString(1))
                        )
                    }
                }

            db.query(SimpleSQLiteQuery("SELECT browseId, text, Info.id FROM Info JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId WHERE browseId NOT NULL;"))
                .use { cursor ->
                    val artistValues = ContentValues(2)
                    while (cursor.moveToNext()) {
                        artistValues.put("id", cursor.getString(0))
                        artistValues.put("name", cursor.getString(1))
                        db.insert("Artist", CONFLICT_IGNORE, artistValues)

                        db.execSQL(
                            "UPDATE SongWithAuthors SET authorInfoId = '${cursor.getString(0)}' WHERE authorInfoId = ${
                                cursor.getLong(
                                    2
                                )
                            }"
                        )
                    }
                }

            db.execSQL("INSERT INTO SongArtistMap(songId, artistId) SELECT songId, authorInfoId FROM SongWithAuthors")

            db.execSQL("DROP TABLE Info;")
            db.execSQL("DROP TABLE SongWithAuthors;")
        }
    }

    class From10To11Migration : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.query(SimpleSQLiteQuery("SELECT id, albumId FROM Song;")).use { cursor ->
                val songAlbumMapValues = ContentValues(2)
                while (cursor.moveToNext()) {
                    songAlbumMapValues.put("songId", cursor.getString(0))
                    songAlbumMapValues.put("albumId", cursor.getString(1))
                    db.insert("SongAlbumMap", CONFLICT_IGNORE, songAlbumMapValues)
                }
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, `loudnessDb` REAL, `contentLength` INTEGER, PRIMARY KEY(`id`))")

            db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength FROM Song;")
            db.execSQL("DROP TABLE Song;")
            db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }

    @RenameTable("SongInPlaylist", "SongPlaylistMap")
    @RenameTable("SortedSongInPlaylist", "SortedSongPlaylistMap")
    class From11To12Migration : AutoMigrationSpec

    class From14To15Migration : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.query(SimpleSQLiteQuery("SELECT id, loudnessDb, contentLength FROM Song;"))
                .use { cursor ->
                    val formatValues = ContentValues(3)
                    while (cursor.moveToNext()) {
                        formatValues.put("songId", cursor.getString(0))
                        formatValues.put("loudnessDb", cursor.getFloatOrNull(1))
                        formatValues.put("contentLength", cursor.getFloatOrNull(2))
                        db.insert("Format", CONFLICT_IGNORE, formatValues)
                    }
                }

            db.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs FROM Song;")
            db.execSQL("DROP TABLE Song;")
            db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }

    @DeleteColumn.Entries(
        DeleteColumn("Artist", "shuffleVideoId"),
        DeleteColumn("Artist", "shufflePlaylistId"),
        DeleteColumn("Artist", "radioVideoId"),
        DeleteColumn("Artist", "radioPlaylistId"),
    )
    class From20To21Migration : AutoMigrationSpec

    @DeleteColumn.Entries(DeleteColumn("Artist", "info"))
    class From21To22Migration : AutoMigrationSpec

    class From22To23Migration : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS Lyrics (`songId` TEXT NOT NULL, `fixed` TEXT, `synced` TEXT, PRIMARY KEY(`songId`), FOREIGN KEY(`songId`) REFERENCES `Song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")

            db.query(SimpleSQLiteQuery("SELECT id, lyrics, synchronizedLyrics FROM Song;")).use { cursor ->
                val lyricsValues = ContentValues(3)
                while (cursor.moveToNext()) {
                    lyricsValues.put("songId", cursor.getString(0))
                    lyricsValues.put("fixed", cursor.getString(1))
                    lyricsValues.put("synced", cursor.getString(2))
                    db.insert("Lyrics", CONFLICT_IGNORE, lyricsValues)
                }
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS Song_new (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT, `thumbnailUrl` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs FROM Song;")
            db.execSQL("DROP TABLE Song;")
            db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }


    class From23To24Migration : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE SongPlaylistMap ADD COLUMN setVideoId TEXT;")
            } catch (e: Exception) {
                println("Database From23To24Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From24To25Migration : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Playlist ADD COLUMN isEditable INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From24To25Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From25To26Migration : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Playlist ADD COLUMN isYoutubePlaylist INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From25To26Migration error ${e.stackTraceToString()}")
            }

        }
    }
    class From26To27Migration : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Album ADD COLUMN isYoutubeAlbum INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From26To27Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Artist ADD COLUMN isYoutubeArtist INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From26To27Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE SongPlaylistMap ADD COLUMN dateAdded INTEGER NULL;")
            } catch (e: Exception) {
                println("Database From26To27Migration error ${e.stackTraceToString()}")
            }

        }
    }
    class From31To32Migration : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS QueuedMediaItem_copy (`mediaId` TEXT NOT NULL, `mediaItem` BLOB NOT NULL, `position` INTEGER, `idQueue` INTEGER, PRIMARY KEY(`mediaId`));")
                db.execSQL("DROP TABLE IF EXISTS QueuedMediaItem;")
                db.execSQL("ALTER TABLE QueuedMediaItem_copy RENAME TO QueuedMediaItem;")
            } catch (e: Exception) {
                println("Database From31To32Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From38To39Migration : Migration(38, 39) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN folder TEXT;")
            } catch (e: Exception) {
                println("Database From38To39Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From40To41Migration : Migration(40, 41) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Blacklist ADD COLUMN name TEXT;")
            } catch (e: Exception) {
                println("Database From40To41Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From43To44Migration : Migration(43, 44) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN musicVaultState TEXT NOT NULL DEFAULT 'NONE'")
            } catch (e: Exception) {
                println("Database From43To44Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN musicVaultFileName TEXT")
            } catch (e: Exception) {
                println("Database From43To44Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN musicVaultThumbnailFileName TEXT")
            } catch (e: Exception) {
                println("Database From43To44Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From45To46Migration : Migration(45, 46) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Album ADD COLUMN genres TEXT;")
            } catch (e: Exception) {
                println("Database From45To46Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Artist ADD COLUMN genres TEXT;")
            } catch (e: Exception) {
                println("Database From45To46Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN genres TEXT;")
            } catch (e: Exception) {
                println("Database From45To46Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From54To55Migration : Migration(54, 55) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                // 1. Crea la nuova tabella con il DEFAULT 'NONE'
                db.execSQL("""
                    CREATE TABLE Song_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        mediaId TEXT,
                        title TEXT NOT NULL,
                        artistsText TEXT,
                        durationText TEXT,
                        thumbnailUrl TEXT,
                        likedAt INTEGER,
                        totalPlayTimeMs INTEGER NOT NULL DEFAULT 0,
                        isAudioOnly INTEGER NOT NULL DEFAULT 1,
                        isPodcast INTEGER NOT NULL DEFAULT 0,
                        folder TEXT,
                        musicVaultState TEXT NOT NULL DEFAULT 'NONE',
                        musicVaultFileName TEXT,
                        musicVaultThumbnailFileName TEXT,
                        genres TEXT
                    )
                """)

                // 2. Copia i dati
                db.execSQL("""
                    INSERT INTO Song_new (id, mediaId, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs, isAudioOnly, isPodcast, folder, musicVaultState, musicVaultFileName, musicVaultThumbnailFileName, genres)
                    SELECT id, mediaId, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs, isAudioOnly, isPodcast, folder, musicVaultState, musicVaultFileName, musicVaultThumbnailFileName, genres
                    FROM Song
                """)

                // 3. Rimuovi la vecchia tabella
                db.execSQL("DROP TABLE Song")

                // 4. Rinomina la nuova tabella
                db.execSQL("ALTER TABLE Song_new RENAME TO Song")
            } catch (e: Exception) {
                println("Database From54To55Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From55To56Migration : Migration(55, 56) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                // Profilo utente
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_artist_affinity (
                userId TEXT NOT NULL,
                artistId TEXT NOT NULL,
                score REAL NOT NULL,
                playCount INTEGER NOT NULL,
                totalPlayTimeMs INTEGER NOT NULL,
                lastPlayedAt INTEGER NOT NULL,
                likedSongs INTEGER NOT NULL,
                dislikedSongs INTEGER NOT NULL,
                bookmarked INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(userId, artistId)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_artist_affinity_userId_score ON user_artist_affinity(userId, score)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_artist_affinity_artistId ON user_artist_affinity(artistId)")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_keyword_affinity (
                userId TEXT NOT NULL,
                keyword TEXT NOT NULL,
                weight REAL NOT NULL,
                playCount INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(userId, keyword)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_keyword_affinity_userId_weight ON user_keyword_affinity(userId, weight)")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_era_affinity (
                userId TEXT NOT NULL,
                decade INTEGER NOT NULL,
                weight REAL NOT NULL,
                playCount INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(userId, decade)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_era_affinity_userId_weight ON user_era_affinity(userId, weight)")

                // Recommendation
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS recommendation (
                userId TEXT NOT NULL,
                songId TEXT NOT NULL,
                strategyId TEXT NOT NULL,
                score REAL NOT NULL,
                reasonsJson TEXT NOT NULL,
                generatedAt INTEGER NOT NULL,
                consumed INTEGER NOT NULL DEFAULT 0,
                consumedAt INTEGER,
                rejectedAt INTEGER,
                PRIMARY KEY(userId, songId, strategyId)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_userId_strategyId_score ON recommendation(userId, strategyId, score)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_userId_consumed ON recommendation(userId, consumed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_userId_rejectedAt ON recommendation(userId, rejectedAt)")

                // Artist relation (anticipata)
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS artist_relation (
                fromArtistId TEXT NOT NULL,
                toArtistId TEXT NOT NULL,
                relationType TEXT NOT NULL,
                direction TEXT NOT NULL DEFAULT 'bidirectional',
                fetchedAt INTEGER NOT NULL,
                PRIMARY KEY(fromArtistId, toArtistId, relationType)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_relation_fromArtistId ON artist_relation(fromArtistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_relation_toArtistId ON artist_relation(toArtistId)")
            } catch (e: Exception) {
                println("Database From55To56Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From56To57Migration : Migration(56, 57) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mb_album (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        primaryType TEXT,
                        secondaryTypes TEXT,
                        firstReleaseDate TEXT,
                        originalYear INTEGER,
                        genres TEXT,
                        tags TEXT,
                        rating REAL,
                        ratingVotes INTEGER,
                        wikipediaUrl TEXT,
                        links TEXT,
                        artistCredit TEXT,
                        artistMbIds TEXT,
                        matchedAlbumId TEXT,
                        matchScore REAL,
                        matchedAt INTEGER,
                        fetchedAt INTEGER NOT NULL,
                        popularityScore REAL NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_rating_ratingVotes ON mb_album(rating, ratingVotes)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_originalYear ON mb_album(originalYear)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_primaryType ON mb_album(primaryType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_fetchedAt ON mb_album(fetchedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_matchedAlbumId ON mb_album(matchedAlbumId)")

            } catch (e: Exception) {
                println("Database From55To56Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From57To58Migration : Migration(57, 58) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE song ADD COLUMN albumId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_albumId ON song(albumId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS song_artist_cross_ref (
                        songId TEXT NOT NULL,
                        artistId TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'main',
                        "order" INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(songId, artistId),
                        FOREIGN KEY(songId) REFERENCES Song(id) ON DELETE CASCADE,
                        FOREIGN KEY(artistId) REFERENCES Artist(id) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_artist_cross_ref_songId ON song_artist_cross_ref(songId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_artist_cross_ref_artistId ON song_artist_cross_ref(artistId)")
            } catch (e: Exception) {
                println("Database From57To58Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From58To59Migration : Migration(58, 59) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE Artist ADD COLUMN mbId TEXT")
            } catch (e: Exception) {
                println("Database From58To59Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From59To60Migration : Migration(59, 60) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                // === Artist ===
                // IMPORTANTE: DEFAULT NULL esplicito per matchare @ColumnInfo(defaultValue = "NULL")

                db.execSQL("ALTER TABLE Artist ADD COLUMN youtubeChannelId TEXT")
                // IMPORTANTE: NOT NULL per matchare il campo Kotlin non-nullable
                db.execSQL("ALTER TABLE Artist ADD COLUMN nature TEXT NOT NULL DEFAULT 'UNKNOWN'")

                // Popola youtubeChannelId per artisti YT esistenti
                db.execSQL("UPDATE Artist SET youtubeChannelId = id")

                // Indici non-unique
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_mbId ON Artist(mbId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_youtubeChannelId ON Artist(youtubeChannelId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_name ON Artist(name)")

                // === Album ===
                db.execSQL("ALTER TABLE Album ADD COLUMN mbId TEXT")
                db.execSQL("ALTER TABLE Album ADD COLUMN youtubeAlbumId TEXT")
                db.execSQL("ALTER TABLE Album ADD COLUMN nature TEXT NOT NULL DEFAULT 'UNKNOWN'")

                db.execSQL("UPDATE Album SET youtubeAlbumId = id")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_mbId ON Album(mbId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_youtubeAlbumId ON Album(youtubeAlbumId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_title_authorsText ON Album(title, authorsText)")

                // === MBAlbum ===
                db.execSQL("ALTER TABLE mb_album ADD COLUMN nature TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_nature ON mb_album(nature)")
            } catch (e: Exception) {
                println("Database From59To60Migration error ${e.stackTraceToString()}")
            }
        }
    }
}


@TypeConverters
object Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }
    }

    @TypeConverter
    @JvmStatic
    fun fromString(stringListString: String): List<String> {
        return stringListString.split(",").map { it }
    }

    @TypeConverter
    @JvmStatic
    fun toString(stringList: List<String>): String {
        return stringList.joinToString(separator = ",")
    }

    @TypeConverter
    @JvmStatic
    @UnstableApi
    fun mediaItemToByteArray(mediaItem: MediaItem?): ByteArray? {
        if (mediaItem == null) return null

        val parcel = Parcel.obtain()
        return try {
            @Suppress("DEPRECATION") // Necessario finché Media3 non fornisce un'alternativa a toBundle
            val bundle = mediaItem.toBundle()

            parcel.writeBundle(bundle)
            parcel.marshall()
        } catch (e: Exception) {
            //Timber.e("TypeConverter Errore serializzazione MediaItem ${e.message}")
            null
        } finally {
            parcel.recycle() // Fondamentale!
        }
    }

    @TypeConverter
    @JvmStatic
    @UnstableApi
    fun mediaItemFromByteArray(value: ByteArray?): MediaItem? {
        if (value == null) return null

        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(value, 0, value.size)
            parcel.setDataPosition(0)

            // ClassLoader sicuro con fallback
            val classLoader = MediaItem::class.java.classLoader
                ?: Thread.currentThread().contextClassLoader

            val bundle = parcel.readBundle(classLoader)

            @Suppress("DEPRECATION")
            bundle?.let(MediaItem::fromBundle)
        } catch (e: Exception) {
            //Timber.e("TypeConverter Errore deserializzazione MediaItem ${e.message}")
            null
        } finally {
            parcel.recycle()
        }
    }

    @TypeConverter
    @JvmStatic
    fun fromMusicVaultState(state: MusicVaultState): String = state.name

    @TypeConverter
    @JvmStatic
    fun toMusicVaultState(value: String): MusicVaultState =
        MusicVaultState.valueOf(value)

    // Configura Json per essere più flessibile e sicuro
    val RoomJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @TypeConverter
    @JvmStatic
    fun fromAlbumList(value: List<Album>): String {
        return RoomJson.encodeToString(value) // Serializza in JSON String
    }

    @TypeConverter
    @JvmStatic
    fun toAlbumList(value: String): List<Album> {
        return RoomJson.decodeFromString(value) // Deserializza da JSON String a Lista
    }

    @TypeConverter
    fun fromExternalLinkList(value: List<ExternalLink>?): String? {
        return value?.let { RoomJson.encodeToString(it) }
    }

    @TypeConverter
    fun toExternalLinkList(value: String?): List<ExternalLink>? {
        return value?.let { RoomJson.decodeFromString<List<ExternalLink>>(it) }
    }

    @TypeConverter
    fun artistNatureToString(nature: ArtistNature?): String? = nature?.name

    @TypeConverter
    fun stringToArtistNature(value: String?): ArtistNature? =
        value?.let { try { ArtistNature.valueOf(it) } catch (e: IllegalArgumentException) { ArtistNature.UNKNOWN } }

    @TypeConverter
    fun albumNatureToString(nature: AlbumNature?): String? = nature?.name

    @TypeConverter
    fun stringToAlbumNature(value: String?): AlbumNature? =
        value?.let { try { AlbumNature.valueOf(it) } catch (e: IllegalArgumentException) { AlbumNature.UNKNOWN } }

}
