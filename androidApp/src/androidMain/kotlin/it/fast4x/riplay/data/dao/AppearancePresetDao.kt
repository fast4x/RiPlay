package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.AppearancePresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppearancePresetDao {

    @Query("SELECT * FROM appearance_presets ORDER BY isBuiltIn DESC, name ASC")
    fun getAllPresets(): Flow<List<AppearancePresetEntity>>

    @Query("SELECT * FROM appearance_presets WHERE id = :id")
    suspend fun getPresetById(id: String): AppearancePresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: AppearancePresetEntity)

    @Query("DELETE FROM appearance_presets WHERE isBuiltIn = 0")
    suspend fun deleteNonBuiltInPresets() // Utile se l'utente vuole fare "Ripristina predefiniti"

    @Query("SELECT COUNT(*) FROM appearance_presets")
    suspend fun getPresetCount(): Int

    @Query("UPDATE app_settings SET activePresetId = :id WHERE id = 1")
    suspend fun setActivePreset(id: String)

    @Query("SELECT activePresetId FROM app_settings WHERE id = 1")
    fun getActivePreset(): Flow<String?>

    @Query("UPDATE appearance_presets SET settingsJson = :jsonSettings WHERE id = :id")
    suspend fun updatePresetSettings(id: String, jsonSettings: String)

}