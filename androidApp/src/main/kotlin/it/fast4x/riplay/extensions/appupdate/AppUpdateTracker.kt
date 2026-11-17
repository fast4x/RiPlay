package it.fast4x.riplay.extensions.appupdate

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.annotation.OptIn
import androidx.core.content.pm.PackageInfoCompat
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.AppUpdateKind
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.classifyVersionChange
import it.fast4x.riplay.installedVersionCode
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.utils.isAtLeastAndroid11
import timber.log.Timber
import androidx.core.content.edit

/**
 * APP UPDATE (RiPlay) — tracciamento dell'aggiornamento dell'app e
 * garanzia di riavvio del PlayerService.
 *
 * Cosa succede a un aggiornamento (da Play Store, GitHub APK, adb):
 * 1) Android termina il processo dell'app (tutti i servizi, anche FGS, vengono
 *    fermati: il "vecchio" PlayerService NON può sopravvivere all'update);
 * 2) l'app entra in stopped state finché l'utente non la riapre;
 * 3) il sistema invia ACTION_MY_PACKAGE_REPLACED alla sola app aggiornata:
 *    lo riceve AppUpdatedReceiver.
 *
 * Dato che ogni codice in esecuzione dopo l'update è per costruzione quello
 * del nuovo APK, gli obiettivi reali sono:
 *  - RILEVARE l'update: confronto lastSeenVersionCode <-> installedVersionCode
 *    (funzione KMP condivisa classifyVersionChange in composeApp);
 *  - FAR RIPARTIRE il PlayerService: tentativo immediato dal receiver
 *    (MY_PACKAGE_REPLACED è tra le esenzioni ufficiali degli avvii FGS da
 *    background su Android 12+, cfr. doc "Restrictions on starting a
 *    foreground service from the background"); se una ROM lo rifiuta, il flag
 *    pendente viene consumato da MainActivity.onStart con l'app in foreground
 *    (nessuna restrizione);
 *  - VERIFICARE la versione in esecuzione: sanity check BuildConfig.VERSION_CODE
 *    vs PackageInfo installato + log di auditoria (lastUpdateTime, installer).
 *
 * Lo stato è su un file SharedPreferences DEDICATO e NON sul file
 * "preferences" standard (che cleanUpUnusedPreferences svuota a ogni avvio
 * rimuovendo tutte le chiavi non presenti in PreferenceKey.allKeys) e non sul
 * DB: il receiver deve poter persistere il flag PRIMA di qualunque
 * inizializzazione del database.
 */
object AppUpdateTracker {

    /**
     * Azione esplicita con cui receiver/Activity avviano il PlayerService
     * dopo un aggiornamento. Il servizio la usa per soddisfare il contratto
     * startForegroundService() e per tracciare il riavvio post-update.
     */
    const val ACTION_APP_UPDATED = "it.fast4x.riplay.action.APP_UPDATED"

    private const val PREFS_FILE = "app_update_state"

    private const val KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode"
    private const val KEY_LAST_UPDATE_FROM_VERSION_CODE = "lastUpdateFromVersionCode"
    private const val KEY_LAST_UPDATE_TIME = "lastAppUpdateTime"
    private const val KEY_PENDING_SERVICE_RESTART = "pendingServiceRestartAfterUpdate"
    private const val KEY_LAST_SERVICE_RESTART_VERSION_CODE = "lastServiceRestartAfterUpdateVersionCode"
    private const val KEY_LAST_SERVICE_RESTART_TIME = "lastServiceRestartAfterUpdateTime"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    /** Informazioni complete sulla versione attualmente installata. */
    data class InstalledAppVersion(
        val versionCode: Long,
        val versionName: String?,
        val lastUpdateTime: Long,
        val installerPackage: String?
    )

    /** Esito del rilevamento a avvio del processo. */
    data class AppUpdateReport(
        val kind: AppUpdateKind,
        val installed: InstalledAppVersion,
        val previousVersionCode: Long
    )

    /**
     * Legge la versione attualmente installata dal PackageManager.
     * Restituisce null solo in caso di errore grave (pacchetto non trovato).
     */
    fun getInstalledVersion(context: Context): InstalledAppVersion? = runCatching {
        val pm = context.packageManager
        val pi: PackageInfo = pm.getPackageInfo(context.packageName, 0)

        // getInstallSourceInfo richiede API 30+; sotto si usa il metodo
        // deprecato ma equivalente getInstallerPackageName
        val installer = if (isAtLeastAndroid11)
            runCatching {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            }.getOrNull()
        else
            @Suppress("DEPRECATION")
            runCatching {
                pm.getInstallerPackageName(context.packageName)
            }.getOrNull()

        InstalledAppVersion(
            versionCode = PackageInfoCompat.getLongVersionCode(pi),
            versionName = pi.versionName,
            lastUpdateTime = pi.lastUpdateTime,
            installerPackage = installer
        )
    }.onFailure {
        Timber.e("AppUpdateTracker: lettura versione installata fallita: ${it.message}")
    }.getOrNull()

    /**
     * Da chiamare in MainApplication.onCreate: rileva se il processo sta
     * partendo dopo l'applicazione di un aggiornamento e persiste la nuova
     * versione vista. Sincrono e leggero (una lettura SharedPreferences +
     * una PackageManager), non richiede il database.
     */
    fun onProcessStart(context: Context): AppUpdateReport? {
        val installed = getInstalledVersion(context) ?: return null

        val p = prefs(context)
        val lastSeen = p.getLong(KEY_LAST_SEEN_VERSION_CODE, 0L)
        val kind = classifyVersionChange(installed.versionCode, lastSeen)
        val report = AppUpdateReport(kind, installed, lastSeen)

        when (kind) {
            AppUpdateKind.UPDATE -> {
                val previousUpdateTime = p.getLong(KEY_LAST_UPDATE_TIME, 0L)
                Timber.i(
                    "AppUpdateTracker: UPDATE applicato $lastSeen -> ${installed.versionCode} " +
                        "(versionName=${installed.versionName}, lastUpdateTime=${installed.lastUpdateTime}, " +
                        "updatePrecedente=$previousUpdateTime, installer=${installed.installerPackage})"
                )
                p.edit()
                    .putLong(KEY_LAST_UPDATE_FROM_VERSION_CODE, lastSeen)
                    .putLong(KEY_LAST_UPDATE_TIME, installed.lastUpdateTime)
                    .putLong(KEY_LAST_SEEN_VERSION_CODE, installed.versionCode)
                    .apply()

                // ── Hook per future migrazioni post-aggiornamento ──────────
                // Qui (o meglio, in un punto con il database già inizializzato,
                // es. dopo appSettingsManager.initialize()) si possono aggiungere
                // migrazioni dati legate a versioni specifiche, es.:
                //   if (lastSeen < 111) { /* migrare impostazioni da v110 */ }
            }

            AppUpdateKind.FRESH_INSTALL -> {
                Timber.i(
                    "AppUpdateTracker: prima installazione (versionCode=${installed.versionCode}, " +
                        "versionName=${installed.versionName}, installer=${installed.installerPackage})"
                )
                p.edit { putLong(KEY_LAST_SEEN_VERSION_CODE, installed.versionCode) }
            }

            AppUpdateKind.DOWNGRADE -> {
                // Sideload di una versione inferiore: si traccia comunque,
                // così il prossimo avvio non appare come update
                Timber.w(
                    "AppUpdateTracker: DOWNGRADE ${lastSeen} -> ${installed.versionCode} " +
                        "(sideload di una versione inferiore?), stato ri-allineato"
                )
                p.edit { putLong(KEY_LAST_SEEN_VERSION_CODE, installed.versionCode) }
            }

            AppUpdateKind.UNCHANGED -> {
                Timber.d(
                    "AppUpdateTracker: avvio normale (versionCode=${installed.versionCode})"
                )
            }
        }

        return report
    }

    // ─────────────────────────────────────────────────────────────────────
    // Riavvio del PlayerService post-aggiornamento
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Marca il riavvio del servizio come pendente. Chiamato dal receiver
     * PRIMA di qualunque tentativo di avvio: se l'avvio immediato fallisce
     * il flag resta e viene consumato al prossimo MainActivity.onStart.
     */
    fun requestServiceRestart(context: Context) {
        prefs(context).edit { putBoolean(KEY_PENDING_SERVICE_RESTART, true) }
    }

    fun isServiceRestartPending(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PENDING_SERVICE_RESTART, false)

    /** Intent dedicato per avviare il PlayerService dopo un aggiornamento. */
    @OptIn(UnstableApi::class)
    fun buildServiceRestartIntent(context: Context): Intent =
        Intent(context, PlayerService::class.java)
            .setAction(ACTION_APP_UPDATED)
            .setPackage(context.packageName)

    /**
     * Chiamato dal PlayerService quando gestisce l'azione APP_UPDATED:
     * consuma il flag pendente e traccia "quando/quale versione ha garantito
     * il riavvio post-update". È l'UNICO punto in cui il flag viene consumato:
     * così, se il servizio crasha prima di gestire l'azione, il flag resta e
     * il prossimo MainActivity.onStart riprova (riavvio veramente garantito).
     * L'utente può verificare nei log che il servizio in esecuzione è quello
     * nuovo anche quando l'avvio immediato dal receiver non era possibile.
     */
    fun onServiceRestartedAfterUpdate(context: Context) {
        val versionCode = installedVersionCode()
        prefs(context).edit {
            putBoolean(KEY_PENDING_SERVICE_RESTART, false)
                .putLong(KEY_LAST_SERVICE_RESTART_VERSION_CODE, versionCode)
                .putLong(KEY_LAST_SERVICE_RESTART_TIME, System.currentTimeMillis())
        }
        Timber.i(
            "AppUpdateTracker: PlayerService riavviato dopo update (versionCode=$versionCode)"
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // Verifica "il servizio in esecuzione è l'ultima versione installata?"
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Verifica che la versione del codice in esecuzione coincida con quella
     * dell'APK installato e registra nei log le informazioni d'auditoria.
     * Restituisce true se tutto coincide.
     *
     * Nota: dopo un update Android termina SEMPRE il processo, quindi i due
     * valori coincidono per costruzione; il check serve come sanity check
     * (es. bug nel packaging, MultiAPK con versionCode diversi, ecc.).
     */
    fun verifyRunningVersion(context: Context): Boolean {
        val installed = getInstalledVersion(context)
        val running = BuildConfig.VERSION_CODE.toLong()

        val pending = isServiceRestartPending(context)
        val lastRestartVersion = prefs(context)
            .getLong(KEY_LAST_SERVICE_RESTART_VERSION_CODE, 0L)

        val match = installed == null || installed.versionCode == running
        val message = buildString {
            append("AppUpdateTracker verifica versione: running(BuildConfig)=$running")
            append(", installed(PackageManager)=${installed?.versionCode}")
            append(", versionName=${installed?.versionName}")
            append(", corrispondenti=$match")
            append(", riavvioPostUpdatePendente=$pending")
            if (lastRestartVersion > 0L) {
                append(", ultimoRiavvioPostUpdatePerVersione=$lastRestartVersion")
            }
            if (installed != null) {
                append(", installer=${installed.installerPackage}")
                append(", lastUpdateTime=${installed.lastUpdateTime}")
            }
        }
        if (match) Timber.i(message)
        else Timber.w(message)

        if (!match) {
            Timber.e(
                "AppUpdateTracker: VERSIONE IN ESECUZIONE ($running) DIVERSA DA QUELLA " +
                    "INSTALLATA (${installed.versionCode}) — verificare packaging/versionCode"
            )
        }
        return match
    }
}
