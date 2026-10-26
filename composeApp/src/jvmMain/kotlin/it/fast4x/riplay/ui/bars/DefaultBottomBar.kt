package it.fast4x.riplay.ui.bars

import androidx.compose.ui.text.style.TextOverflow

import it.fast4x.riplay.i18n.ds

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import extension.formatTimestamp
import it.fast4x.riplay.ui.components.PremiumSlider
import it.fast4x.riplay.ui.components.RiPlayAccent
import player.PlayerController
import kotlin.math.roundToLong

/**
 * Barra di controllo del pannello player di SINISTRA in home (sotto il
 * browser KCEF o il frame VLCJ).
 *
 * STEP 3k (8 sistemazioni richieste dall'utente dopo la conferma v28):
 *  - n.1: SWITCH AUDIO/VIDEO anche qui (prima esisteva solo nel player
 *    full, dove da oggi e' RIMOSSO: per guardare il video si deve tornare
 *    alla home). Icona videocamera = mostra il video, icona immagine =
 *    torna alla cover. La scelta persiste nel db (Song.isAudioOnly,
 *    semantica Android) tramite onToggleVideo.
 *  - n.2: pulsante ESPANDI (OpenInFull): apre il player full screen
 *    ANCHE quando la cover non e' visibile perche' sta suonando un video
 *    (prima l'unico ingresso al full screen era il click sulla cover).
 *  - n.4: pulsante RADIO come nel player full (pill con icona e feedback
 *    "Aggiunti N brani simili"): aggiunge i brani simili in coda.
 *  - n.6: il VOLUME ha una RIGA TUTTA SUA in fondo (prima stava nella
 *    stessa riga della timeline e nella colonna stretta risultava troppo
 *    compresso: slider a larghezza piena su riga dedicata).
 *
 * STEP 3l (richiesta utente: video espandibile a tutta larghezza della
 * finestra dell'app come fosse un fullscreen, con ritorno alla larghezza
 * iniziale): nuovo pulsante FULLSCREEN tra la pill Radio ed ESPANDI.
 * Visibile solo quando c'e' un VIDEO in visualizzazione (o quando il
 * video e' GIA' espanso); icona fullscreen = espande il pannello player
 * su tutta la larghezza dell'app (modalita' teatro), icona
 * fullscreen-exit = TORNA alla larghezza iniziale. Il pulsante ESPANDI
 * (player full screen con coda/radio/controlli) resta sempre presente.
 *
 * Il motore di riproduzione NON e' toccato: questa barra legge solo lo
 * [PlayerState] del controller attivo (KCEF o VLCJ).
 *
 * STEP 3o (richiesta utente n.5 "aggiungere i controlli avanti/indietro
 * nel miniplayer della home ... prova a riorganizzare al meglio i vari
 * controlli e pulsanti gia' presenti"): la barra e' RIORGANIZZATA:
 *  - RIGA 1 (trasporto): INDIETRO (SkipPrevious), play/pausa, AVANTI
 *    (SkipNext) a sinistra — la stessa navigazione di sessione/coda del
 *    player full (onPrevious/onNext con i grigi quando non disponibili);
 *    a destra restano le azioni display: switch audio/video, fullscreen
 *    (teatro) ed espandi player full. La colonna e' stretta: e' l'unica
 *    divisione che tiene insieme tutto senza compressioni.
 *  - RIGA 2: la pill RADIO da sola e CENTRATA (prima era schiacciata a
 *    destra della riga transport: col testo di feedback esce dallo
 *    spazio; da sola ha tutta la larghezza che le serve).
 *  - RIGHE 3-5 (invariate): timeline, timestamp, volume.
 *  - onPrevious/onNext NULLI = pulsanti nascosti (comportamento v32
 *    per chi usa la barra senza passare la navigazione).
 */
@Composable
fun DefaultBottomBar(
    modifier: Modifier = Modifier,
    controller: PlayerController,
    // STEP 3k n.1: stato display corrente (true = video visibile, false = cover)
    videoVisible: Boolean = true,
    // STEP 3k n.1: switch audio/video (null = pulsante nascosto)
    onToggleVideo: (() -> Unit)? = null,
    // STEP 3k n.4: radio brani simili (null = pulsante nascosto)
    onAddRadio: (() -> Unit)? = null,
    radioLoading: Boolean = false,
    radioFeedback: String? = null,
    // STEP 3k n.2: apri il player full screen (null = pulsante nascosto)
    onExpand: (() -> Unit)? = null,
    // STEP 3l: TRUE = video GIA' espanso a tutta larghezza finestra
    // (modalita' teatro): il pulsante fullscreen diventa fullscreen-exit
    widened: Boolean = false,
    // STEP 3l: espande/ripristina la larghezza del pannello player
    // (null = pulsante nascosto)
    onToggleWiden: (() -> Unit)? = null,
    // STEP 3o (richiesta utente n.5): AVANTI/INDIETRO nel miniplayer della
    // home (stessa navigazione del player full: cronologia + coda).
    // Null = pulsante nascosto (parametro facoltativo per compatibilita').
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    canGoPrevious: Boolean = false,
    canGoNext: Boolean = false,
    // STEP 3q (richiesta utente: "aggiungere nel miniplayer il titolo della
    // canzone ed il nome dell'artista con font non molto grande"): RIGA META
    // in testa alla barra. Null = riga nascosta (compatibilita' con
    // FramePlayer che non la passa).
    title: String? = null,
    artist: String? = null
) {

    val state by controller.state.collectAsState()

    val animatedTimestamp by animateFloatAsState(state.timestamp.toFloat())

    Column(
        modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // STEP 3q: RIGA META (titolo 13sp bianco, artista 11sp attenuato,
        // una riga ciascuno con ellissi) sopra i controlli. Nascosta quando
        // non c'e' nulla da mostrare (nessun brano / chiamate legacy).
        if (!title.isNullOrBlank() || !artist.isNullOrBlank()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!artist.isNullOrBlank()) {
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Riga 1 (STEP 3o): trasporto + azioni display.
        // Sinistra: INDIETRO - play/pausa - AVANTI (cronologia di sessione,
        // stessa funzione del player full; grigi quando non disponibili).
        // Destra: switch audio/video, fullscreen (teatro), espandi full.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // STEP 3o: brano precedente (nascosto se nessuna navigazione
            // e' stata passata: compatibilita' con il comportamento v32)
            if (onPrevious != null) {
                IconButton(onClick = onPrevious, enabled = canGoPrevious) {
                    Icon(
                        Icons.Filled.SkipPrevious, ds("player.previous"),
                        tint = Color.White.copy(alpha = if (canGoPrevious) 0.9f else 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (state.isPlaying) {
                IconButton(controller::pause) {
                    Icon(
                        Icons.Filled.Pause, ds("player.pause"),
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                IconButton(controller::play) {
                    Icon(
                        Icons.Rounded.PlayArrow, ds("player.play"),
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // STEP 3o: brano successivo (cronologia esaurita = parte la coda)
            if (onNext != null) {
                IconButton(onClick = onNext, enabled = canGoNext) {
                    Icon(
                        Icons.Filled.SkipNext, ds("player.next"),
                        tint = Color.White.copy(alpha = if (canGoNext) 0.9f else 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // STEP 3k n.1: switch audio/video come nel player full
            // (videocamera = guarda il video, immagine = torna alla cover)
            if (onToggleVideo != null) {
                IconButton(onClick = onToggleVideo) {
                    Icon(
                        if (videoVisible) Icons.Filled.Image else Icons.Filled.Videocam,
                        contentDescription = if (videoVisible)
                            ds("player.back_to_cover")
                        else ds("player.watch_video"),
                        tint = RiPlayAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // STEP 3l: VIDEO A TUTTA LARGHEZZA FINESTRA (teatro). Con un
            // video in visualizzazione (o gia' espanso) il pulsante
            // fullscreen espande il pannello player a tutta la larghezza
            // dell'app e, una volta espanso, diventa fullscreen-exit per
            // TORNARE alla larghezza iniziale. Nascosto in modalita' cover
            // (non c'e' un video da espandere).
            if (onToggleWiden != null && (videoVisible || widened)) {
                IconButton(onClick = onToggleWiden) {
                    Icon(
                        if (widened) Icons.Filled.FullscreenExit
                        else Icons.Filled.Fullscreen,
                        contentDescription = if (widened)
                            ds("player.restore_video")
                        else
                            ds("player.expand_video"),
                        tint = RiPlayAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // STEP 3k n.2: espandi il player full screen (l'unico ingresso
            // quando la cover e' coperta dal video in riproduzione)
            if (onExpand != null) {
                IconButton(onClick = onExpand) {
                    Icon(
                        Icons.Filled.OpenInFull, ds("player.open_fullscreen"),
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // STEP 3o: RIGA 2 - la pill RADIO da sola e centrata (prima stava
        // schiacciata a destra della riga transport: col feedback testuale
        // esce dallo spazio della colonna stretta)
        if (onAddRadio != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { if (!radioLoading) onAddRadio() }
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    if (radioLoading) {
                        CircularProgressIndicator(
                            color = RiPlayAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Radio,
                            contentDescription = ds("player.radio"),
                            tint = RiPlayAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        radioFeedback ?: "Radio",
                        color = if (radioFeedback != null) RiPlayAccent
                                else Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Riga 2: timeline (slider premium con bolla del tempo) a LARGHEZZA PIENA
        PremiumSlider(
            value = animatedTimestamp,
            onValueChange = {},
            onValueChangeFinished = { controller.seekTo(it.roundToLong()) },
            valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
            showTimeBubble = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // Riga 3: timestamp corrente / durata
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.timestamp.formatTimestamp(),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                state.duration.formatTimestamp(),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // STEP 3k n.6: riga del VOLUME tutta sua in fondo (icona + slider a
        // larghezza piena: prima lo slider da 300.dp nella stessa riga della
        // timeline restava compresso nella colonna stretta del player)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isMuted || state.volume == 0f) IconButton(controller::toggleSound) {
                Icon(Icons.AutoMirrored.Rounded.VolumeOff, ds("player.volume_off"),
                    tint = Color.White,
                    modifier = Modifier.size(26.dp))
            }
            else {
                if (state.volume < .5f) IconButton(controller::toggleSound) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeDown, ds("player.volume_low"),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp))
                } else IconButton(controller::toggleSound) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, ds("player.volume_high"),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp))
                }
            }
            PremiumSlider(
                value = state.volume,
                onValueChange = controller::setVolume,
                modifier = Modifier.weight(1f).padding(end = 16.dp)
            )
        }

    }

}
