package it.fast4x.riplay.extensions.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * ALARM (Sveglia) — ri-programmazione degli allarmi dopo eventi di sistema.
 *
 * Le sveglie programmate con AlarmManager vengono cancellate al riavvio del
 * dispositivo e modificati da cambio ora/fuso: questo receiver le ripristina.
 */
class AlarmBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.i("AlarmBootReceiver rescheduling alarms after ${intent.action}")
                AlarmScheduler.rescheduleAll(context)
            }
        }
    }
}
