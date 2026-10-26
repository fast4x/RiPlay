package it.fast4x.riplay.ui.components

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import extension.formatTimestamp
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.utils.LoadImage
import org.jetbrains.compose.resources.painterResource
import player.PlayerController
import player.PlayerState
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.chevron_up

/**
 * Barra "in riproduzione" sotto la finestra (sopra la barra di navigazione).
 *
 * STEP 3: sempre agganciata al motore ATTIVO (YouTube KCEF o locale VLCJ):
 * mostra il brano corrente e i comandi play/pausa + tempo dello stesso
 * controller usato dall'area video.
 *
 * STEP 3c (il "posto cliccabile" per i controlli completi, come il mini
 * player Android): l'INTERA barra e' cliccabile e apre il player espandibile
 * (DesktopPlayerOverlay) con timeline/seek, volume e toggle video/cover;
 * in piu' mostra la LINEA DI AVANZAMENTO del brano e il chevron di espansione.
 */
@Composable
fun MiniPlayer(
    song: Song?,
    controller: PlayerController?,
    onExpandAction: () -> Unit
){

    // Stato del motore ATTIVO: stesso StateFlow consumato dall'area video
    val state = if (controller != null) {
        val s by controller.state.collectAsState()
        s
    } else PlayerState()

    Column (verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        HorizontalDivider(
            color = Color.DarkGray,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth().alpha(0.6f)
        )
        // STEP 3c: linea di avanzamento del brano sulla barra (se la durata
        // e' nota): rende visibile a colpo d'occhio dove si e' il brano
        if (state.duration > 0) {
            LinearProgressIndicator(
                progress = (
                    (state.timestamp.toFloat() / state.duration.toFloat())
                        .coerceIn(0f, 1f)
                    ),
                color = Color.Green.copy(alpha = 0.6f),
                backgroundColor = Color.Black,
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
        }
        Row(
            Modifier
                .background(Color.Black)
                .fillMaxWidth()
                // STEP 3c: tutta la barra apre il player espandibile (i
                // IconButton interni gestiscono i propri click e restano
                // funzionanti come prima)
                .clickable { onExpandAction() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

                SongItem(
                    thumbnailContent = {
                        song?.thumbnailUrl?.let {
                            LoadImage(it)
                        }
                    },
                    authors = song?.artistsText,
                    duration = song?.durationText,
                    title = song?.title,
                    thumbnailSizeDp = 80.dp,
                    modifier = Modifier.fillMaxWidth(0.2f)
                )
            }

            IconButton(
                onClick = {
                    if (state.isPlaying) controller?.pause() else controller?.play()
                },
                enabled = controller != null
            ) {
                if (state.isPlaying) {
                    Icon(
                        Icons.Filled.Pause, ds("player.pause"),
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.PlayArrow, ds("player.play"),
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                "${state.timestamp.formatTimestamp()} / ${state.duration.formatTimestamp()}",
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(Modifier.weight(1f))

            // STEP 3c: indicatore di espansione (il player espandibile mostra
            // cover grande, timeline, volume e toggle video/cover)
            Image(
                painter = painterResource(Res.drawable.chevron_up),
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f)),
                contentDescription = ds("player.expand"),
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .size(18.dp)
            )
        }
    }

}
