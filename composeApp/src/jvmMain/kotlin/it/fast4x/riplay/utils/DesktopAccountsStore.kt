package it.fast4x.riplay.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Tipo di scrobble, porting desktop di LastFmScrobbleType /
 * ListenBrainzScrobbleType Android:
 *  - Simple:     il brano viene registrato (scrobble/listen) quando INIZIA
 *                (l'equivalente desktop di onMediaItemTransition PlayerService);
 *  - NowPlaying: il brano viene pubblicato come "in ascolto ora"
 *                (track.updateNowPlaying / listen_type=playing_now).
 */
enum class DesktopScrobbleType {
    Simple,
    NowPlaying;

    val key: String
        get() = name.lowercase()
}

/**
 * STEP 3ao: IMPOSTAZIONI ACCOUNT (Last.fm, ListenBrainz, Discord, WebDAV)
 * sul DESKTOP — replica delle chiavi AppSettings Android usate da
 * AccountsSettings.kt:
 *
 *  - isEnabledLastFM            -> lastfmEnabled + lastfmSessionKey
 *  - lastFMSessionToken         -> lastfmSessionKey
 *  - lastFmScrobbleType         -> lastfmScrobbleType
 *  - isEnabledListenBrainz      -> listenbrainzEnabled
 *  - listenBrainzToken          -> listenbrainzToken
 *  - listenBrainzUsername       -> listenbrainzUsername
 *  - listenBrainzServerUrl      -> listenbrainzServerUrl
 *  - listenBrainzScrobbleType   -> listenbrainzScrobbleType
 *  - isDiscordPresenceEnabled   -> discordEnabled
 *  - discordPersonalAccessToken -> discordToken
 *  - discordAccountName         -> discordUserName (+ discordUserAvatar)
 *  - isWebDavEnabled            -> webDavEnabled
 *
 * Stesso schema di DesktopCloseToTray/DesktopMiniPlayer: stato Compose
 * (mutableStateOf, la UI si ridisegna da sola) + persistenza IMMEDIATA
 * su ~/.riplay/accounts.properties (l'utente non deve "salvare" nulla).
 *
 * I servizi fuori dalla composizione (DesktopMediaIntegrations, player,
 * presence Discord) leggono i valori in modo NON-composable con i getter
 * plain (es. lastFmSessionKeyOrNull): le proprieta' Compose restano
 * comunque leggibili da qualunque thread.
 *
 * NB: i token sono salvati in chiaro nel file utente (~/.riplay, permessi
 * utente) esattamente come fa il gestore account YouTube Music desktop
 * (account.properties) e come le SharedPreferences Android.
 */
object DesktopAccountsStore {

    private val prefsFile: File
        get() = File(System.getProperty("user.home"), ".riplay").resolve("accounts.properties")

    // ── LAST.FM ─────────────────────────────────────────────────────────────
    var lastFmEnabled: Boolean by mutableStateOf(false)
        private set
    var lastFmSessionKey: String by mutableStateOf("")
        private set
    var lastFmScrobbleType: DesktopScrobbleType by mutableStateOf(DesktopScrobbleType.Simple)
        private set

    // ── LISTENBRAINZ ────────────────────────────────────────────────────────
    var listenBrainzEnabled: Boolean by mutableStateOf(false)
        private set
    var listenBrainzToken: String by mutableStateOf("")
        private set
    var listenBrainzUsername: String by mutableStateOf("")
        private set

    /** Server ListenBrainz: vuoto = server ufficiale, altrimenti istanza self-hosted. */
    var listenBrainzServerUrl: String by mutableStateOf("")
        private set
    var listenBrainzScrobbleType: DesktopScrobbleType by mutableStateOf(DesktopScrobbleType.Simple)
        private set

    // ── DISCORD ─────────────────────────────────────────────────────────────
    var discordEnabled: Boolean by mutableStateOf(false)
        private set
    var discordToken: String by mutableStateOf("")
        private set
    var discordUserName: String by mutableStateOf("")
        private set
    var discordUserAvatar: String by mutableStateOf("")
        private set

    // ── WEBDAV ──────────────────────────────────────────────────────────────
    var webDavEnabled: Boolean by mutableStateOf(false)
        private set

    init {
        load()
    }

    // ── LETTURE NON-COMPOSABLE (per i servizi) ──────────────────────────────

    fun lastFmSessionKeyOrNull(): String? =
        lastFmSessionKey.takeIf { lastFmEnabled && it.isNotBlank() }

    fun listenBrainzTokenOrNull(): String? =
        listenBrainzToken.takeIf { listenBrainzEnabled && it.isNotBlank() }

    fun discordTokenOrNull(): String? =
        discordToken.takeIf { discordEnabled && it.isNotBlank() }

    // ── SCRITTURA + PERSISTENZA (una funzione per campo, come Android
    //    appSettingsManager.updateSettings(copy(...))) ──────────────────────

    fun updateLastFmEnabled(value: Boolean) {
        lastFmEnabled = value
        persist("lastfmEnabled", value.toString())
    }

    fun updateLastFmSessionKey(value: String) {
        lastFmSessionKey = value
        persist("lastfmSessionKey", value)
    }

    fun updateLastFmScrobbleType(value: DesktopScrobbleType) {
        lastFmScrobbleType = value
        persist("lastfmScrobbleType", value.name)
    }

    fun updateListenBrainzEnabled(value: Boolean) {
        listenBrainzEnabled = value
        persist("listenbrainzEnabled", value.toString())
    }

    fun updateListenBrainzToken(value: String) {
        listenBrainzToken = value
        persist("listenbrainzToken", value)
    }

    fun updateListenBrainzUsername(value: String) {
        listenBrainzUsername = value
        persist("listenbrainzUsername", value)
    }

    fun updateListenBrainzServerUrl(value: String) {
        listenBrainzServerUrl = value
        persist("listenbrainzServerUrl", value)
    }

    fun updateListenBrainzScrobbleType(value: DesktopScrobbleType) {
        listenBrainzScrobbleType = value
        persist("listenbrainzScrobbleType", value.name)
    }

    fun updateDiscordEnabled(value: Boolean) {
        discordEnabled = value
        persist("discordEnabled", value.toString())
    }

    fun setDiscordLogin(token: String, username: String, avatar: String) {
        discordToken = token
        discordUserName = username
        discordUserAvatar = avatar
        persistAll(
            "discordToken" to token,
            "discordUserName" to username,
            "discordUserAvatar" to avatar
        )
    }

    fun updateWebDavEnabled(value: Boolean) {
        webDavEnabled = value
        persist("webDavEnabled", value.toString())
    }

    // ── PERSISTENZA ─────────────────────────────────────────────────────────

    private fun load() {
        try {
            val p = Properties()
            if (prefsFile.isFile) FileInputStream(prefsFile).use { p.load(it) }
            lastFmEnabled = p.getProperty("lastfmEnabled", "false").equals("true", true)
            lastFmSessionKey = p.getProperty("lastfmSessionKey", "").orEmpty()
            lastFmScrobbleType =
                runCatching { DesktopScrobbleType.valueOf(p.getProperty("lastfmScrobbleType", "Simple")) }
                    .getOrDefault(DesktopScrobbleType.Simple)
            listenBrainzEnabled = p.getProperty("listenbrainzEnabled", "false").equals("true", true)
            listenBrainzToken = p.getProperty("listenbrainzToken", "").orEmpty()
            listenBrainzUsername = p.getProperty("listenbrainzUsername", "").orEmpty()
            listenBrainzServerUrl = p.getProperty("listenbrainzServerUrl", "").orEmpty()
            listenBrainzScrobbleType =
                runCatching { DesktopScrobbleType.valueOf(p.getProperty("listenbrainzScrobbleType", "Simple")) }
                    .getOrDefault(DesktopScrobbleType.Simple)
            discordEnabled = p.getProperty("discordEnabled", "false").equals("true", true)
            discordToken = p.getProperty("discordToken", "").orEmpty()
            discordUserName = p.getProperty("discordUserName", "").orEmpty()
            discordUserAvatar = p.getProperty("discordUserAvatar", "").orEmpty()
            webDavEnabled = p.getProperty("webDavEnabled", "false").equals("true", true)
            println("[RiPlay-AccountsStore] caricato: lastfm=${lastFmEnabled} lb=${listenBrainzEnabled} discord=${discordEnabled} webdav=${webDavEnabled}")
        } catch (t: Throwable) {
            println("[RiPlay-AccountsStore] load errore: ${t.message}")
        }
    }

    private fun persist(key: String, value: String) = persistAll(key to value)

    /** Persistenza silenziosa: mai bloccare la UI per un file di preferenze. */
    private fun persistAll(vararg entries: Pair<String, String>) {
        try {
            val p = Properties()
            if (prefsFile.isFile) FileInputStream(prefsFile).use { p.load(it) }
            entries.forEach { (k, v) -> p.setProperty(k, v) }
            prefsFile.parentFile?.mkdirs()
            FileOutputStream(prefsFile).use { p.store(it, "RiPlay desktop - accounts (lastfm/listenbrainz/discord/webdav)") }
        } catch (t: Throwable) {
            println("[RiPlay-AccountsStore] persist errore: ${t.message}")
        }
    }
}
