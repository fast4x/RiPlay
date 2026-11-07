package it.fast4x.riplay.extensions.alarm

import android.content.Context
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.utils.globalContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

/**
 * ALARM (Sveglia) — modello dati e persistenza.
 *
 * Le sveglie vengono salvate come lista JSON in SharedPreferences:
 * non serve quindi nessuna migration del database Room e le sveglie
 * sopravvivono a riavvii del telefono grazie al receiver di boot.
 *
 * Ogni sveglia punta a una sorgente musicale:
 *  - [songId]     -> brano della libreria (o brano online scelto via ricerca)
 *  - [playlistId] -> playlist locale dell'app
 *  - [albumId]    -> album della libreria (i brani vengono riprodotti in ordine traccia)
 * [songSnapshot] conserva i metadati del brano scelto online, così la
 * sveglia può ricostruire il MediaItem anche se il brano non è mai stato
 * riprodotto prima e non è presente nel database.
 */
@Serializable
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val enabled: Boolean = true,
    val hour: Int = 7,
    val minute: Int = 30,
    /**
     * Giorni di ripetizione in formato ISO (1 = Lunedì ... 7 = Domenica).
     * Insieme vuoto = sveglia "una volta sola".
     */
    val repeatDays: Set<Int> = emptySet(),
    val songId: String? = null,
    val playlistId: Long? = null,
    val albumId: String? = null,
    val songSnapshot: Song? = null,
    val sourceName: String = "",
    val shuffle: Boolean = false,
) {
    val hasSource: Boolean
        get() = songId != null || playlistId != null || albumId != null

    val timeLabel: String
        get() = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
}

/**
 * Opzioni globali della sveglia (valgono per tutte le sveglie).
 */
@Serializable
data class AlarmOptions(
    /** Volume che sale gradualmente quando la sveglia suona. */
    val fadeInEnabled: Boolean = true,
    /**
     * Auto-stop della riproduzione dopo N minuti (0 = mai).
     * Utile per non lasciare la musica attiva se si esce di casa.
     */
    val autoStopMinutes: Int = 15,
    /** Durata dello snooze dal pulsante nella notifica. */
    val snoozeMinutes: Int = 5,
)

private val alarmJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// ------------------------------------------------------------------
// Azioni/extra condivisi tra receiver e PlayerService
// ------------------------------------------------------------------

/** Azioni gestite da PlayerService.onStartCommand */
object AlarmServiceActions {
    const val PLAY = "it.fast4x.riplay.alarmplay"
    const val STOP = "it.fast4x.riplay.alarmstop"
}

/** Azioni gestite da AlarmReceiver */
const val ACTION_ALARM_FIRE = "it.fast4x.riplay.alarm.ACTION_FIRE"
const val ACTION_ALARM_SNOOZE = "it.fast4x.riplay.alarm.ACTION_SNOOZE"
const val ACTION_ALARM_STOP = "it.fast4x.riplay.alarm.ACTION_STOP"

const val EXTRA_ALARM_ID = "alarmId"

// ------------------------------------------------------------------
// Persistenza (SharedPreferences "preferences")
// ------------------------------------------------------------------

private const val PREF_KEY_ALARMS = "alarmItems"
private const val PREF_KEY_ALARM_OPTIONS = "alarmOptions"

fun loadAlarms(context: Context = globalContext()): List<AlarmItem> {
    return runCatching {
        val raw = context.preferences.getString(PREF_KEY_ALARMS, null) ?: return emptyList()
        alarmJson.decodeFromString<List<AlarmItem>>(raw)
    }.onFailure {
        Timber.e("Alarm loadAlarms failed: ${it.message}")
    }.getOrDefault(emptyList())
}

fun saveAlarms(context: Context, alarms: List<AlarmItem>) {
    runCatching {
        context.preferences.edit()
            .putString(PREF_KEY_ALARMS, alarmJson.encodeToString(alarms))
            .apply()
    }.onFailure {
        Timber.e("Alarm saveAlarms failed: ${it.message}")
    }
}

fun loadAlarmOptions(context: Context = globalContext()): AlarmOptions {
    return runCatching {
        val raw = context.preferences.getString(PREF_KEY_ALARM_OPTIONS, null)
        raw?.let { alarmJson.decodeFromString<AlarmOptions>(it) } ?: AlarmOptions()
    }.onFailure {
        Timber.e("Alarm loadAlarmOptions failed: ${it.message}")
    }.getOrDefault(AlarmOptions())
}

fun saveAlarmOptions(context: Context, options: AlarmOptions) {
    runCatching {
        context.preferences.edit()
            .putString(PREF_KEY_ALARM_OPTIONS, alarmJson.encodeToString(options))
            .apply()
    }.onFailure {
        Timber.e("Alarm saveAlarmOptions failed: ${it.message}")
    }
}

/**
 * Calcola il prossimo istante di attivazione (epoch millis) di una sveglia
 * in base a ora, minuti e giorni di ripetizione.
 */
fun nextTriggerAtMillis(
    hour: Int,
    minute: Int,
    repeatDays: Set<Int>,
    fromMillis: Long = System.currentTimeMillis(),
): Long {
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = fromMillis
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    // Sveglia "una volta": domani se l'orario di oggi è già passato
    if (repeatDays.isEmpty()) {
        if (calendar.timeInMillis <= fromMillis) calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }

    // Sveglia ripetuta: primo giorno (tra oggi e i prossimi 7) tra i giorni scelti
    for (i in 0..7) {
        if (calendar.timeInMillis > fromMillis) {
            val isoDay = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.MONDAY -> 1
                java.util.Calendar.TUESDAY -> 2
                java.util.Calendar.WEDNESDAY -> 3
                java.util.Calendar.THURSDAY -> 4
                java.util.Calendar.FRIDAY -> 5
                java.util.Calendar.SATURDAY -> 6
                else -> 7 // SUNDAY
            }
            if (isoDay in repeatDays) return calendar.timeInMillis
        }
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
    }

    return calendar.timeInMillis
}

fun AlarmItem.nextTriggerAtMillis(fromMillis: Long = System.currentTimeMillis()): Long =
    nextTriggerAtMillis(hour, minute, repeatDays, fromMillis)
