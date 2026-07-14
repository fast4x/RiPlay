package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT activePresetId FROM app_settings WHERE id = 1")
    fun getActivePreset(): Flow<String?>

    @Query("SELECT settingsJson FROM app_settings WHERE id = 1")
    suspend fun getSettings(): String?

    @Query("UPDATE app_settings SET settingsJson = :jsonSettings WHERE id = 1")
    suspend fun updateSettings(jsonSettings: String)

}