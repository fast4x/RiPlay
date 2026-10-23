package it.fast4x.riplay.data

/**
 * Accesso all'istanza singleton del database, risolto per piattaforma:
 * - androidMain: AndroidDbRegistry (builder con Context, registrato da androidApp)
 * - jvmMain: DesktopDbRegistry (builder con percorso dati utente)
 */
expect fun getDatabaseInitializer(): DatabaseInitializer

expect fun getDatabaseDao(): Database
