package it.fast4x.riplay.ui

import java.io.File

/**
 * STEP 3o (richiesta utente n.4 "rendere la coda persistente in modo che
 * all'apertura dell'app si possa continuare con le canzoni in coda
 * precedenti"): SALVA la coda di riproduzione su disco.
 *
 * Il file e' ~/.riplay/queue.properties (stessa cartella della
 * persistenza della finestra): un semplice elenco di ID brano
 * (queue.0, queue.1, ...). Gli id sono sufficienti perche' OGNI brano
 * aggiunto alla coda viene anche salvato nel db (upsert in onAddToQueue/
 * onPlayCollection/onAddRadio): al riavvio l'app rilegge gli id e
 * ricarica le Song complete dal database.
 *
 * Formato a proprieta' indicizzate invece di una riga unica separata da
 * virgole: gli id dei brani LOCALI sono percorsi di file (possono
 * contenere qualunque carattere tranne l'a-capo) e Properties si occupa
 * dell'escaping. Un solo id per riga: nessun problema di separatori.
 *
 * Il salvataggio avviene a ogni modifica della coda (debounce nella
 * ThreeColumnsApp); il caricamento solo all'avvio. Ogni errore e'
 * silenzioso: la persistenza non deve mai impedire l'uso dell'app.
 */

private val queuePrefsFile: File
    get() = File(System.getProperty("user.home"), ".riplay").resolve("queue.properties")

/** Limite di sicurezza sul numero di id salvati (come la coda: 50). */
private const val QUEUE_PERSIST_MAX = 200

/** ID brano in coda salvati sul disco (ordinati dall'attesa piu' vecchia). */
fun loadSavedQueueIds(): List<String> = try {
    val p = java.util.Properties()
    if (queuePrefsFile.isFile) {
        queuePrefsFile.inputStream().use { p.load(it) }
    }
    (0 until QUEUE_PERSIST_MAX)
        .mapNotNull { idx -> p.getProperty("queue.$idx")?.takeIf { it.isNotBlank() } }
} catch (t: Throwable) {
    emptyList()
}

/**
 * Salvataggio della coda (lista di id nell'ordine di attesa).
 * Ignora ogni errore: mai bloccare l'app per la persistenza.
 */
fun saveQueueIds(ids: List<String>) {
    try {
        val p = java.util.Properties()
        ids.take(QUEUE_PERSIST_MAX).forEachIndexed { idx, id ->
            p.setProperty("queue.$idx", id)
        }
        queuePrefsFile.parentFile?.mkdirs()
        queuePrefsFile.outputStream().use { p.store(it, "RiPlay desktop playback queue") }
    } catch (t: Throwable) {
        // silenzioso
    }
}
