package it.fast4x.riplay.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appearance_presets")
data class AppearancePresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String?,
    val imageUrl: String?,
    val localImageRes: Int?,
    val isBuiltIn: Boolean = false,
    val settingsJson: String
)