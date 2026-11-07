package it.fast4x.riplay.extensions.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import it.fast4x.riplay.MainActivity
import timber.log.Timber

/**
 * ALARM (Sveglia) — scheduling con AlarmManager.
 *
 * Viene usata [AlarmManager.setAlarmClock], la stessa API delle app orologio di sistema:
 *  - non richiede il permesso SCHEDULE_EXACT_ALARM;
 *  - viene rispettata anche in modalità Doze;
 *  - mostra l'icona sveglia nella status bar e permette all'utente di
 *    toccarla per aprire l'app.
 *
 * In caso (raro, alcuni ROM) in cui il sistema blocca le sveglie esatte,
 * si degrada con grazia su setAndAllowWhileIdle (imprecisa ma consentita).
 *
 * Ogni sveglia ha un proprio PendingIntent (requestCode = id.hashCode()).
 * Lo snooze usa un requestCode dedicato così da non sovrascrivere la
 * programmazione normale della sveglia.
 */
object AlarmScheduler {

    private const val SNOOZE_REQUEST_CODE_OFFSET = 1_000_000

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return alarmManager?.canScheduleExactAlarms() ?: false
    }

    // ------------------------------------------------------------------
    // PendingIntent
    // ------------------------------------------------------------------

    private fun firePendingIntent(context: Context, alarmId: String, snooze: Boolean): PendingIntent {
        val requestCode = if (snooze)
            (SNOOZE_REQUEST_CODE_OFFSET + alarmId.hashCode())
        else
            alarmId.hashCode()

        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_ALARM_FIRE)
            .setPackage(context.packageName)
            .putExtra(EXTRA_ALARM_ID, alarmId)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            10_000,
            Intent(context, MainActivity::class.java).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // ------------------------------------------------------------------
    // Programmazione
    // ------------------------------------------------------------------

    /** Programa (o riprogramma) la sveglia al suo prossimo istante di attivazione. */
    fun schedule(context: Context, alarm: AlarmItem) {
        if (!alarm.enabled) {
            cancel(context, alarm.id)
            return
        }

        val triggerAt = alarm.nextTriggerAtMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        val pendingIntent = firePendingIntent(context, alarm.id, snooze = false)

        runCatching {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, openAppPendingIntent(context)),
                    pendingIntent
                )
            } else {
                // Fallback di grazia: sveglia imprecisa ma sempre consentita
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
            Timber.i("AlarmScheduler scheduled alarm ${alarm.id} (${alarm.timeLabel}) at $triggerAt")
        }.onFailure {
            Timber.e("AlarmScheduler schedule failed: ${it.message}")
        }
    }

    /**
     * Snooze: riprogramma la stessa sveglia tra [minutes] minuti come
     * sveglia "una volta" dedicata (non tocca la programmazione normale).
     */
    fun scheduleSnooze(context: Context, alarmId: String, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        val pendingIntent = firePendingIntent(context, alarmId, snooze = true)

        runCatching {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, openAppPendingIntent(context)),
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
            Timber.i("AlarmScheduler snooze alarm $alarmId in $minutes minutes")
        }.onFailure {
            Timber.e("AlarmScheduler snooze failed: ${it.message}")
        }
    }

    fun cancelSnooze(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        runCatching {
            alarmManager.cancel(firePendingIntent(context, alarmId, snooze = true))
        }
    }

    /** Disattiva la sveglia: rimuove l'alarm programmato e lo snooze pendente. */
    fun cancel(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        runCatching {
            alarmManager.cancel(firePendingIntent(context, alarmId, snooze = false))
            alarmManager.cancel(firePendingIntent(context, alarmId, snooze = true))
        }
        Timber.i("AlarmScheduler canceled alarm $alarmId")
    }

    /**
     * Riprogramma tutte le sveglie attive. Chiamato al boot, al cambio ora/fuso
     * e quando l'app si aggiorna (le sveglie AlarmManager non sopravvivono a questi eventi).
     */
    fun rescheduleAll(context: Context) {
        val alarms = loadAlarms(context)
        alarms.forEach { alarm ->
            if (alarm.enabled) schedule(context, alarm)
        }
        Timber.i("AlarmScheduler rescheduleAll: ${alarms.count { it.enabled }} active alarms")
    }
}
