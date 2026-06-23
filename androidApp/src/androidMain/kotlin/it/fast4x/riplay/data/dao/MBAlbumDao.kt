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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mbAlbum: MBAlbum)

}