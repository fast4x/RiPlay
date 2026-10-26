package it.fast4x.riplay.ui.pages

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.commonutils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongEntity
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.styling.Dimensions.layoutColumnBottomSpacer
import it.fast4x.riplay.ui.components.RiPlayAccent
import upload.LOCAL_AUDIO_EXTENSIONS
import upload.pickLocalAudioFiles
import upload.pickLocalFolder
import java.io.File

/**
 * STEP 3m (richiesta utente n.2 "manca la parte di caricamento dei files
 * locali presenti nel pc che poi vengono riprodotti con il motore vlcj"):
 * pagina dei FILE LOCALI del computer.
 *
 * Comportamento:
 *  - "Aggiungi cartella": picker nativo di una directory, scansione
 *    RICORSIVA dei file audio (mp3, m4a, flac, ogg, opus, wav, ...) e
 *    inserimento nel db come brani locali (id = "local:<percorso>",
 *    Song.folder = cartella, artistsText = nome della cartella);
 *  - "Aggiungi file": picker nativo MULTI-selezione di singoli file;
 *  - la lista elenca TUTTI i brani locali del db (ordine alfabetico per
 *    titolo) e si AGGIORNA DA SOLA (Flow) dopo ogni importazione;
 *  - click su un brano = parte col motore VLCJ (il routing automatico
 *    di DesktopPlayerManager invia gli id "local:..." a VLCJ) e ANCHE I
 *    RESTANTI brani della lista entrano in coda (STEP 3n, richiesta
 *    utente n.5, stesso comportamento degli album: la lista completa e'
 *    passata a onPlayCollection, che applica il cap di QUEUE_MAX_ITEMS);
 *  - "+" = aggiunge il SOLO brano alla coda di riproduzione.
 *
 * L'upsert e' IDEMPOTENTE (stesso percorso = stesso id): reimportare la
 * stessa cartella non crea duplicati, aggiorna solo le righe esistenti.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LocalSongsPage(
    // STEP 3n (richiesta utente n.5 "dalla scheda Locali se clicco su una
    // canzone le restanti non vengono aggiunte in coda"): il click passa
    // da onPlayCollection = brano cliccato parte subito + RESTO della
    // lista in coda (i brani locali sono gia' nel db: l'upsert dentro
    // onPlayCollection e' idempotente e non crea duplicati)
    onPlayCollection: (List<Song>, Song) -> Unit,
    onAddToQueue: (Song) -> Unit
) {

    val songs = remember { SnapshotStateList<Song>() }
    LaunchedEffect(Unit) {
        Database.getLocalSongs().collect {
            songs.clear()
            songs += it
        }
    }

    // richiesta di importazione: "folder" oppure "files" (un tick = una
    // apertura del picker, one-shot dentro LaunchedEffect come PlayerInput)
    var scanRequest by remember { mutableStateOf<Int?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun importSong(file: File): Song = Song(
        id = LOCAL_KEY_PREFIX + file.absolutePath,
        mediaId = file.absolutePath,
        title = file.nameWithoutExtension,
        // nome della cartella come contesto "autore" (il file non ha tag letti)
        artistsText = file.parentFile?.name?.takeIf { it.isNotBlank() } ?: ds("local.fallback_artist"),
        durationText = null,
        thumbnailUrl = null,
        // audio (no video): la COVER copre il player, come i brani YT
        isAudioOnly = 1,
        folder = file.parent
    )

    LaunchedEffect(scanRequest) {
        val request = scanRequest ?: return@LaunchedEffect
        feedback = null
        if (request == 1) {
            // CARTELLA: scansione ricorsiva
            val dirPath = pickLocalFolder() ?: return@LaunchedEffect
            scanning = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val dir = File(dirPath)
                val files = runCatching {
                    dir.walkTopDown()
                        .filter { it.isFile && it.extension.lowercase() in LOCAL_AUDIO_EXTENSIONS }
                        .toList()
                }.getOrDefault(emptyList())
                files.forEach { file ->
                    runCatching { Database.upsert(importSong(file)) }
                }
                files.size
            }.also { count ->
                scanning = false
                feedback = if (count == 0)
                    ds("local.none_found_in", "value" to dirPath)
                else
                    ds("local.imported_from", "n" to count, "value" to dirPath)
                println("[RiPlay-Locali] importati $count file da $dirPath")
            }
        } else {
            // FILE MULTIPLI
            val paths = pickLocalAudioFiles()
            if (paths.isEmpty()) return@LaunchedEffect
            scanning = true
            val added = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                paths.map { File(it) }
                    .filter { it.isFile }
                    .onEach { file ->
                        runCatching { Database.upsert(importSong(file)) }
                    }
                    .size
            }
            scanning = false
            feedback = ds("local.imported", "n" to added)
            println("[RiPlay-Locali] importati $added singoli file")
        }
    }

    val lazyListState = rememberLazyListState()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = ds("page.local", "n" to songs.size),
                style = TextStyle(
                    fontSize = typography.titleSmall.fontSize,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
            )
        }

        // Pulsanti di importazione + feedback
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth()
        ) {
            ImportPill(
                icon = { Icon(Icons.Filled.Folder, contentDescription = null, tint = RiPlayAccent, modifier = Modifier.size(17.dp)) },
                label = ds("local.add_folder"),
                enabled = !scanning
            ) { scanRequest = 1 }

            ImportPill(
                icon = { Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = RiPlayAccent, modifier = Modifier.size(17.dp)) },
                label = ds("local.add_files"),
                enabled = !scanning
            ) { scanRequest = 2 }

            if (scanning) {
                CircularProgressIndicator(
                    color = RiPlayAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    ds("local.scanning"),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        feedback?.let {
            Text(
                it,
                color = RiPlayAccent.copy(alpha = 0.9f),
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (songs.isEmpty() && !scanning) {
            Text(
                ds("local.empty_hint"),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 12.dp, end = 8.dp)
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                SongItem(
                    songEntity = SongEntity(song = song),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onLongClick = {},
                            // STEP 3n: il click mette in coda ANCHE i restanti
                            // brani della lista (cap 50 in onPlayCollection)
                            onClick = { onPlayCollection(songs.toList(), song) }
                        ),
                    onAddToQueue = { onAddToQueue(song) }
                )
            }

            item(key = "bottom") {
                Spacer(modifier = Modifier.height(layoutColumnBottomSpacer))
            }
        }
    }
}

/** Pill compatta dei due comandi di importazione (stile pill "Coda"/"Radio"). */
@Composable
private fun ImportPill(
    icon: @Composable () -> Unit,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(RiPlayAccent.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        icon()
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            color = if (enabled) RiPlayAccent else RiPlayAccent.copy(alpha = 0.4f),
            fontSize = 13.sp
        )
    }
}
