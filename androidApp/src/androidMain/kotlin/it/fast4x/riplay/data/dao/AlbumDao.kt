package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song

@Dao
interface AlbumDao {

    @Query("SELECT * FROM album WHERE id = :albumId")
    suspend fun getById(albumId: String): Album?

    @Query("SELECT * FROM album WHERE mbId = :mbId LIMIT 1")
    suspend fun getByMbId(mbId: String): Album?

    @Query("SELECT * FROM Album WHERE youtubeAlbumId = :ytId LIMIT 1")
    suspend fun getByYoutubeAlbumId(ytId: String): Album?

    @Query("""
    SELECT * FROM Album 
    WHERE lower(title) = lower(:title) 
      AND lower(authorsText) = lower(:artist) 
    LIMIT 1
""")
    suspend fun findByTitleAndArtistExact(title: String, artist: String): Album?

    @Query("SELECT * FROM Album WHERE mbId IS NULL AND title IS NOT NULL LIMIT :limit")
    suspend fun getAlbumsWithoutMbId(limit: Int): List<Album>

    @Query("SELECT * FROM Album")
    suspend fun getAllAlbums(): List<Album>

    @Query("""
    SELECT * FROM Album 
    WHERE (mbId IS NULL OR mbId = '') 
      AND title IS NOT NULL 
      AND title != ''
      AND authorsText IS NOT NULL
      AND title NOT IN ('Audio', 'WhatsApp Audio', 'Unknown Album')
      AND title NOT LIKE 'Audio %'
    ORDER BY timestamp DESC
    LIMIT :limit
""")
    suspend fun getYtAlbumsWithoutMbId(limit: Int): List<Album>

    @Query("""
    SELECT a.* FROM Album a
    WHERE a.authorsText LIKE '%' || :artistName || '%'
    ORDER BY a.originalYear ASC
""")
    suspend fun getAlbumsByArtistName(artistName: String): List<Album>

    // recupera album per artista via cross-ref
    @Query("""
    SELECT a.* FROM Album a
    INNER JOIN song_artist_cross_ref sac ON sac.songId IN (SELECT id FROM Song WHERE albumId = a.id)
    WHERE sac.artistId = :artistId
    GROUP BY a.id
    ORDER BY a.originalYear ASC
""")
    suspend fun getAlbumsByArtist(artistId: String): List<Album>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(album: Album)

}