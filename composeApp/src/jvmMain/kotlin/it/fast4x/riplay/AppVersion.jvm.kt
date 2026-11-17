package it.fast4x.riplay

/**
 * Desktop: non esiste un "versionCode di pacchetto" installato confrontabile
 * con Android. Si restituisce 0 (sconosciuto): la classificazione con
 * classifyVersionChange() causerà FRESH_INSTALL alla prima esecuzione e
 * UNCHANGED nelle successive, comportamento neutro su desktop dove il
 * tracciamento post-aggiornamento non è pertinente.
 */
actual fun installedVersionCode(): Long = 0L
