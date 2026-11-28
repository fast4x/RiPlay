package it.fast4x.riplay.musicvault

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.utils.appContext
import timber.log.Timber
import java.util.UUID

/**
 * Client del plugin esterno "MusicVault" se installato.
 *
 * Responsibilita':
 *  1. RILEVAMENTO: trova sul dispositivo le app che dichiarano un Service
 *     esportato con intent-filter su VaultPluginContract.ACTION_ENQUEUE.
 *     Non esiste scelta manuale: vale il primo plugin compatibile rilevato
 *     (la configurazione vive nel plugin, non in RiPlay).
 *  2. DISPATCH: invia le richieste come startForegroundService esplicito
 *     quindi la chiamata e' consentita anche su Android 12+);
 *  3. CALLBACK: le risposte del plugin tornano a VaultPluginResultReceiver
 *     tramite broadcast espliciti verso EXTRA_CALLBACK_PACKAGE.
 */
object VaultPluginClient {

    /** Rappresenta un plugin compatibile installato sul dispositivo. */
    data class VaultPlugin(
        val packageName: String,
        val serviceClassName: String,
        val label: String
    )

    /** Esito del dispatch di una richiesta. */
    sealed class DispatchResult {
        data class Sent(val plugin: VaultPlugin) : DispatchResult()
        data class NoPlugin(val reason: String) : DispatchResult()
        data class Error(val reason: String) : DispatchResult()
    }

    private fun appSettings() =
        (appContext() as MainApplication).appSettingsManager.activeSettings.value

    /** TTL della cache di disponibilita': evita queryIntentServices ad ogni recomposition. */
    private const val AVAILABILITY_CACHE_TTL_MS = 5_000L

    @Volatile
    private var availabilityCache: Pair<Long, Boolean>? = null

    /**
     * Rileva i plugin installati che dichiarano ACTION_ENQUEUE.
     * Richiede la visibilita' dei package: il manifest full dichiara <queries>
     * sull'azione (e in piu' l'app usa QUERY_ALL_PACKAGES).
     */
    fun discoverPlugins(context: Context): List<VaultPlugin> {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val intent = Intent(VaultPluginContract.ACTION_ENQUEUE)

        val resolved: List<android.content.pm.ResolveInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                pm.queryIntentServices(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            else
                @Suppress("DEPRECATION")
                pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Timber.e("VaultPluginClient discoverPlugins error: ${e.message}")
            emptyList()
        }

        val plugins = resolved.mapNotNull { info ->
            val service = info.serviceInfo ?: return@mapNotNull null
            val label = runCatching {
                (service.applicationInfo?.loadLabel(pm) ?: service.loadLabel(pm)).toString()
            }.getOrNull().takeUnless { it.isNullOrBlank() } ?: service.packageName
            VaultPlugin(
                packageName = service.packageName,
                serviceClassName = service.name,
                label = label
            )
        }.distinctBy { it.packageName }

        Timber.d("VaultPluginClient discoverPlugins: ${plugins.map { it.packageName }}")
        return plugins
    }

    /**
     * true se esiste almeno un plugin compatibile installato.
     * Risultato in cache per [AVAILABILITY_CACHE_TTL_MS]: chiamabile anche da
     * composables hot (pulsante brano) senza costi ad ogni frame.
     */
    fun isPluginAvailable(context: Context): Boolean {
        val now = SystemClock.elapsedRealtime()
        availabilityCache?.let { (timestamp, value) ->
            if (now - timestamp < AVAILABILITY_CACHE_TTL_MS) return value
        }
        val available = discoverPlugins(context).isNotEmpty()
        availabilityCache = now to available
        return available
    }

    /** Invalida la cache di disponibilita' (es. dopo install/disinstallazione). */
    fun invalidateAvailabilityCache() {
        availabilityCache = null
    }

    /** Plugin attivo: il primo compatibile rilevato sul dispositivo. */
    fun resolvePlugin(context: Context): VaultPlugin? =
        discoverPlugins(context).firstOrNull()

    /**
     * Invia la richiesta al plugin.
     * Lo stato QUEUED viene scritto dal chiamante (MusicVaultService.enqueue);
     * l'evento STARTED del plugin fara' passare lo stato a DOWNLOADING.
     * Il formato (audio/video) NON viene inviato: e' configurazione del plugin.
     */
    fun dispatchDownload(
        context: Context,
        song: Song
    ): DispatchResult {
        val plugin = resolvePlugin(context)
            ?: return DispatchResult.NoPlugin(
                "Nessuna app MusicVault compatibile installata"
            )

        val settings = runCatching { appSettings() }.getOrNull()
        val savedVaultPath = settings?.musicVaultPath.orEmpty()

        // Cartella Vault risolta (same logic as the old internal worker)
        val targetDir = MusicVaultRepository.getOutputDir()
        val targetUri = MusicVaultRepository.getOutputUri()?.toString().orEmpty()

        // La cartella di default e' il filesDir PRIVATO di RiPlay: nessuna app
        // esterna puo' scriverla -> consegna in handoff (FileProvider).
        // Con una cartella scelta dall'utente (pubblica/SAF) si tenta il direct.
        val delivery =
            if (savedVaultPath.isBlank()) VaultPluginContract.DELIVERY_HANDOFF
            else VaultPluginContract.DELIVERY_DIRECT

        val intent = Intent(VaultPluginContract.ACTION_ENQUEUE).apply {
            setClassName(plugin.packageName, plugin.serviceClassName)
            putExtra(VaultPluginContract.EXTRA_PROTOCOL_VERSION, VaultPluginContract.PROTOCOL_VERSION)
            putExtra(VaultPluginContract.EXTRA_REQUEST_ID, UUID.randomUUID().toString())
            putExtra(VaultPluginContract.EXTRA_SONG_ID, song.id)
            putExtra(VaultPluginContract.EXTRA_URL, "https://youtube.com/watch?v=${song.id}")
            putExtra(VaultPluginContract.EXTRA_TITLE, song.title.orEmpty())
            putExtra(VaultPluginContract.EXTRA_ARTIST, song.artistsText.orEmpty())
            putExtra(VaultPluginContract.EXTRA_TARGET_DIR, targetDir)
            putExtra(VaultPluginContract.EXTRA_TARGET_URI, targetUri)
            putExtra(VaultPluginContract.EXTRA_DELIVERY, delivery)
            putExtra(VaultPluginContract.EXTRA_CALLBACK_PACKAGE, context.packageName)
        }

        return try {
            context.startForegroundService(intent)
            Timber.i("VaultPluginClient dispatchDownload ${song.id} -> ${plugin.packageName} ($delivery)")
            DispatchResult.Sent(plugin)
        } catch (e: Exception) {
            Timber.e("VaultPluginClient dispatchDownload failed: ${e.message}")
            DispatchResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Richiede al plugin l'annullamento del lavoro sul brano.
     * Fire-and-forget: lo stato locale viene resettato subito dal chiamante.
     */
    fun dispatchCancel(context: Context, songId: String) {
        val plugin = resolvePlugin(context) ?: run {
            Timber.d("VaultPluginClient dispatchCancel: nessun plugin, skip")
            return
        }
        val intent = Intent(VaultPluginContract.ACTION_CANCEL).apply {
            setClassName(plugin.packageName, plugin.serviceClassName)
            putExtra(VaultPluginContract.EXTRA_PROTOCOL_VERSION, VaultPluginContract.PROTOCOL_VERSION)
            putExtra(VaultPluginContract.EXTRA_SONG_ID, songId)
            putExtra(VaultPluginContract.EXTRA_CALLBACK_PACKAGE, context.packageName)
        }
        try {
            context.startService(intent)
            Timber.i("VaultPluginClient dispatchCancel ${songId} -> ${plugin.packageName}")
        } catch (e: Exception) {
            // Se il processo del plugin non e' avviabile non c'e' nulla da annullare
            Timber.e("VaultPluginClient dispatchCancel failed: ${e.message}")
        }
    }
}
