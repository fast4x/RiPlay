package it.fast4x.riplay

/**
 * APP UPDATE (RiPlay) — logica di versionamento condivisa KMP.
 *
 * Perché serve: quando l'app viene aggiornata Android termina il processo e
 * sostituisce l'APK. Per capire "è appena stata applicata una nuova versione?"
 * si confronta il versionCode attualmente installato con l'ultimo visto
 * dall'esecuzione precedente (persistito lato piattaforma).
 *
 * La classificazione è pura e condivisa (Android/desktop), mentre la lettura
 * del versionCode installato è per natura dipendente dalla piattaforma
 * (PackageManager su Android, nessun concetto equivalente su desktop).
 *
 * L'actual Android è registrato da androidApp in MainApplication.onCreate
 * tramite [AndroidAppVersionRegistry] (stesso pattern di AndroidDbRegistry
 * per il database KMP).
 */
expect fun installedVersionCode(): Long

/**
 * Tipo di transizione di versione rilevata all'avvio del processo.
 */
enum class AppUpdateKind {
    /** Prima installazione: nessuna versione precedente registrata. */
    FRESH_INSTALL,

    /** Versione installata maggiore dell'ultima vista: update appena applicato. */
    UPDATE,

    /** Versione installata minore dell'ultima vista (rollback/sideload). */
    DOWNGRADE,

    /** Nessuna variazione: avvio normale. */
    UNCHANGED
}

/**
 * Classifica pura (testabile, condivisa) del cambio di versione.
 *
 * Convenzione: [lastSeenVersionCode] <= 0 significa "mai registrato"
 * (prima installazione oppure prima esecuzione dopo l'aggiunta di questa
 * funzionalità a un'app già presente).
 */
fun classifyVersionChange(
    installedVersionCode: Long,
    lastSeenVersionCode: Long
): AppUpdateKind = when {
    lastSeenVersionCode <= 0L -> AppUpdateKind.FRESH_INSTALL
    installedVersionCode > lastSeenVersionCode -> AppUpdateKind.UPDATE
    installedVersionCode < lastSeenVersionCode -> AppUpdateKind.DOWNGRADE
    else -> AppUpdateKind.UNCHANGED
}
