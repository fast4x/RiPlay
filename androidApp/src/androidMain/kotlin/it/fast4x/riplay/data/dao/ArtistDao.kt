package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.enums.ArtistNature
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM Artist WHERE youtubeChannelId = :channelId LIMIT 1")
    suspend fun getByYoutubeChannelId(channelId: String): Artist?

    @Query("SELECT * FROM Artist WHERE name IS NOT NULL")
    suspend fun getAllWithName(): List<Artist>

    // Match per nome normalizzato (lo facciamo in-memory perché SQLite non ha regex nativi)
    @Query("SELECT * FROM Artist WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun findByNameExactIgnoreCase(name: String): Artist?

    @Query("SELECT * FROM Artist WHERE id = :id")
    suspend fun getById(id: String): Artist?

    @Query("SELECT * FROM Artist WHERE nature = :nature")
    suspend fun getArtistsByNature(nature: ArtistNature): List<Artist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artist: Artist)

}