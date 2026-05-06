package it.fast4x.riplay.extensions.experimental.appearancepreset

import android.content.Context
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearanceSettings
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetSource
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.toShareString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AppearancePresetRepositoryImpl(
    private val context: Context
) : AppearancePresetRepository {


    override fun localPresets(): List<AppearancePreset> = listOf(
        AppearancePreset(
            id = "aura",
            name = "Aura",
            imageRes = R.drawable.preset0,
            author = "Fast4x",
            source = PresetSource.LOCAL,
            settings = AppearanceSettings.Aura
        ),
        AppearancePreset(
            id       = "deck",
            name     = "Deck",
            imageRes = R.drawable.preset1,
            author = "Fast4x",
            source   = PresetSource.LOCAL,
            settings = AppearanceSettings.Deck
        ),
        AppearancePreset(
            id       = "zen",
            name     = "Zen",
            imageRes = R.drawable.preset2,
            author = "Fast4x",
            source   = PresetSource.LOCAL,
            settings = AppearanceSettings.Zen
        ),
        AppearancePreset(
            id       = "noir",
            name     = "Noir",
            imageRes = R.drawable.preset3,
            author = "Fast4x",
            source   = PresetSource.LOCAL,
            settings = AppearanceSettings.Noir
        ),
        AppearancePreset(
            id       = "prism",
            name     = "Prism",
            imageRes = R.drawable.preset4,
            author = "Fast4x",
            source   = PresetSource.LOCAL,
            settings = AppearanceSettings.Prism
        ),
        AppearancePreset(
            id       = "groove",
            name     = "Groove",
            imageRes = R.drawable.preset5,
            author = "Fast4x",
            source   = PresetSource.LOCAL,
            settings = AppearanceSettings.Groove
        ),
    )


    override fun remotePresets(): Flow<List<AppearancePreset>> =
        flow { emit(emptyList()) }

    override suspend fun loadSharedPreset(shareUrl: String): Result<AppearancePreset> =
        runCatching {
            throw NotImplementedError("Backend non ancora configurato")
        }

    override suspend fun sharePreset(preset: AppearancePreset): Result<String> =
        runCatching {
            val encoded = preset.settings.toShareString()
            "https://tuoapp.com/preset?data=$encoded"
        }
}
