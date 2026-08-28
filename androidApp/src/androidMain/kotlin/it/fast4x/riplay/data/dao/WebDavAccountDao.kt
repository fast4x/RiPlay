package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import it.fast4x.riplay.data.models.WebDavAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDavAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: WebDavAccount): Long

    @Update
    suspend fun update(account: WebDavAccount)

    @Delete
    suspend fun delete(account: WebDavAccount)

    @Query("SELECT * FROM webdav_account ORDER BY name ASC")
    fun getAllAsFlow(): Flow<List<WebDavAccount>>

    @Query("SELECT * FROM webdav_account")
    suspend fun getAll(): List<WebDavAccount>

    @Query("SELECT * FROM webdav_account WHERE id = :id")
    suspend fun getById(id: Long): WebDavAccount?
}