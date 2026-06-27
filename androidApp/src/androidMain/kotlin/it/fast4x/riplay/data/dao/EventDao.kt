package it.fast4x.riplay.data.dao

import android.database.SQLException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.Event

@Dao
interface EventDao {

    @Query("""
    SELECT COUNT(*) FROM Event e
    INNER JOIN song_artist_cross_ref sac ON sac.songId = e.songId
    WHERE sac.artistId = :artistId
""")
    suspend fun getPlayCountByArtist(artistId: String): Int

    @Query("SELECT COUNT(*) FROM Event WHERE songId = :songId")
    suspend fun getPlayCountBySong(songId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Throws(SQLException::class)
    fun insert(event: Event)

}