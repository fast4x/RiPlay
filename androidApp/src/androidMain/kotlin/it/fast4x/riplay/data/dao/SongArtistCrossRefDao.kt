package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongArtistCrossRef

@Dao
interface SongArtistCrossRefDao {

    @Query("""
        SELECT a.* FROM artist a
        INNER JOIN song_artist_cross_ref sac ON sac.artistId = a.id
        WHERE sac.songId = :songId
        ORDER BY sac."order"
    """)
    suspend fun getArtistsForSong(songId: String): List<Artist>

    @Query("""
        SELECT a.* FROM artist a
        INNER JOIN song_artist_cross_ref sac ON sac.artistId = a.id
        WHERE sac.songId IN (:songIds)
    """)
    suspend fun getArtistsForSongs(songIds: List<String>): List<Artist>

    @Query("""
        SELECT s.* FROM song s
        INNER JOIN song_artist_cross_ref sac ON sac.songId = s.id
        WHERE sac.artistId = :artistId
          AND s.isPodcast = 0
        ORDER BY s.totalPlayTimeMs DESC
        LIMIT :limit
    """)
    suspend fun getSongsByArtist(artistId: String, limit: Int): List<Song>

    @Query("""
        SELECT s.* FROM song s
        INNER JOIN song_artist_cross_ref sac ON sac.songId = s.id
        WHERE sac.artistId = :artistId
          AND s.isPodcast = 0
          AND s.totalPlayTimeMs = 0
        LIMIT :limit
    """)
    suspend fun getUnplayedSongsByArtist(artistId: String, limit: Int): List<Song>

    @Query("SELECT COUNT(*) FROM song_artist_cross_ref WHERE songId = :songId")
    suspend fun countArtistsForSong(songId: String): Int

    @Query("SELECT COUNT(*) FROM song_artist_cross_ref")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM song_artist_cross_ref")
    suspend fun countSongsWithArtist(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<SongArtistCrossRef>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ref: SongArtistCrossRef)

    @Query("DELETE FROM song_artist_cross_ref WHERE songId = :songId")
    suspend fun deleteForSong(songId: String)

    // SongArtistCrossRefDao.kt
    @Query("""
    SELECT sac.songId as songId, a.* FROM artist a
    INNER JOIN song_artist_cross_ref sac ON sac.artistId = a.id
    WHERE sac.songId IN (:songIds)
    ORDER BY sac."order"
""")
    suspend fun getArtistsWithSongId(songIds: List<String>): List<SongArtistPair>

    data class SongArtistPair(
        val songId: String,
        @Embedded val artist: Artist
    )
}