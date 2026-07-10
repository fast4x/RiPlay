package it.fast4x.riplay.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val activePresetId: String = "aura" // Il default
)