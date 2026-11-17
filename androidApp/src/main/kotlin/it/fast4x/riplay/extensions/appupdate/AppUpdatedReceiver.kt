package it.fast4x.riplay.extensions.appupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.fast4x.riplay.utils.sendCommandToPlayerService
import timber.log.Timber

/**
 * APP UPDATE (RiPlay) — riceve ACTION_MY_PACKAGE_REPLACED al termine
 * dell'installazione di una nuova versione dell'app.
 *
 * Garanzie:
 *  - il broadcast arriva anche se l'app è in stopped state (dopo un update
 *    l'app è SEMPRE in stopped state finché l'utente non la riapre) e viene
 *    inviato SOLO al pacchetto aggiornato;
 *  - MY_PACKAGE_REPLACED è tra le esenzioni ufficiali per l'avvio di un
 *    foreground service da background (Android 12+), quindi il tentativo
 *    immediato di riavvio del PlayerService è generalmente consentito;
 *  - su Android 15+ la restrizione "BOOT_COMPLETED non può avviare FGS di
 *    tipo mediaPlayback" riguarda solo BOOT_COMPLETED, ma alcune ROM con
 *    battery manager aggressivi possono comunque rifiutare: in quel caso il
 *    flag pendente resta e il riavvio avviene al prossimo MainActivity.onStart
 *    (app in foreground, nessuna restrizione).
 *
 * Nota sul contratto FGS: se l'avvio con startForegroundService() riesce, il
 * PlayerService deve chiamare startForeground() entro ~5s. Lo fa gestendo
 * l'azione APP_UPDATED in onStartCommand (promozione immediata con
 * notifica placeholder, stesso meccanismo usato dal flusso sveglia).
 */
class AppUpdatedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // 1. Marca il riavvio come pendente PRIMA di qualunque tentativo:
        //    è l'operazione che non deve mai fallire (apply = write async su
        //    disco garantito anche se il processo viene interrotto poco dopo).
        AppUpdateTracker.requestServiceRestart(context)

        // 2. Rilevamento/versione nei log (utile a diagnosi anche lato utente
        //    con "log debug" attivo): il receiver gira nel nuovo processo con
        //    il nuovo APK, quindi MainApplication.onCreate registra già l'update
        //    tramite AppUpdateTracker.onProcessStart (l'ordine è Application ->
        //    receiver). Qui basta tracciare il tentativo di riavvio.
        Timber.i("AppUpdatedReceiver: ricevuto MY_PACKAGE_REPLACED, riavvio PlayerService richiesto")

        // 3. Tentativo di riavvio immediato del servizio (app chiusa).
        //    Stessa strada della sveglia: sendCommandToPlayerService usa
        //    startForegroundService su Android 8+, startService altrimenti.
        //    La marcatura del riavvio avvenuto NON è qui: è il PlayerService
        //    (unico punto autorevole) a tracciare il riavvio quando gestisce
        //    l'azione APP_UPDATED in onStartCommand, così il tracciamento
        //    funziona identico sia per l'avvio immediato sia per quello
        //    differito da MainActivity.
        runCatching {
            sendCommandToPlayerService(AppUpdateTracker.buildServiceRestartIntent(context))
            Timber.i("AppUpdatedReceiver: PlayerService avviato immediatamente dopo l'update")
        }.onFailure {
            Timber.w(
                "AppUpdatedReceiver: avvio immediato rifiutato dal sistema " +
                    "(${it.javaClass.simpleName}: ${it.message}); " +
                    "riavvio differito al prossimo MainActivity.onStart"
            )
        }
    }
}
