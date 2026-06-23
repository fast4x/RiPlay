package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.MBAlbum

@Dao
interface MBAlbumDao {
    @Query("SELECT * FROM mb_album")
    suspend fun getAll(): List<MBAlbum>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mbAlbum: MBAlbum)

}