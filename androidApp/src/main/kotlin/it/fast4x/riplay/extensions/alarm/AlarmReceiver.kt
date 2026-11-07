package it.fast4x.riplay.extensions.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.utils.sendCommandToPlayerService
import timber.log.Timber

/**
 * ALARM (Sveglia) — receiver degli eventi di attivazione.
 *
 * FIRE  : la sveglia è scattata -> aggiorna la programmazione (prossima
 *         ricorrenza o disattivazione se "una volta") e avvia la
 *         riproduzione tramite PlayerService (azione alarmplay).
 * SNOOZE: dalla notifica -> ferma la riproduzione e riprogramma la
 *         sveglia tra N minuti (dalle opzioni sveglia).
 * STOP  : dalla notifica -> ferma la riproduzione e cancella lo snooze pendente.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return

        when (intent.action) {
            ACTION_ALARM_FIRE -> handleFire(context, alarmId)
            ACTION_ALARM_SNOOZE -> handleSnooze(context, alarmId)
            ACTION_ALARM_STOP -> handleStop(context, alarmId)
        }
    }

    private fun handleFire(context: Context, alarmId: String) {
        Timber.i("AlarmReceiver FIRE alarm $alarmId")

        val alarm = loadAlarms(context).firstOrNull { it.id == alarmId }
        if (alarm == null) {
            Timber.w("AlarmReceiver FIRE: alarm $alarmId not found, ignoring")
            return
        }

        // Aggiorna la programmazione:
        //  - sveglia ripetuta -> programma la prossima ricorrenza
        //  - sveglia "una volta" -> la disattiva dopo l'uso
        if (alarm.repeatDays.isEmpty()) {
            val updated = alarm.copy(enabled = false)
            saveAlarms(context, loadAlarms(context).map {
                if (it.id == alarmId) updated else it
            })
            AlarmScheduler.cancel(context, alarmId)
        } else {
            AlarmScheduler.schedule(context, alarm)
        }

        // Avvia la riproduzione nel PlayerService (foreground service).
        // setAlarmClock mette temporaneamente l'app in allowlist, quindi
        // l'avvio del foreground service dal receiver è consentito anche
        // a dispositivo bloccato / Doze (esenzione "exact alarm" di Android 12+).
        // Su alcune ROM particolarmente restrittive l'avvio può comunque
        // essere rifiutato: registrato in log senza far crashare il receiver.
        val serviceIntent = Intent(context, PlayerService::class.java)
            .setAction(AlarmServiceActions.PLAY)
            .setPackage(context.packageName)
            .putExtra(EXTRA_ALARM_ID, alarmId)
        runCatching {
            sendCommandToPlayerService(serviceIntent)
            Timber.i("AlarmReceiver FIRE: PlayerService avviato per la sveglia $alarmId")
        }.onFailure {
            Timber.e("AlarmReceiver FIRE: avvio PlayerService rifiutato dal sistema: ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    private fun handleSnooze(context: Context, alarmId: String) {
        val snoozeMinutes = loadAlarmOptions(context).snoozeMinutes.coerceAtLeast(1)
        Timber.i("AlarmReceiver SNOOZE alarm $alarmId in $snoozeMinutes minutes")

        // Ferma la riproduzione corrente
        runCatching {
            sendCommandToPlayerService(
                Intent(context, PlayerService::class.java)
                    .setAction(AlarmServiceActions.STOP)
                    .setPackage(context.packageName)
            )
        }.onFailure {
            Timber.e("AlarmReceiver SNOOZE: invio stop al servizio fallito: ${it.message}")
        }

        // Riprogramma lo snooze
        AlarmScheduler.scheduleSnooze(context, alarmId, snoozeMinutes)
    }

    private fun handleStop(context: Context, alarmId: String) {
        Timber.i("AlarmReceiver STOP alarm $alarmId")

        // Rimuove anche un eventuale snooze ancora pendente
        AlarmScheduler.cancelSnooze(context, alarmId)

        runCatching {
            sendCommandToPlayerService(
                Intent(context, PlayerService::class.java)
                    .setAction(AlarmServiceActions.STOP)
                    .setPackage(context.packageName)
            )
        }.onFailure {
            Timber.e("AlarmReceiver STOP: invio stop al servizio fallito: ${it.message}")
        }
    }
}
