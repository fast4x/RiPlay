package it.fast4x.riplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.unit.dp
import extension.formatTimestamp
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.utils.LoadImage
import player.PlayerController
import player.PlayerState

/**
 * STEP 3: barra mini SEMPRE agganciata al motore ATTIVO (YouTube KCEF o
 * locale VLCJ): mostra il brano corrente e i comandi play/pausa + tempo
 * dello stesso controller usato dall'area video. Rimosso il vecchio
 * wiring di test (FramePlayer con URL fissa BigBuckBunny), che caricava
 * un video dimostrativo ad ogni avvio.
 */
@Composable
fun MiniPlayer(
    song: Song?,
    controller: PlayerController?,
    onExpandAction: () -> Unit
){

    Column (verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        HorizontalDivider(
            color = Color.DarkGray,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth().alpha(0.6f)
        )
        Row(
            Modifier.background(Color.Black).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /*
            ExpandIcon(
                onAction = onExpandAction
            )
             */
            Row(
                //Modifier.border(BorderStroke(1.dp, Color.Red)),
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

            // Stato del motore ATTIVO: stesso StateFlow consumato dall'area video
            val state = if (controller != null) {
                val s by controller.state.collectAsState()
                s
            } else PlayerState()

            IconButton(
                onClick = {
                    if (state.isPlaying) controller?.pause() else controller?.play()
                },
                enabled = controller != null
            ) {
                if (state.isPlaying) {
                    Icon(
                        Icons.Filled.Pause, "pause media",
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.PlayArrow, "play media",
                        tint = Color.Green.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                "${state.timestamp.formatTimestamp()} / ${state.duration.formatTimestamp()}",
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }

}
