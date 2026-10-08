package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import it.fast4x.riplay.data.models.UnplayableTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface UnplayableTracksDao {

    @Upsert
    suspend fun upsert(track: UnplayableTrack)

    @Query("SELECT videoId FROM unplayable_tracks")
    fun observeAllIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM unplayable_tracks")
    fun countAll(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM unplayable_tracks WHERE videoId = :videoId)")
    suspend fun isMarked(videoId: String): Boolean

    @Query("DELETE FROM unplayable_tracks WHERE videoId = :videoId")
    suspend fun clear(videoId: String)

    @Query("DELETE FROM unplayable_tracks WHERE markedWhileLoggedIn = :currentLoggedIn")
    suspend fun revokeForLoginState(currentLoggedIn: Boolean)   // revoca al cambio login

    @Query("DELETE FROM unplayable_tracks")
    suspend fun clearAll()
}