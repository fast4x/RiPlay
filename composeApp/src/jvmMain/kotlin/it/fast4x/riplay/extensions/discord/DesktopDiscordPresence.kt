package it.fast4x.riplay.extensions.discord

import it.fast4x.riplay.config.EnvironmentConfig
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.DesktopAccountsStore
import it.fast4x.riplay.utils.DesktopAppInfo
import it.fast4x.riplay.utils.DesktopPlaybackHub
import it.fast4x.riplay.utils.DesktopSecureConfig
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * PORTING DESKTOP di DiscordPresenceManager.kt (androidApp): gestisce la
 * Rich Presence Discord su RiPlay desktop.
 *
 * Comportamento replicato dall'originale:
 *  - attivita' LISTENING (type 2) "RiPlay": details = titolo, state = artista;
 *  - timestamps start = adesso - posizione, end = start + durata;
 *  - pausa: "⏸️ Paused: <titolo>" con timestamp congelati (start = end);
 *  - refresh della presence ogni 15 s (riga di avanzamento viva in Discord);
 *  - dopo 5 minuti di pausa CONTINUA la presence viene cancellata (issue #238)
 *    e la connessione gateway chiusa; al play successivo si riconnette;
 *  - cambio token (nuovo login) -> riconnessione pulita;
 *  - artwork: cover del brano convertita in asset "mp:external/..." via
 *    l'endpoint external-assets dell'applicazione Discord (su desktop le
 *    cover online sono gia' URL http: no upload su litterbox, conversione
 *    diretta e risultato CACHE-ATO per URL: prima ogni refresh rifaceva
 *    la POST ogni 15 s e gli errori venivano solo assorbiti);
 *    fallback = icona RiPlay su GitHub, come Android;
 *  - pulsanti "Get RiPlay" / "Listen to YTMusic" (stessi URL Android).
 *
 * Il driver e' DesktopMediaIntegrations (utils): il polling 1 s del player
 * chiama onPlayingStateChanged / onTrackChanged; qui vive solo la logica
 * presence.
 */
class DesktopDiscordPresenceManager(
    private val getToken: () -> String? = { DesktopAccountsStore.discordTokenOrNull() },
    externalScope: CoroutineScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("DiscordPresenceDesktopScope")
    )
) {

    companion object {
        private val APPLICATION_ID: String =
            DesktopSecureConfig.getApiKey(EnvironmentConfig.RiPlay_DISCORD_APPLICATION_ID)
        private const val PAUSE_CLEAR_TIMEOUT_MS = 5 * 60 * 1000L // 5 minuti
        private const val RIPLAY_ICON_URL =
            "https://raw.githubusercontent.com/fast4x/RiPlay/main/assets/design/latest/icon.png"
    }

    private val discordScope = externalScope
    private var refreshJob: Job? = null

    @Volatile
    private var rpc: DiscordRpcGateway? = null

    @Volatile
    private var lastToken: String? = null

    @Volatile
    private var lastSong: Song? = null

    @Volatile
    private var lastPositionMs: Long = 0L

    @Volatile
    private var lastDurationMs: Long = 0L

    @Volatile
    private var stopped = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Cache URL copertina -> asset Discord (mp:external/...). */
    private val assetCache = ConcurrentHashMap<String, String>()

    @Volatile
    private var smallImage: String? = null

    /** true = token rifiutato dall'API (ultima verifica); la UI lo puo' mostrare. */
    @Volatile
    var lastTokenInvalid: Boolean = false
        private set

    // ── VERIFICA TOKEN ──────────────────────────────────────────────────────

    /** true = valido, false = rifiutato, null = errore di rete (riprovare). */
    fun validateToken(token: String): Boolean? {
        val request = Request.Builder()
            .url("https://discord.com/api/v9/users/@me")
            .header("Authorization", token)
            .get()
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    401, 403 -> false
                    429 -> null // rate limit: riprova piu' tardi
                    else -> response.isSuccessful
                }
            }
        }.getOrElse { null }
    }

    // ── ASSET DISCORD ───────────────────────────────────────────────────────

    /** Converte un URL pubblico in asset Discord (mp:external/...) con cache. */
    private fun getDiscordAsset(imageUrl: String, token: String): String? {
        if (imageUrl.startsWith("mp:")) return imageUrl
        assetCache[imageUrl]?.let { return it }
        if (APPLICATION_ID.isBlank()) return null
        val request = Request.Builder()
            .url("https://discord.com/api/v9/applications/$APPLICATION_ID/external-assets")
            .header("Authorization", token)
            .post(
                buildJsonObject { putJsonArray("urls") { add(imageUrl) } }.toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("[RiPlay-Discord] external-assets http ${response.code} (rate limit?)")
                    return@use null
                }
                val body = response.body?.string() ?: return@use null
                val arr = kotlinx.serialization.json.Json
                    .parseToJsonElement(body) as kotlinx.serialization.json.JsonArray
                val path = arr.firstOrNull()
                    ?.jsonObject?.get("external_asset_path")
                    ?.jsonPrimitive?.content
                    ?: return@use null
                val asset = "mp:$path"
                assetCache[imageUrl] = asset
                asset
            }
        }.getOrNull()
    }

    private fun getSmallImage(token: String): String? {
        smallImage?.let { return it }
        return getDiscordAsset(RIPLAY_ICON_URL, token)?.also { smallImage = it }
    }

    private fun getLargeImage(song: Song, token: String): String? {
        val artworkUrl = song.thumbnailUrl?.takeIf { it.startsWith("http") } ?: RIPLAY_ICON_URL
        return getDiscordAsset(artworkUrl, token) ?: getDiscordAsset(RIPLAY_ICON_URL, token)
    }

    // ── PRESENCE ────────────────────────────────────────────────────────────

    /** Nuovo brano in riproduzione (cambio traccia): aggiorna lo stato interno. */
    fun onTrackChanged(song: Song?, positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        if (stopped) return
        val token = getToken() ?: return
        if (token.isEmpty()) return
        lastSong = song
        lastPositionMs = positionMs
        lastDurationMs = durationMs
        if (song == null) {
            sendPausedPresence(token)
        } else if (isPlaying) {
            sendPlayingPresence(token, positionMs)
            startRefreshJob(token)
        } else {
            sendPausedPresence(token)
            startRefreshJob(token)
        }
    }

    /** Cambio play/pausa sul brano corrente. */
    fun onPlayingStateChanged(isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        if (stopped) return
        val token = getToken() ?: return
        if (token.isEmpty()) return
        lastPositionMs = positionMs
        lastDurationMs = durationMs
        val song = lastSong
        if (song == null) return
        if (isPlaying) {
            sendPlayingPresence(token, positionMs)
            startRefreshJob(token)
        } else {
            sendPausedPresence(token)
            startRefreshJob(token)
        }
    }

    private fun ensureGateway(token: String) {
        if (token != lastToken) {
            rpc?.closeConnection()
            rpc = DiscordRpcGateway(token)
            lastToken = token
        } else if (rpc == null) {
            // La connessione era stata chiusa (pausa prolungata): riconnette.
            rpc = DiscordRpcGateway(token)
        }
    }

    /** Presence "in riproduzione": timer vivo (start = adesso - posizione). */
    private fun sendPlayingPresence(token: String, positionMs: Long) {
        val song = lastSong ?: return
        val now = System.currentTimeMillis()
        val start = now - positionMs
        val end = start + lastDurationMs
        val title = song.title.ifBlank { "Unknown title" }
        val artist = song.artistsText?.ifBlank { null } ?: "Unknown artist"
        discordScope.launch {
            sendActivity(
                token = token,
                song = song,
                details = title,
                state = artist,
                start = start,
                end = end
            )
        }
    }

    /** Presence "in pausa": timer congelato (come Android). */
    private fun sendPausedPresence(token: String) {
        val song = lastSong ?: return
        val frozen = System.currentTimeMillis() - lastPositionMs
        val title = song.title.ifBlank { "Unknown title" }
        val artist = song.artistsText?.ifBlank { null } ?: "Unknown artist"
        discordScope.launch {
            sendActivity(
                token = token,
                song = song,
                details = "\u23F8\uFE0F Paused: $title",
                state = artist,
                start = frozen,
                end = frozen
            )
        }
    }

    private suspend fun sendActivity(
        token: String,
        song: Song,
        details: String,
        state: String,
        start: Long,
        end: Long
    ) {
        if (stopped) return
        // Verifica del token (solo quando cambia o e' stale: la validazione
        // ripetuta ogni 15 s e' traffico inutile verso /users/@me)
        val validity = validateToken(token)
        lastTokenInvalid = validity == false
        when (validity) {
            false -> {
                println("[RiPlay-Discord] token non valido: presence sospesa (riconnettersi dall'Account)")
                return
            }
            null -> {
                println("[RiPlay-Discord] errore di rete nella verifica token, aggiornamento saltato")
                return
            }
            true -> { /* token valido: continua */ }
        }

        ensureGateway(token)
        val largeImage = getLargeImage(song, token)
        val smallImage = getSmallImage(token)
        val largeText = if (state.isNotBlank()) "$details - $state" else details
        val watchUrl = if (song.id.startsWith("webdav:") || song.id.startsWith("local:")) null
        else "https://music.youtube.com/watch?v=${song.id}"

        val activity = buildJsonObject {
            put("name", "RiPlay")
            put("type", DiscordRpcGateway.ACTIVITY_TYPE_LISTENING)
            put("details", details.take(120))
            put("state", state.take(120))
            if (APPLICATION_ID.isNotBlank()) put("application_id", APPLICATION_ID)
            putJsonObject("timestamps") {
                put("start", start)
                put("end", end)
            }
            putJsonObject("assets") {
                largeImage?.let { put("large_image", it) }
                put("large_text", largeText.take(120))
                smallImage?.let { put("small_image", it) }
                put("small_text", "v${DesktopAppInfo.VERSION}")
            }
            // I pulsanti richiedono application_id (come su Android)
            if (APPLICATION_ID.isNotBlank()) {
                putJsonArray("buttons") {
                    add("Get RiPlay")
                    if (watchUrl != null) add("Listen to YTMusic")
                }
                putJsonObject("metadata") {
                    val urls = buildList {
                        add("https://github.com/fast4x/RiPlay")
                        watchUrl?.let { add(it) }
                    }
                    putJsonArray("button_urls") { urls.forEach { add(it) } }
                }
            }
        }

        val sent: Boolean = runCatching { rpc?.sendPresence(activity) ?: false }.getOrDefault(false)
        if (!sent) println("[RiPlay-Discord] presence non inviata (gateway non connesso?)")
    }

    /**
     * Ciclo di refresh (come startRefreshJob Android): ogni 15 s
     * ripubblica la presence con la posizione corrente; dopo 5 minuti di
     * pausa continua cancella la presence e chiude il gateway (issue #238).
     */
    private fun startRefreshJob(token: String) {
        refreshJob?.cancel()
        refreshJob = discordScope.launch {
            var pauseStartedAt = -1L
            while (isActive && !stopped) {
                delay(15_000L)
                val playing = DesktopPlaybackHub.activeController?.state?.value?.isPlaying == true
                if (playing) {
                    pauseStartedAt = -1L
                    val pos = DesktopPlaybackHub.activeController?.state?.value?.timestamp ?: lastPositionMs
                    lastPositionMs = pos
                    sendPlayingPresence(token, pos)
                } else {
                    val now = System.currentTimeMillis()
                    if (pauseStartedAt < 0L) pauseStartedAt = now
                    if (now - pauseStartedAt >= PAUSE_CLEAR_TIMEOUT_MS) {
                        clearPresence()
                        break
                    }
                    sendPausedPresence(token)
                }
            }
        }
    }

    /** Cancella la presence chiudendo la connessione gateway (come clearPresence Android). */
    fun clearPresence() {
        if (stopped) return
        discordScope.launch {
            runCatching { rpc?.closeConnection() }
            rpc = null
            lastToken = null
            println("[RiPlay-Discord] presence cancellata (gateway chiuso)")
        }
    }

    /** Stop totale (logout/uscita). */
    fun onStop() {
        stopped = true
        refreshJob?.cancel()
        runCatching { rpc?.closeConnection() }
        rpc = null
        discordScope.cancel()
    }
}
