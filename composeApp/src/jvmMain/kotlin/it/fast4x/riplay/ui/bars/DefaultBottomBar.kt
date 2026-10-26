package it.fast4x.riplay.ui.bars

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
    onToggleWiden: (() -> Unit)? = null
) {

    val state by controller.state.collectAsState()

    val animatedTimestamp by animateFloatAsState(state.timestamp.toFloat())

    Column(
        modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Riga 1: play/pausa + switch audio/video ... radio + espandi full screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (state.isPlaying) {
                IconButton(controller::pause) {
                    Icon(
                        Icons.Filled.Pause, "pause media",
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                IconButton(controller::play) {
                    Icon(
                        Icons.Rounded.PlayArrow, "play media",
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // STEP 3k n.1: switch audio/video come nel player full
            // (videocamera = guarda il video, immagine = torna alla cover)
            if (onToggleVideo != null) {
                IconButton(onClick = onToggleVideo) {
                    Icon(
                        if (videoVisible) Icons.Filled.Image else Icons.Filled.Videocam,
                        contentDescription = if (videoVisible)
                            "torna alla cover del brano"
                        else "guarda il video",
                        tint = RiPlayAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // STEP 3k n.4: radio (brani simili in coda), pill compatta con
            // lo stesso stile del pulsante Radio del player full
            if (onAddRadio != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { if (!radioLoading) onAddRadio() }
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                            contentDescription = "radio: aggiungi brani simili in coda",
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
                            "riporta il video alla larghezza iniziale"
                        else
                            "espandi il video a tutta larghezza della finestra",
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
                        Icons.Filled.OpenInFull, "apri il player a schermo intero",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
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
                Icon(Icons.AutoMirrored.Rounded.VolumeOff, "volume off",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp))
            }
            else {
                if (state.volume < .5f) IconButton(controller::toggleSound) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeDown, "volume low",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp))
                } else IconButton(controller::toggleSound) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, "volume high",
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
