package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.utils.WEBDAV_KEY_PREFIX
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDavAccountDao {
    // Usiamo IGNORE: se l'URL, l'utente e la cartella esistono già, non fa nulla e ritorna -1
    // invece di cancellare e reinserire.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: WebDavAccount): Long
    @Update
    suspend fun update(account: WebDavAccount)

    @Delete
    suspend fun delete(account: WebDavAccount)

    suspend fun deleteAccountAndAllData(account: WebDavAccount) {
        val webDavServer = account.baseUrl.substringBeforeLast("/")
        deleteWebDavBlacklistedSongs(webDavServer)
        deleteWebDavEventsSongs(webDavServer)
        deleteWebDavFormatsSong(webDavServer)
        deleteWebDavQueuedSongs(webDavServer)
        deleteWebDavPlaylistsSongs(webDavServer)

        deleteWebDavSongs(webDavServer)
        delete(account)
    }

    @Query("DELETE FROM Blacklist where id LIKE '$WEBDAV_KEY_PREFIX' || :id || '%'")
    suspend fun deleteWebDavBlacklistedSongs(id: String)

    @Query("DELETE FROM Event where songId LIKE '$WEBDAV_KEY_PREFIX' || :id || '%'")
    suspend fun deleteWebDavEventsSongs(id: String)

    @Query("DELETE FROM Format where songId LIKE '$WEBDAV_KEY_PREFIX' || :id || '%'")
    suspend fun deleteWebDavFormatsSong(id: String)

    @Query("DELETE FROM QueuedMediaItem where mediaId LIKE '$WEBDAV_KEY_PREFIX' || :id || '%'")
    suspend fun deleteWebDavQueuedSongs(id: String)

    @Query("DELETE FROM Song where id LIKE '$WEBDAV_KEY_PREFIX' || :id || '%'")
    suspend fun deleteWebDavSongs(id: String)

    @Query("DELETE FROM SongPlaylistMap where songId LIKE '$WEBDAV_KEY_PREFIX' || :id || '%'")
    suspend fun deleteWebDavPlaylistsSongs(id: String)

    @Query("SELECT * FROM webdav_account ORDER BY name ASC")
    fun getAllAsFlow(): Flow<List<WebDavAccount>>

    @Query("SELECT * FROM webdav_account")
    suspend fun getAll(): List<WebDavAccount>

    @Query("SELECT * FROM webdav_account WHERE id = :id")
    suspend fun getById(id: Long): WebDavAccount?

    @Query("SELECT * FROM webdav_account WHERE isMusicSource = 0 LIMIT 1")
    suspend fun getBackupAccount(): WebDavAccount?
}