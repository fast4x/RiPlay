package it.fast4x.riplay.musicvault

/**
 * Contratto del protocollo "MusicVault" (versione 1).
 *   1. dichiara un Service esportato con intent-filter su ACTION_ENQUEUE;
 *   2. RiPlay invia una richiesta e la cartella Vault;
 *   3. elabora la richiesta;
 *   4. notifica l'esito con broadcast espliciti verso EXTRA_CALLBACK_PACKAGE.
 *
 * Azioni RiPlay -> plugin (startForegroundService/startService esplicito):
 *   - ACTION_ENQUEUE: accoda ed esegue la richiesta
 *   - ACTION_CANCEL:  annulla un lavoro in coda o in esecuzione
 *
 * Azioni plugin -> RiPlay (sendBroadcast esplicito con setPackage(callback)):
 *   - ACTION_RESULT_STARTED:  il plugin ha iniziato l'elaborazione
 *   - ACTION_RESULT_PROGRESS: aggiornamento opzionale di progresso (0..100)
 *   - ACTION_RESULT_COMPLETED: elaborazione conclusa con successo
 *   - ACTION_RESULT_FAILED:   elaborazione fallita
 *
 * La specifica completa (extras, modalita' di consegna file) e' in
 * docs/musicvault-external-plugin.md e nel PROTOCOL.md dell'app plugin
 * di riferimento "MusicVault" (package com.musicvault.app).
 */
object VaultPluginContract {

    /** Versione del protocollo. Incrementa in caso di breaking change. */
    const val PROTOCOL_VERSION = 1

    /** Versione minima del protocollo accettata da RiPlay. */
    const val MIN_PROTOCOL_VERSION = 1

    // ---------------------------------------------------------------------
    // Azioni RiPlay -> plugin
    // ---------------------------------------------------------------------
    const val ACTION_ENQUEUE = "com.musicvault.action.ENQUEUE"
    const val ACTION_CANCEL  = "com.musicvault.action.CANCEL"

    // ---------------------------------------------------------------------
    // Azioni plugin -> RiPlay
    // ---------------------------------------------------------------------
    const val ACTION_RESULT_STARTED   = "com.musicvault.action.RESULT_STARTED"
    const val ACTION_RESULT_PROGRESS  = "com.musicvault.action.RESULT_PROGRESS"
    const val ACTION_RESULT_COMPLETED = "com.musicvault.action.RESULT_COMPLETED"
    const val ACTION_RESULT_FAILED    = "com.musicvault.action.RESULT_FAILED"

    // ---------------------------------------------------------------------
    // Extra comuni (richiesta e risposta)
    // ---------------------------------------------------------------------
    /** Versione del protocollo del mittente. */
    const val EXTRA_PROTOCOL_VERSION = "protocol_version"

    /** Identificativo univoco della richiesta (UUID), echo nelle risposte. */
    const val EXTRA_REQUEST_ID = "request_id"

    /** Id del brano/video (mediaId RiPlay, es. videoId YouTube). */
    const val EXTRA_SONG_ID = "song_id"

    /** Package dell'app chiamante: il plugin invia le risposte a questo package. */
    const val EXTRA_CALLBACK_PACKAGE = "callback_package"

    // ---------------------------------------------------------------------
    // Extra richiesta (RiPlay -> plugin, azione ENQUEUE)
    // ---------------------------------------------------------------------
    /** URL completo della pagina del contenuto (es. https://youtube.com/watch?v=ID). */
    const val EXTRA_URL = "url"

    /** Titolo noto a RiPlay (puo' essere vuoto: il plugin usa i metadati reali). */
    const val EXTRA_TITLE = "title"

    /** Artista/autore noto a RiPlay (puo' essere vuoto). */
    const val EXTRA_ARTIST = "artist"

    /**
     * Percorso assoluto della cartella Music Vault risolto da RiPlay.
     * In modalita' DELIVERY_HANDOFF e' puramente informativo.
     */
    const val EXTRA_TARGET_DIR = "target_dir"

    /**
     * URI content:// (SAF) della cartella Vault scelta dall'utente, se esiste.
     * Fornito anche come fallback informativo per il plugin.
     */
    const val EXTRA_TARGET_URI = "target_uri"

    /**
     * Modalita' di consegna del file:
     *  - DELIVERY_DIRECT: il plugin scrive direttamente in EXTRA_TARGET_DIR
     *    (cartella pubblica/SAF accessibile) e restituisce solo i nomi file;
     *  - DELIVERY_HANDOFF: la cartella Vault non e' accessibile da terze parti
     *    (es. filesDir privato di RiPlay): il plugin scarica nel proprio storage,
     *    restituisce EXTRA_FILE_URI / EXTRA_THUMBNAIL_URI (FileProvider) e RiPlay
     *    importa i file nella cartella Vault.
     * Il plugin puo' SEMPRE degradare a handoff se la scrittura diretta fallisce.
     */
    const val EXTRA_DELIVERY = "delivery"
    const val DELIVERY_DIRECT = "direct"
    const val DELIVERY_HANDOFF = "handoff"

    // ---------------------------------------------------------------------
    // Extra risposta (plugin -> RiPlay)
    // ---------------------------------------------------------------------
    /** Nome file audio/video prodotto (solo COMPLETED, modalita' direct). */
    const val EXTRA_FILE_NAME = "file_name"

    /** Nome file thumbnail prodotto (solo COMPLETED, puo' essere assente). */
    const val EXTRA_THUMBNAIL_FILE_NAME = "thumbnail_file_name"

    /** Uri content:// del file (solo COMPLETED, modalita' handoff). */
    const val EXTRA_FILE_URI = "file_uri"

    /** Uri content:// della thumbnail (solo COMPLETED, modalita' handoff). */
    const val EXTRA_THUMBNAIL_URI = "thumbnail_uri"

    /** Titolo reale estratto da yt-dlp (solo COMPLETED). */
    const val EXTRA_RESULT_TITLE = "result_title"

    /** Artista/uploader reale estratto da yt-dlp (solo COMPLETED). */
    const val EXTRA_RESULT_ARTIST = "result_artist"

    /** Durata in secondi (solo COMPLETED, 0 se sconosciuta). */
    const val EXTRA_DURATION_SECONDS = "duration_seconds"

    /** Percorso locale del file prodotto, solo informativo (solo COMPLETED). */
    const val EXTRA_FILE_PATH = "file_path"

    /** Messaggio di errore leggibile (solo FAILED). */
    const val EXTRA_ERROR_MESSAGE = "error_message"

    /** Percentuale di avanzamento 0..100 (solo PROGRESS, opzionale). */
    const val EXTRA_PROGRESS = "progress"
}
