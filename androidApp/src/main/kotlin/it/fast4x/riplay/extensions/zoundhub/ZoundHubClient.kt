package it.fast4x.riplay.extensions.zoundhub

import it.fast4x.environment.utils.ProxyPreferences as EnvironmentProxyPreferences
import it.fast4x.riplay.data.ZoundHubDecisionPersistenceRoom
import it.fast4x.riplay.utils.appContext
import it.fast4x.zoundhub.ZoundHubCatalogState
import it.fast4x.zoundhub.ZoundHubCatalogStore
import it.fast4x.zoundhub.ZoundHubService
import it.fast4x.zoundhub.utils.ProxyPreferences
import it.fast4x.zoundhub.utils.ProxyPreferenceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Punto di accesso del modulo :zoundhub lato Android.
 *
 * Il modulo :zoundhub e' un modulo kotlin("jvm") puro (ktor/okhttp) gia'
 * incluso nelle dipendenze commonMain di composeApp: funziona su desktop
 * senza modifiche (come :lastfm, vedi DesktopLastFm.kt). Questo object
 * fornisce solo il WIRING Android:
 *  - directory cache = filesDir/zoundhub (privata dell'app, no permessi);
 *  - sincronizzazione del proxy applicativo (impostato in MainActivity
 *    su EnvironmentProxyPreferences) verso il ProxyPreferences del modulo;
 *  - caricamento della cache a freddo (nessuna rete all'avvio).
 *
 * Il refresh del catalogo NON e' automatico: parte dall'impostazione
 * "Aggiorna catalogo ora" in DataSettings (scelta di progetto: catalogo
 * locale, aggiornamento consapevole e manuale).
 */
object ZoundHubClient {

    /**
     * Scope di avvio del catalogo: indipendente dal ciclo di vita dei
     * composable (la cache deve caricarsi una sola volta per processo).
     */
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val store: ZoundHubCatalogStore by lazy {
        syncProxy()
        val dir = File(appContext().filesDir, "zoundhub")
        ZoundHubCatalogStore(
            cacheDir = dir,
            service = ZoundHubService(),
            // v1.4: le decisioni del filtro AI sono persistite nel database
            // Room (tabella zoundhub_decisions, cache derivata taggata per
            // versione di catalogo) — al riavvio il filtro parte a caldo
            // anche su dispositivi vecchi, senza ripagare i miss fuzzy per
            // i testi autori gia' visti.
            decisionPersistence = ZoundHubDecisionPersistenceRoom.fromCurrentDatabase(),
        ).also {
            // FIX FREEZE (v1.1): la cache e' un JSON da ~3 MB con ~9.700 artisti:
            // parse + costruzione indice richiedono centinaia di ms. NON si usa
            // piu' runBlocking (bloccava il main thread alla prima composizione
            // della home): il caricamento parte in background e la UI, che
            // osserva store.state, mostra i badge appena il catalogo e' pronto.
            initScope.launch {
                it.loadFromCache()

                // Ponte MusicBrainz (Fase 2): ogni volta che il catalogo diventa
                // Ready (cache a freddo, primo download o refresh manuale) si
                // ri-marka la natura degli artisti della libreria. Il backfiller
                // gira una sola volta per versione di catalogo e su Dispatchers.IO.
                it.state.collect { state ->
                    if (state.status == ZoundHubCatalogState.Status.Ready) {
                        ZoundHubNatureBackfiller.runIfNeeded(it)
                    }
                }
            }
        }
    }

    /**
     * Copia il proxy applicativo (che MainActivity imposta su
     * it.fast4x.environment.utils.ProxyPreferences) nel modulo zoundhub.
     */
    fun syncProxy() {
        ProxyPreferences.preference = EnvironmentProxyPreferences.preference?.let {
            ProxyPreferenceItem(
                proxyHost = it.proxyHost,
                proxyPort = it.proxyPort,
                proxyMode = it.proxyMode
            )
        }
    }
}
