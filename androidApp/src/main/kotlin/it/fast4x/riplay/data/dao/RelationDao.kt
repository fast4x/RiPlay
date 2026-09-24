package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.ArtistRelation

@Dao
interface RelationDao {
    @Query("""
        SELECT * FROM artist_relation
        WHERE fromArtistId = :artistId OR toArtistId = :artistId
    """)
    suspend fun getBidirectional(artistId: String): List<ArtistRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistRelation(items: List<ArtistRelation>)
}