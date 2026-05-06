package it.fast4x.riplay.extensions.experimental.appearancepreset

import android.content.Context
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PlayerSettings
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetSource
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.toShareString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AppearancePresetRepositoryImpl(
    private val context: Context
) : AppearancePresetRepository {


    override fun localPresets(): List<AppearancePreset> = listOf(
        AppearancePreset(
            id = "modern",
            name = "Modern",
            imageRes = R.drawable.preset0,
            source = PresetSource.LOCAL,
            settings = PlayerSettings.Modern
        ),
        AppearancePreset(
            id       = "minimal",
            name     = "Minimal",
            imageRes = R.drawable.preset1,
            source   = PresetSource.LOCAL,
            settings = PlayerSettings.Minimal
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
