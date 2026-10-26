package it.fast4x.riplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.QueuedSong
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * STEP 3m: pannello della CODA DI RIPRODUZIONE estratto dal player full
 * screen (step3i/3j/3k) e reso PUBBLICO: ora e' usato in DUE posti:
 *  - nel player full screen (DesktopPlayerOverlay) come prima;
 *  - nella NUOVA colonna all'estrema destra della finestra principale
 *    (ThreeColumnsApp): la coda e' accessibile SENZA aprire il player
 *    full screen, con le stesse identiche funzionalita'.
 *
 * Contenuto (invariato dagli step precedenti):
 *  - click su un brano = riproduci ora (esce dalla coda);
 *  - maniglia = TRASCINA per riordinare (sh.calvin.reorderable, la stessa
 *    libreria della coda Android; chiavi stabili QueuedSong.uid);
 *  - X = rimuovi dalla coda, cestino = svuota tutto;
 *  - X nell'intestazione = chiude il pannello ([onClose]).
 *
 * STEP 3m: nuovo parametro [topPadding]: nel player full screen vale
 * 70.dp (il pannello deve iniziare SOTTO la barra superiore per lasciare
 * cliccabile il pulsante "Coda", fix step3k n.5); nella colonna destra
 * della finestra principale vale 0.dp (non c'e' alcuna barra da schivare).
 */
@Composable
fun PlayQueuePanel(
    queue: List<QueuedSong>,
    onPlay: (Song) -> Unit,
    onRemove: (Song) -> Unit,
    onClear: () -> Unit,
    onMove: (Long, Long) -> Unit,
    onClose: () -> Unit,
    topPadding: Dp = 0.dp
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThreshold = 40.dp
    ) { from, to ->
        val fromUid = from.key as? Long
        val toUid = to.key as? Long
        if (fromUid != null && toUid != null) onMove(fromUid, toUid)
    }

    Column(
        Modifier
            .fillMaxHeight()
            .width(340.dp)
            // STEP 3k n.5 (player full): il pannello inizia SOTTO la barra
            // superiore; nella colonna destra (step3m) topPadding = 0.
            .padding(top = topPadding)
            .background(Color(0xF2101110))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.List,
                contentDescription = null,
                tint = RiPlayAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "Coda di riproduzione (${queue.size})",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            if (queue.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "svuota la coda",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // STEP 3k n.5: pulsante di CHIUSURA del pannello coda
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "chiudi il pannello coda",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "Trascina la maniglia per riordinare",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 11.sp
        )

        Spacer(Modifier.height(8.dp))

        if (queue.isEmpty()) {
            Text(
                "La coda \u00e8 vuota.\n\nAggiungi i brani con il pulsante + accanto al titolo nelle liste (Ricerca, Songs, album, artista, playlist), oppure premi Radio per i brani simili. Cliccando una canzone dentro un ALBUM entra in coda tutto l'album.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                items(queue, key = { it.uid }) { queued ->
                    ReorderableItem(
                        state = reorderableState,
                        key = queued.uid
                    ) { isDragging ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .background(
                                    if (isDragging) RiPlayAccent.copy(alpha = 0.12f)
                                    else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onPlay(queued.song) }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            // maniglia di trascinamento (riordino)
                            Icon(
                                Icons.Filled.DragHandle,
                                contentDescription = "trascina per riordinare",
                                tint = Color.White.copy(alpha = if (isDragging) 0.9f else 0.35f),
                                modifier = Modifier
                                    .size(22.dp)
                                    .draggableHandle()
                            )
                            Spacer(Modifier.width(6.dp))
                            AsyncImage(
                                model = queued.song.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    queued.song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                queued.song.artistsText?.let {
                                    Text(
                                        it,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onRemove(queued.song) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "rimuovi dalla coda",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
