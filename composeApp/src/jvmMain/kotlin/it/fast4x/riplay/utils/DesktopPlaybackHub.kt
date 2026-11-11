package it.fast4x.riplay.utils

import it.fast4x.riplay.player.player.DesktopPlayerManager
import it.fast4x.riplay.data.models.Song
import player.PlayerController

/**
 * Ponte tra la composizione (ThreeColumnsApp) e i servizi GLOBALI
 * che vivono FUORI dall'albero Compose (tray icon, sleep timer).
 *
 * Il DesktopPlayerManager e' "remember" dentro ThreeColumnsApp e i comandi
 * di navigazione (avanti/indietro tra i brani) sono chiusure locali di
 * quella funzione: il tray e il timer però devono funzionare anche con la
 * finestra NASCOSTA (chiusa nel tray mentre la musica suona). All'avvio
 * ThreeColumnsApp registra qui i riferimenti (remember, una sola volta) e
 * da quel momento i servizi li richiamano a qualunque ora.
 *
 * Tutti i campi sono @Volatile: scritti una volta dalla composizione,
 * letti da thread AWT/coroutine. I lambda catturano oggetti di stato
 * remember (stabili per tutta la vita dell'app), quindi leggere l'istanza
 * registrata alla prima composizione equivale a leggere lo stato corrente.
 */
object DesktopPlaybackHub {

    /** Gestore dei due motori (KCEF/VLCJ) registrato da ThreeColumnsApp. */
    @Volatile
    var manager: DesktopPlayerManager? = null

    /**
     * Controller del motore attivo in questo momento (o null: nessun media
     * ancora caricato / hub non ancora registrato).
     */
    val activeController: PlayerController?
        get() = manager?.active

    /** Avanti nella coda/cronologia (stesso comando del pulsante AVANTI). */
    @Volatile
    var onNext: (() -> Unit)? = null

    /** Indietro nella cronologia (stesso comando del pulsante INDIETRO). */
    @Volatile
    var onPrevious: (() -> Unit)? = null

    /** Titolo del brano in riproduzione (per il tooltip del tray). */
    @Volatile
    var currentSongTitle: () -> String? = { null }

    @Volatile
    var currentSongThumbnail: () -> String? = { null }

    @Volatile
    var currentSong: () -> Song? = { null }
}
