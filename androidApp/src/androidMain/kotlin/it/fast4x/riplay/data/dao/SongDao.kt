package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Query
import it.fast4x.riplay.data.models.Song


@Dao
interface SongDao {
    @Query(
        """
    SELECT s.* FROM song s
    LEFT JOIN song_artist_cross_ref sac ON sac.songId = s.id
    LEFT JOIN artist a ON a.id = sac.artistId
    WHERE s.isPodcast = 0
      AND s.totalPlayTimeMs = 0
      AND (
        (a.beginYear IS NOT NULL AND a.beginYear >= :decadeStart AND a.beginYear < :decadeEnd)
        OR (s.albumId IS NOT NULL AND EXISTS (
            SELECT 1 FROM album al WHERE al.id = s.albumId 
            AND al.originalYear IS NOT NULL 
            AND al.originalYear >= :decadeStart 
            AND al.originalYear < :decadeEnd
        ))
      )
    LIMIT :limit
"""
    )
    suspend fun getSongsByDecade(decadeStart: Int, decadeEnd: Int, limit: Int): List<Song>

    @Query("SELECT * FROM song WHERE id = :songId")
    suspend fun getById(songId: String): Song?

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

}
// Helper extension
suspend fun SongDao.getSongsByDecade(decade: Int, limit: Int): List<Song> =
    getSongsByDecade(decadeStart = decade, decadeEnd = decade + 10, limit = limit)
