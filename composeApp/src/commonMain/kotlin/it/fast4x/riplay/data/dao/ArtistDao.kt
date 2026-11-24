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
    // Negative cache: esclude gli artisti la cui risoluzione mbId è fallita
    // negli ultimi N giorni (mbLookupFailedAt recente). Con cutoff = 0 si
    // ri-tentano tutti.
    @Query("""
    SELECT * FROM artist 
    WHERE mbId IS NULL 
      AND name IS NOT NULL 
      AND name != ''
      AND id NOT LIKE 'virtual::%'
      AND (mbLookupFailedAt IS NULL OR mbLookupFailedAt < :failedBefore)
    ORDER BY timestamp DESC
    LIMIT :limit
""")
    suspend fun getArtistsWithoutMbId(limit: Int, failedBefore: Long): List<Artist>

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

    // Ponte ZoundHub: artisti con Spotify ID noto (da url-rels MusicBrainz).
    // Per questi il match col catalogo AI è ESATTO per ID, senza fuzzy.
    @Query("SELECT * FROM Artist WHERE spotifyId = :spotifyId LIMIT 1")
    suspend fun getBySpotifyId(spotifyId: String): Artist?

    @Query("SELECT * FROM Artist WHERE spotifyId IS NOT NULL AND name IS NOT NULL")
    suspend fun getArtistsWithSpotifyId(): List<Artist>

    @Query("""
    SELECT * FROM Artist
    WHERE beginYear IS NOT NULL
      AND beginYear >= :minYear
      AND (genres IS NOT NULL OR tags IS NOT NULL)
    ORDER BY beginYear DESC
    LIMIT :limit
""")
    suspend fun getRecentArtists(minYear: Int, limit: Int): List<Artist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artist: Artist)

    // Music Assistant bridge (riplay-bridge): paginazione libreria per /api/v1.
    @Query("SELECT * FROM Artist WHERE name IS NOT NULL ORDER BY name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllArtistsPaged(limit: Int, offset: Int): List<Artist>

    @Query("SELECT COUNT(*) FROM Artist WHERE name IS NOT NULL")
    suspend fun countArtists(): Int

}