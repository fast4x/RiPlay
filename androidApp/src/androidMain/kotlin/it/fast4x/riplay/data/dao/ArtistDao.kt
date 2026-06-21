package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.Artist

@Dao
interface ArtistDao {
    // ArtistDao.kt
    @Query("""
    SELECT * FROM artist 
    WHERE mbId IS NULL 
      AND name IS NOT NULL 
      AND name != ''
      AND id NOT LIKE 'virtual::%'
    ORDER BY timestamp DESC
    LIMIT :limit
""")
    suspend fun getArtistsWithoutMbId(limit: Int): List<Artist>

    @Query("SELECT * FROM artist WHERE mbId = :mbId LIMIT 1")
    suspend fun getByMbId(mbId: String): Artist?

    @Query("SELECT COUNT(*) FROM artist WHERE mbId IS NOT NULL")
    suspend fun countWithMbId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artist: Artist)

}