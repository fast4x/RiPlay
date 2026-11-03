package it.fast4x.riplay.utils

import player.DesktopPlayerManager
import player.PlayerController


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
}
