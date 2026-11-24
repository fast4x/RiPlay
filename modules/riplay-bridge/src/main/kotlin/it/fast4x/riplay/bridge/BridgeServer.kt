/*
 * riplay-bridge — Server standalone (sviluppo/test e uso desktop sperimentale).
 *
 * In produzione il modulo bridgeModule() viene montato NELLO STESSO Ktor
 * application del RelayServer (stessa porta, stesso token) tramite il
 * parametro extraModules di RelayServer — vedere androidApp-glue
 * (BridgeIntegration.kt) e composeApp jvmMain (DesktopBridge.kt).
 *
 * Questa classe serve solo quando si vuole eseguire il bridge DA SOLO, per
 * esempio per provare l'API con il provider Music Assistant senza avviare
 * l'app: installa IgnoreTrailingSlash (che in produzione fornisce relayEndpoints)
 * e poi bridgeModule.
 */
package it.fast4x.riplay.bridge

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.IgnoreTrailingSlash

class BridgeServer(
    private val catalog: BridgeCatalog,
    private val resolver: StreamUrlResolver = DirectUrlResolver,
    private val config: BridgeConfig,
    private val preferredPort: Int = DEFAULT_PORT,
) {

    private var embedded: EmbeddedServer<*, *>? = null

    @Volatile
    var actualPort: Int = preferredPort
        private set

    val isRunning: Boolean
        get() = embedded != null

    /**
     * Avvia il bridge sulla [preferredPort] o, se occupata, sulle successive
     * (fino a [PORT_ATTEMPTS]). Stessa strategia del RelayServer di RiPlay.
     */
    fun start(): Boolean {
        if (embedded != null) return true
        repeat(PORT_ATTEMPTS) { attempt ->
            val port = preferredPort + attempt
            try {
                val server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
                    // Stesso comportamento dell'applicazione completa del RelayServer:
                    // IgnoreTrailingSlash e' installato qui (in produzione proviene da
                    // relayEndpoints, quindi bridgeModule non lo reinstalla).
                    install(IgnoreTrailingSlash)
                    bridgeModule(catalog, resolver, config)
                }
                server.start(wait = false)
                embedded = server
                actualPort = port
                return true
            } catch (_: Throwable) {
                // porta occupata: prova la successiva
            }
        }
        return false
    }

    fun stop() {
        runCatching { embedded?.stop(500, 1_000) }
        embedded = null
    }

    companion object {
        /** Porta di default: la stessa del RelayServer di RiPlay. */
        const val DEFAULT_PORT = 8765
        const val PORT_ATTEMPTS = 10
    }
}
