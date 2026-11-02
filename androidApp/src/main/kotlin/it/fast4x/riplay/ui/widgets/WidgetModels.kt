package it.fast4x.riplay.ui.widgets

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Stato "grezzo" del player, prodotto dal PlayerService.
 * Non contiene preferenze utente: è la materia prima che il
 * [WidgetUpdater] combina con le impostazioni per generare [WidgetData].
 */
data class PlayerWidgetSnapshot(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    /** Percorso della copertina salvata in cache ([WidgetArtworkCache]), null se non disponibile */
    val artworkPath: String? = null,
    /** Stato del like corrente, vedi [LikeState] */
    val likedState: Int = LikeState.NONE,
    val shuffleEnabled: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
)

/** Stati possibili del like del brano corrente. */
object LikeState {
    const val DISLIKED = -1
    const val NONE = 0
    const val LIKED = 1
}

/**
 * Dati completi pubblicati nei widget: snapshot del player + preferenze
 * utente risolte (tema, accento, sfondo, copertina, pulsanti). È l'unica fonte
 * letta dai widget in fase di rendering.
 */
data class WidgetData(
    val snapshot: PlayerWidgetSnapshot = PlayerWidgetSnapshot(),
    /** Nome del [it.fast4x.riplay.enums.WidgetTheme] scelto dall'utente */
    val themeName: String = WidgetThemeNames.SYSTEM,
    /** Nome del [it.fast4x.riplay.enums.WidgetAccentColor] scelto dall'utente */
    val accentName: String = WidgetAccentNames.DEFAULT,
    /** Nome del [it.fast4x.riplay.enums.WidgetBackground] scelto dall'utente */
    val backgroundName: String = WidgetBackgroundNames.SOLID,
    /** Nome del [it.fast4x.riplay.enums.WidgetBadgePosition] scelto dall'utente */
    val badgePositionName: String = WidgetBadgeNames.BOTTOM_END,
    val showArtwork: Boolean = true,
    val showLikeButton: Boolean = true,
    val showShuffleButton: Boolean = true,
    val showProgress: Boolean = true,
) {

    /**
     * Confronto per la deduplica degli aggiornamenti: ignora posizione e durata,
     * che cambiano continuamente ma vengono pubblicate dal canale dedicato
     * [WidgetUpdater.pushPosition].
     */
    fun hasSameVisualContent(other: WidgetData?): Boolean {
        if (other == null) return false
        val a = copy(snapshot = snapshot.copy(positionMs = 0L, durationMs = 0L))
        val b = other.copy(snapshot = other.snapshot.copy(positionMs = 0L, durationMs = 0L))
        return a == b
    }
}

/** Nomi dei temi gestiti dai widget (rispecchiano it.fast4x.riplay.enums.WidgetTheme). */
object WidgetThemeNames {
    const val SYSTEM = "System"
    const val LIGHT = "Light"
    const val DARK = "Dark"
    const val PURE_BLACK = "PureBlack"
}

/** Nomi degli accenti gestiti dai widget (rispecchiano it.fast4x.riplay.enums.WidgetAccentColor). */
object WidgetAccentNames {
    const val DEFAULT = "Default"
    const val ORANGE = "Orange"
    const val RED = "Red"
    const val PINK = "Pink"
    const val PURPLE = "Purple"
    const val BLUE = "Blue"
    const val TEAL = "Teal"
    const val GREEN = "Green"
}

/** Nomi degli sfondi gestiti dai widget (rispecchiano it.fast4x.riplay.enums.WidgetBackground). */
object WidgetBackgroundNames {
    const val SOLID = "Solid"
    const val SEMI_TRANSPARENT = "SemiTransparent"
    const val TRANSPARENT = "Transparent"
}

/** Nomi delle posizioni del badge coda (rispecchiano it.fast4x.riplay.enums.WidgetBadgePosition). */
object WidgetBadgeNames {
    const val TOP_START = "TopStart"
    const val TOP_END = "TopEnd"
    const val BOTTOM_START = "BottomStart"
    const val BOTTOM_END = "BottomEnd"
}

/** Chiavi DataStore condivise da tutti i widget RiPlay. */
object WidgetKeys {
    val title = stringPreferencesKey("widget_title")
    val artist = stringPreferencesKey("widget_artist")
    val isPlaying = booleanPreferencesKey("widget_isPlaying")
    val artworkPath = stringPreferencesKey("widget_artworkPath")
    val likedState = intPreferencesKey("widget_likedState")
    val shuffleEnabled = booleanPreferencesKey("widget_shuffleEnabled")
    val positionMs = longPreferencesKey("widget_positionMs")
    val durationMs = longPreferencesKey("widget_durationMs")
    val queueIndex = intPreferencesKey("widget_queueIndex")
    val queueSize = intPreferencesKey("widget_queueSize")
    val themeName = stringPreferencesKey("widget_themeName")
    val accentName = stringPreferencesKey("widget_accentName")
    val backgroundName = stringPreferencesKey("widget_backgroundName")
    val badgePosition = stringPreferencesKey("widget_badgePosition")
    val showArtwork = booleanPreferencesKey("widget_showArtwork")
    val showLikeButton = booleanPreferencesKey("widget_showLikeButton")
    val showShuffleButton = booleanPreferencesKey("widget_showShuffleButton")
    val showProgress = booleanPreferencesKey("widget_showProgress")
}

/** Scrive tutti i campi del [WidgetData] nel datastore del widget. */
fun WidgetData.applyTo(prefs: MutablePreferences) {
    prefs[WidgetKeys.title] = snapshot.title
    prefs[WidgetKeys.artist] = snapshot.artist
    prefs[WidgetKeys.isPlaying] = snapshot.isPlaying
    prefs[WidgetKeys.artworkPath] = snapshot.artworkPath ?: ""
    prefs[WidgetKeys.likedState] = snapshot.likedState
    prefs[WidgetKeys.shuffleEnabled] = snapshot.shuffleEnabled
    prefs[WidgetKeys.positionMs] = snapshot.positionMs
    prefs[WidgetKeys.durationMs] = snapshot.durationMs
    prefs[WidgetKeys.queueIndex] = snapshot.queueIndex
    prefs[WidgetKeys.queueSize] = snapshot.queueSize
    prefs[WidgetKeys.themeName] = themeName
    prefs[WidgetKeys.accentName] = accentName
    prefs[WidgetKeys.backgroundName] = backgroundName
    prefs[WidgetKeys.badgePosition] = badgePositionName
    prefs[WidgetKeys.showArtwork] = showArtwork
    prefs[WidgetKeys.showLikeButton] = showLikeButton
    prefs[WidgetKeys.showShuffleButton] = showShuffleButton
    prefs[WidgetKeys.showProgress] = showProgress
}

/** Legge i dati del widget dal datastore (i valori mancanti usano i default). */
fun Preferences.toWidgetData(): WidgetData {
    val snapshot = PlayerWidgetSnapshot(
        title = this[WidgetKeys.title] ?: "",
        artist = this[WidgetKeys.artist] ?: "",
        isPlaying = this[WidgetKeys.isPlaying] == true,
        artworkPath = this[WidgetKeys.artworkPath]?.takeIf { it.isNotEmpty() },
        likedState = this[WidgetKeys.likedState] ?: LikeState.NONE,
        shuffleEnabled = this[WidgetKeys.shuffleEnabled] == true,
        positionMs = this[WidgetKeys.positionMs] ?: 0L,
        durationMs = this[WidgetKeys.durationMs] ?: 0L,
        queueIndex = this[WidgetKeys.queueIndex] ?: 0,
        queueSize = this[WidgetKeys.queueSize] ?: 0,
    )
    return WidgetData(
        snapshot = snapshot,
        themeName = this[WidgetKeys.themeName] ?: WidgetThemeNames.SYSTEM,
        accentName = this[WidgetKeys.accentName] ?: WidgetAccentNames.DEFAULT,
        backgroundName = this[WidgetKeys.backgroundName] ?: WidgetBackgroundNames.SOLID,
        badgePositionName = this[WidgetKeys.badgePosition] ?: WidgetBadgeNames.BOTTOM_END,
        showArtwork = this[WidgetKeys.showArtwork] ?: true,
        showLikeButton = this[WidgetKeys.showLikeButton] ?: true,
        showShuffleButton = this[WidgetKeys.showShuffleButton] ?: true,
        showProgress = this[WidgetKeys.showProgress] ?: true,
    )
}

/** Formatta una durata in millisecondi come "m:ss" oppure "h:mm:ss". */
fun formatWidgetTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
