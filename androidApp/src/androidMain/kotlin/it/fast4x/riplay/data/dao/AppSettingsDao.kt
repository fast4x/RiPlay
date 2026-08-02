package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("INSERT OR IGNORE INTO `app_settings` (`id`, `activePresetId`, `settingsJson`, `activeAppearanceJson`) VALUES (1, 'aura', '{}', '{}')")
    suspend fun ensureBaselineRowExists()

    @Query("SELECT activePresetId FROM app_settings WHERE id = 1")
    fun getActivePreset(): Flow<String?>

    @Query("SELECT settingsJson FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<String>

    @Query("SELECT activeAppearanceJson FROM app_settings WHERE id = 1")
    fun getActiveAppearanceSettings(): Flow<String>


    @Query("UPDATE app_settings SET settingsJson = :jsonSettings WHERE id = 1")
    suspend fun updateSettings(jsonSettings: String)

    @Query("UPDATE app_settings SET activeAppearanceJson = :jsonSettings WHERE id = 1")
    suspend fun updateActiveAppearanceSettings(jsonSettings: String)

}