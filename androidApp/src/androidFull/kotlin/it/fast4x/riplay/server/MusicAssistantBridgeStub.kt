/*
 * RiPlay + Music Assistant — STUB del bridge libreria per il flavor FULL.
 *
 * Il bridge Music Assistant (modulo `riplay-bridge`, API REST /api/v1) esiste
 * SOLO nella versione FOSS di RiPlay: e' la condizione accettata dalla
 * community di Music Assistant per l'ammissione del provider `riplay`. La
 * versione full non include il modulo (dipendenza `fossImplementation` in
 * androidApp/build.gradle.kts) e questo actual stub rende il codice di
 * `src/main` (MainApplication, AndroidRelay, GeneralSettings) completamente
 * inerte: nessun modulo montato, nessun toggle visibile, nessuna nuova
 * route — l'applicazione Ktor resta identica al solo relay di streaming.
 *
 * Il desktop (composeApp/jvmMain) non passa da qui: l'app desktop non ha
 * edizioni "full" ne' Music Vault e mantiene il wiring diretto
 * (DesktopBridge.kt + DesktopRelay.kt).
 */
package it.fast4x.riplay.server

import android.content.Context
import io.ktor.server.application.Application
import timber.log.Timber

/** Il bridge Music Assistant non esiste nella versione full. */
val isMusicAssistantBridgeSupported: Boolean = false

/** Stub full: nessun wiring da installare. */
fun installAndroidMusicAssistantBridge(context: Context, enabled: Boolean) {
    Timber.tag("RelayManager").i(
        "bridge Music Assistant non disponibile nella versione full (solo FOSS)"
    )
}

/** Stub full: nessun modulo Ktor da montare — applicazione identica al solo relay. */
fun androidMusicAssistantBridgeModule(): (Application.(token: String) -> Unit)? = null
