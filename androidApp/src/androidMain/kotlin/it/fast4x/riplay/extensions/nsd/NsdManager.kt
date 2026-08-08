package it.fast4x.riplay.extensions.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class NsdDiscoveryManager(private val context: Context) {

    private val nsdManager by lazy {
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    /**
     * Avvia il discovery mDNS e restituisce un Flow reattivo con la lista aggiornata
     * dei dispositivi compatibili trovati e risolti nella rete locale.
     */
    fun discoverServices(serviceType: String): Flow<List<NsdServiceInfo>> = callbackFlow {
        val discoveredServicesList = mutableListOf<NsdServiceInfo>()

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("NsdManager: Discovery iniziato per $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.d("NsdManager: Servizio trovato -> ${service.serviceName}. Avvio risoluzione...")

                // Chiamata diretta al resolver universale per evitare conflitti di Lint e Type Mismatch
                resolveServiceLegacy(service) { resolvedService ->
                    if (resolvedService != null) {
                        // Evitiamo duplicati nella lista locale se il servizio invia aggiornamenti multipli
                        discoveredServicesList.removeAll { it.serviceName == resolvedService.serviceName }
                        discoveredServicesList.add(resolvedService)

                        // Inviamo una copia immutabile della lista aggiornata al Flow
                        trySend(discoveredServicesList.toList())
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Timber.w("NsdManager: Servizio perso -> ${service.serviceName}")
                discoveredServicesList.removeAll { it.serviceName == service.serviceName }
                trySend(discoveredServicesList.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.i("NsdManager: Discovery fermato per $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("NsdManager: Impossibile avviare il discovery: $errorCode")
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("NsdManager: Impossibile fermare il discovery: $errorCode")
                close()
            }
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Timber.e("NsdManager: Errore durante il lancio di discoverServices: ${e.localizedMessage}")
            close()
        }

        // Interrompe il discovery quando il Flow viene cancellato dalla MainActivity
        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: IllegalArgumentException) {
                Timber.e("NsdManager: Errore controllato durante lo stop: ${e.localizedMessage}")
            }
        }
    }.flowOn(Dispatchers.IO) // Esecuzione sicura fuori dal thread principale

    /**
     * Resolver universale retrocompatibile (ottimizzato contro i leak di memoria)
     */
    @Suppress("DEPRECATION")
    private fun resolveServiceLegacy(service: NsdServiceInfo, onResolved: (NsdServiceInfo?) -> Unit) {
        try {
            nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Timber.e("NsdResolver: Risoluzione fallita per ${serviceInfo.serviceName}. Codice: $errorCode")
                    onResolved(null)
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    Timber.i("NsdResolver: Risoluzione completata per ${serviceInfo.serviceName}")
                    onResolved(serviceInfo)
                }
            })
        } catch (e: Exception) {
            Timber.e("NsdResolver: Eccezione durante la risoluzione: ${e.localizedMessage}")
            onResolved(null)
        }
    }
}

