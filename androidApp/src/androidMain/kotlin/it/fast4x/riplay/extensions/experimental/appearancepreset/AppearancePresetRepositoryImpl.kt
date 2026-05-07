package it.fast4x.riplay.extensions.experimental.appearancepreset

import android.content.Context
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearanceSettings
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetSource
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.fromShareString
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.toShareString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

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


    override fun remotePresets(): Flow<List<AppearancePreset>> = flow {
        val url = URL("https://fast4x.github.io/RiPlay/themes/index.json")
        val json = with(withContext(Dispatchers.IO) {
            url.openConnection()
        } as HttpURLConnection) {
            connectTimeout = 5_000
            readTimeout    = 5_000
            requestMethod  = "GET"
            try {
                inputStream.bufferedReader().readText()
            } finally {
                disconnect()
            }
        }

        val presets = parseRemotePresets(json)
        emit(presets)
    }.catch { e ->
        // Rete assente o JSON malformato: emette lista vuota, i preset locali restano visibili
        Timber.d("Appearance remote themes or presets not available ${e.message}")
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

    private fun parseRemotePresets(json: String): List<AppearancePreset> {
        val root   = JSONObject(json)
        val array  = root.getJSONArray("themes")

        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                runCatching {
                    add(
                        AppearancePreset(
                            id          = obj.getString("id"),
                            name        = obj.getString("name"),
                            author      = obj.getString("author"),
                            imageUrl    = obj.getString("imageUrl"),
                            source      = PresetSource.REMOTE,
                            settings    = AppearanceSettings.fromShareString(
                                obj.getString("shareString")
                            )
                        )
                    )
                }

            }
        }
    }

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
