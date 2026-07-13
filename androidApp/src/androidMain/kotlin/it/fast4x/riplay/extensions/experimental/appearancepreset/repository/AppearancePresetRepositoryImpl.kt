package it.fast4x.riplay.extensions.experimental.appearancepreset.repository

import android.content.Context
import android.util.Base64
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePreset
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearancePresetDto
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.AppearanceSettings
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.PresetSource
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.extensions.experimental.appearancepreset.models.RemoteThemesResponse
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.fromCurrentSettings
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.toDomain
import it.fast4x.riplay.extensions.experimental.appearancepreset.utils.toEntity
import it.fast4x.riplay.utils.DbSettingsJson
import it.fast4x.riplay.utils.appContext
import kotlinx.coroutines.flow.map

class AppearancePresetRepositoryImpl(
    private val context: Context
) : AppearancePresetRepository {

    val dao = Database.appearancePresetDao()

    val user_custom_legacy_preset = "user_custom_legacy_preset"

    override fun getAllPresets(): Flow<List<AppearancePreset>> {
        return dao.getAllPresets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActivePreset(): Flow<String?> {
        return dao.getActivePreset()
    }

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


    override fun syncRemotePresets(): Flow<Unit> = flow {
        val url = URL("https://fast4x.github.io/RiPlay/themes/index.json")
        val json = withContext(Dispatchers.IO) {
            (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                requestMethod = "GET"
            }.run {
                try { inputStream.bufferedReader().readText() } finally { disconnect() }
            }
        }

        val response = DbSettingsJson.decodeFromString<RemoteThemesResponse>(json)

        // SALVA NEL DATABASE i preset scaricati diventano entità persistenti.
        response.themes.forEach { dto ->
            val entity = dto.toDomain().toEntity() // DTO -> Domain -> Entity
            dao.insertPreset(entity)
        }

        emit(Unit)

    }.catch { e ->
        //Timber.d("Appearance remote themes or presets not available ${e.message}")
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    override suspend fun loadSharedPreset(shareUrl: String): Result<AppearancePreset> =
        runCatching {
            // Estrae la parte encoded dall'URL (es: ?data=eyJ...)
            val encodedData = shareUrl.toUri().getQueryParameter("data")
                ?: throw IllegalArgumentException("URL non valido o mancante del parametro data")

            // Decodifica dal Base64
            val json = String(Base64.decode(encodedData, Base64.DEFAULT))

            // Deserializza e mappa al dominio
            val dto = DbSettingsJson.decodeFromString<AppearancePresetDto>(json)
            // IMPORTANTE: Lo salviamo nel DB prima di restituirlo!
            val domainPreset = dto.toDomain()
            dao.insertPreset(domainPreset.toEntity())

            domainPreset
        }

    override suspend fun sharePreset(preset: AppearancePreset): Result<String> =
        runCatching {
            // Converte in DTO
            val dto = preset.toDto()

            // Serializza in JSON (con encodeDefaults=false sarà minuscolo!)
            val json = DbSettingsJson.encodeToString(dto)

            // Codifica in Base64 URL-safe per non rompere i link
            val encoded = Base64.encodeToString(
                json.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP or Base64.URL_SAFE
            )

            // Ipotesi di url per condividere il preset
            "https://riplayapp.xxx/preset?data=$encoded"
        }

    override suspend fun ensurePresetsMigrated() {
        // Se il DB non congtiene user_custom_legacy_preset vuol dire che la migrazione non c'è stata
        if (dao.getPresetById(user_custom_legacy_preset) == null) {
            val context = appContext()

            // Estrae le impostazioni vecchie usando la tua vecchia funzione
            val oldSettings = AppearanceSettings.fromCurrentSettings(context)

            // Crea un preset "Tema Personalizzato" con le impostazioni da sharedPrefernces
            val customPreset = AppearancePreset(
                id = user_custom_legacy_preset,
                name = "My old theme",
                author = "You",
                source = PresetSource.LOCAL,
                settings = oldSettings
            )
            dao.insertPreset(customPreset.toEntity())

            // Imposto come attivo!
            dao.setActivePreset(user_custom_legacy_preset)
        }

        // Infine popolo il resto dei preset built-in
        ensureBuiltInPresetsExist()
    }

    // Chiamato una tantum all'avvio dell'app per migrare i preset built-in
    suspend fun ensureBuiltInPresetsExist() {
        // Controlla se esiste almeno un preset built-in
        if (dao.getPresetById("aura") == null) {
            val builtIns = listOf(
                AppearancePreset(id = "aura", name = "Aura", imageRes = R.drawable.preset0, author = "Fast4x", source = PresetSource.LOCAL, settings = AppearanceSettings.Aura),
                AppearancePreset(id = "deck", name = "Deck", imageRes = R.drawable.preset1, author = "Fast4x", source = PresetSource.LOCAL, settings = AppearanceSettings.Deck),
                AppearancePreset(id = "zen", name = "Zen", imageRes = R.drawable.preset2, author = "Fast4x", source = PresetSource.LOCAL, settings = AppearanceSettings.Zen),
                AppearancePreset(id = "noir", name = "Noir", imageRes = R.drawable.preset3, author = "Fast4x", source = PresetSource.LOCAL, settings = AppearanceSettings.Noir),
                AppearancePreset(id = "prism", name = "Prism", imageRes = R.drawable.preset4, author = "Fast4x", source = PresetSource.LOCAL, settings = AppearanceSettings.Prism),
                AppearancePreset(id = "groove", name = "Groove", imageRes = R.drawable.preset5, author = "Fast4x", source = PresetSource.LOCAL, settings = AppearanceSettings.Groove),
            )
            builtIns.map { it.toEntity() }.forEach { dao.insertPreset(it) }
        }
    }
}
