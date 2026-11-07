package it.fast4x.riplay.ui.screens.alarm

import android.text.format.DateFormat
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.fromMusicShelfRenderer
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.PlaylistPreview
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.alarm.AlarmItem
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.Switch
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.align
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.ui.styling.secondary
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getRoundnessShape
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.delay

// ------------------------------------------------------------------
// Helper
// ------------------------------------------------------------------

@Composable
fun getAlarmShape() = getRoundnessShape()

/**
 * Descrizione della ripetizione (per la riga dell'elenco sveglie).
 */
@OptIn(UnstableApi::class)
@Composable
fun AlarmItem.repeatDescription(): String {
    val context = LocalContext.current
    val dayNames = remember {
        (1..7).map { day ->
            val resId = when (day) {
                1 -> R.string.alarm_day_short_1
                2 -> R.string.alarm_day_short_2
                3 -> R.string.alarm_day_short_3
                4 -> R.string.alarm_day_short_4
                5 -> R.string.alarm_day_short_5
                6 -> R.string.alarm_day_short_6
                else -> R.string.alarm_day_short_7
            }
            context.resources.getString(resId)
        }
    }

    return when {
        repeatDays.isEmpty() -> context.resources.getString(R.string.alarm_once)
        repeatDays.size == 7 -> context.resources.getString(R.string.alarm_every_day)
        else -> repeatDays.sorted().mapNotNull { dayNames.getOrNull(it - 1) }
            .joinToString(", ")
    }
}

// ------------------------------------------------------------------
// Editor sveglia
// ------------------------------------------------------------------

@ExperimentalMaterial3Api
@Composable
fun AlarmEditorDialog(
    alarm: AlarmItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
) {
    val context = LocalContext.current

    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var repeatDays by remember { mutableStateOf(alarm.repeatDays) }
    var songId by remember { mutableStateOf(alarm.songId) }
    var playlistId by remember { mutableStateOf(alarm.playlistId) }
    var albumId by remember { mutableStateOf(alarm.albumId) }
    var songSnapshot by remember { mutableStateOf(alarm.songSnapshot) }
    var sourceName by remember { mutableStateOf(alarm.sourceName) }
    var shuffle by remember { mutableStateOf(alarm.shuffle) }

    var showSourcePicker by remember { mutableStateOf(false) }

    DefaultDialog(
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        BasicText(
            text = stringResource(if (isNew) R.string.alarm_add else R.string.alarm_edit),
            style = typography().m.semiBold,
        )

        Spacer(Modifier.height(8.dp))

        // Selettore orario (dial standard Material 3, tema colore dell'app)
        val timeState = rememberTimePickerState(
            initialHour = alarm.hour,
            initialMinute = alarm.minute,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        MaterialTheme(
            colorScheme = if (colorPalette().background1.luminance() < 0.5f)
                darkColorScheme(
                    primary = colorPalette().accent,
                    onPrimary = colorPalette().onAccent,
                    surface = colorPalette().background1,
                    onSurface = colorPalette().text,
                )
            else
                lightColorScheme(
                    primary = colorPalette().accent,
                    onPrimary = colorPalette().onAccent,
                    surface = colorPalette().background1,
                    onSurface = colorPalette().text,
                )
        ) {
            TimePicker(state = timeState)
        }

        Spacer(Modifier.height(8.dp))

        // Ripetizione: Una volta / Ogni giorno / Personalizzato
        val onceText = stringResource(R.string.alarm_once)
        val everyDayText = stringResource(R.string.alarm_every_day)
        val customText = stringResource(R.string.alarm_custom_days)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlarmChip(
                text = onceText,
                selected = repeatDays.isEmpty(),
                onClick = { repeatDays = emptySet() }
            )
            AlarmChip(
                text = everyDayText,
                selected = repeatDays.size == 7,
                onClick = { repeatDays = (1..7).toSet() }
            )
            AlarmChip(
                text = customText,
                selected = repeatDays.isNotEmpty() && repeatDays.size < 7,
                onClick = {
                    // Precompila con Lun-Ven se era vuoto/tutto
                    repeatDays = if (repeatDays.isNotEmpty() && repeatDays.size < 7)
                        repeatDays else (1..5).toSet()
                }
            )
        }

        if (repeatDays.isNotEmpty() && repeatDays.size < 7) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..7).forEach { day ->
                    val letter = when (day) {
                        1 -> stringResource(R.string.alarm_day_short_1)
                        2 -> stringResource(R.string.alarm_day_short_2)
                        3 -> stringResource(R.string.alarm_day_short_3)
                        4 -> stringResource(R.string.alarm_day_short_4)
                        5 -> stringResource(R.string.alarm_day_short_5)
                        6 -> stringResource(R.string.alarm_day_short_6)
                        else -> stringResource(R.string.alarm_day_short_7)
                    }.take(2)
                    AlarmChip(
                        text = letter,
                        selected = day in repeatDays,
                        onClick = {
                            repeatDays = if (day in repeatDays) repeatDays - day
                            else repeatDays + day
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Musica della sveglia
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(getAlarmShape())
                .clickable { showSourcePicker = true }
                .background(colorPalette().background2)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = { showSourcePicker = true },
                icon = when {
                    albumId != null -> R.drawable.album
                    playlistId != null -> R.drawable.playlist
                    else -> R.drawable.musical_notes
                },
                color = colorPalette().textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                BasicText(
                    text = stringResource(R.string.alarm_music),
                    style = typography().s.medium
                )
                BasicText(
                    text = sourceName.ifBlank { stringResource(R.string.alarm_no_music) },
                    style = typography().xxs.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { showSourcePicker = true },
                icon = R.drawable.chevron_forward,
                color = colorPalette().textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        // Shuffle (per playlist e album: sorgenti multi-brano)
        if (playlistId != null || albumId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.shuffle),
                    style = typography().s.medium,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(getAlarmShape())
                        .clickable { shuffle = !shuffle }
                        .padding(4.dp)
                ) {
                    Switch(isChecked = shuffle)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Footer: chiudi / salva
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onDismiss,
                icon = R.drawable.close,
                color = colorPalette().text,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = {
                    onSave(
                        AlarmItem(
                            id = alarm.id,
                            enabled = alarm.enabled,
                            hour = timeState.hour,
                            minute = timeState.minute,
                            repeatDays = repeatDays,
                            songId = songId,
                            playlistId = playlistId,
                            albumId = albumId,
                            songSnapshot = songSnapshot,
                            sourceName = sourceName,
                            shuffle = shuffle,
                        )
                    )
                },
                icon = R.drawable.checkmark,
                color = colorPalette().accent,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showSourcePicker) {
        AlarmSourcePickerDialog(
            onDismiss = { showSourcePicker = false },
            onSongPicked = { song, name ->
                songId = song.id
                playlistId = null
                albumId = null
                songSnapshot = song
                sourceName = name
                shuffle = false
                showSourcePicker = false
            },
            onPlaylistPicked = { id, name ->
                playlistId = id
                songId = null
                albumId = null
                songSnapshot = null
                sourceName = name
                showSourcePicker = false
            },
            onAlbumPicked = { id, name ->
                albumId = id
                songId = null
                playlistId = null
                songSnapshot = null
                sourceName = name
                showSourcePicker = false
            }
        )
    }
}

@Composable
private fun AlarmChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        style = typography().xxs.medium.copy(
            color = if (selected) colorPalette().onAccent else colorPalette().text,
            textAlign = TextAlign.Center
        ),
        maxLines = 1,
        modifier = modifier
            .clip(getAlarmShape())
            .clickable(onClick = onClick)
            .background(if (selected) colorPalette().accent else colorPalette().background4)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ------------------------------------------------------------------
// Selettore musica: brani (libreria + ricerca online), playlist e album
// ------------------------------------------------------------------

@OptIn(UnstableApi::class)
@Composable
fun AlarmSourcePickerDialog(
    onDismiss: () -> Unit,
    onSongPicked: (Song, String) -> Unit,
    onPlaylistPicked: (Long, String) -> Unit,
    onAlbumPicked: (String, String) -> Unit,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    var localSongs by remember { mutableStateOf(emptyList<Song>()) }
    var onlineSongs by remember { mutableStateOf(emptyList<Environment.SongItem>()) }
    var onlineSearching by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf(emptyList<PlaylistPreview>()) }
    var albums by remember { mutableStateOf(emptyList<Album>()) }

    // Ricerca brani in libreria
    LaunchedEffect(query) {
        if (query.length > 1) {
            Database.search("%$query%").collect { localSongs = it }
        } else {
            localSongs = emptyList()
        }
    }

    // Ricerca brani online (con debounce)
    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(600)
            onlineSearching = true
            val result = Environment.searchPage(
                body = SearchBody(
                    query = query,
                    params = Environment.SearchFilter.Song.value
                ),
                fromMusicShelfRendererContent = Environment.SongItem.Companion::fromMusicShelfRenderer
            )
            onlineSongs = result?.getOrNull()?.items ?: emptyList()
            onlineSearching = false
        } else {
            onlineSongs = emptyList()
            onlineSearching = false
        }
    }

    // Playlist locali
    LaunchedEffect(tabIndex) {
        if (tabIndex == 1) {
            Database.playlistPreviewsByNameAsc().collect { playlists = it }
        }
    }

    // Album con brani in libreria (preferiti, on-device e YouTube), ordine alfabetico
    LaunchedEffect(tabIndex) {
        if (tabIndex == 2) {
            Database.albumsWithSongsSavedByTitleAsc().collect { albums = it.filter { it.bookmarkedAt != null } }
        }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        BasicText(
            text = stringResource(R.string.alarm_pick_title),
            style = typography().m.semiBold
        )

        Spacer(Modifier.height(10.dp))

        // Ricerca
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = typography().m.align(TextAlign.Start),
            cursorBrush = SolidColor(colorPalette().text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorPalette().background4, shape = getAlarmShape())
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = {},
                        icon = R.drawable.search,
                        color = colorPalette().text,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
                        if (query.isEmpty())
                            BasicText(
                                text = stringResource(R.string.alarm_search_hint),
                                style = typography().m.secondary,
                                maxLines = 1
                            )
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            icon = R.drawable.close,
                            color = colorPalette().text,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Tab: Brani / Playlist / Album
        val songsText = stringResource(R.string.songs)
        val playlistsText = stringResource(R.string.playlists)
        val albumsText = stringResource(R.string.albums)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlarmChip(
                text = songsText,
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                modifier = Modifier.weight(1f)
            )
            AlarmChip(
                text = playlistsText,
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                modifier = Modifier.weight(1f)
            )
            AlarmChip(
                text = albumsText,
                selected = tabIndex == 2,
                onClick = { tabIndex = 2 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.height(360.dp)) {
            when (tabIndex) {
                0 -> SongPickerList(
                    query = query,
                    localSongs = localSongs,
                    onlineSongs = onlineSongs,
                    onlineSearching = onlineSearching,
                    onSongPicked = onSongPicked,
                )
                1 -> PlaylistPickerList(
                    playlists = playlists,
                    onPlaylistPicked = onPlaylistPicked,
                )
                2 -> AlbumPickerList(
                    albums = albums.filter {
                        it.title.orEmpty().contains(query, ignoreCase = true) ||
                        it.authorsText.orEmpty().contains(query, ignoreCase = true)
                    },
                    onAlbumPicked = onAlbumPicked,
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun SongPickerList(
    query: String,
    localSongs: List<Song>,
    onlineSongs: List<Environment.SongItem>,
    onlineSearching: Boolean,
    onSongPicked: (Song, String) -> Unit,
) {
    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    if (query.length < 2) {
        BasicText(
            text = stringResource(R.string.alarm_search_hint),
            style = typography().m.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        if (localSongs.isNotEmpty()) {
            item {
                BasicText(
                    text = stringResource(R.string.library),
                    style = typography().xs.semiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            items(items = localSongs, key = { "local-${it.id}" }) { song ->
                SongItem(
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier
                        .clickable {
                            val name = buildString {
                                append(song.title)
                                if (!song.artistsText.isNullOrBlank())
                                    append(" · ${song.artistsText}")
                            }
                            onSongPicked(song, name)
                        }
                )
            }
        }

        if (onlineSearching || onlineSongs.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = stringResource(R.string.online),
                        style = typography().xs.semiBold
                    )
                }
            }
            items(items = onlineSongs, key = { "online-${it.key}" }) { envSong ->
                SongItem(
                    song = envSong,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    modifier = Modifier
                        .clickable {
                            val song = envSong.asSong
                            val name = buildString {
                                append(song.title)
                                if (!song.artistsText.isNullOrBlank())
                                    append(" · ${song.artistsText}")
                            }
                            onSongPicked(song, name)
                        }
                )
            }
        }

        if (!onlineSearching && localSongs.isEmpty() && onlineSongs.isEmpty() && query.length >= 2) {
            item {
                BasicText(
                    text = stringResource(R.string.no_results_found),
                    style = typography().m.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlaylistPickerList(
    playlists: List<PlaylistPreview>,
    onPlaylistPicked: (Long, String) -> Unit,
) {
    if (playlists.isEmpty()) {
        BasicText(
            text = stringResource(R.string.alarm_no_playlists),
            style = typography().m.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(items = playlists, key = { it.playlist.id }) { preview ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPlaylistPicked(
                            preview.playlist.id,
                            preview.playlist.name
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = {
                        onPlaylistPicked(
                            preview.playlist.id,
                            preview.playlist.name
                        )
                    },
                    icon = R.drawable.playlist,
                    color = colorPalette().textSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = cleanPrefix(preview.playlist.name),
                        style = typography().m.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val songCountText = stringResource(R.string.songs)
                    BasicText(
                        text = "${preview.songCount} $songCountText",
                        style = typography().xxs.secondary
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AlbumPickerList(
    albums: List<Album>,
    onAlbumPicked: (String, String) -> Unit,
) {
    if (albums.isEmpty()) {
        BasicText(
            text = stringResource(R.string.alarm_no_albums),
            style = typography().m.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(items = albums, key = { "album-${it.id}" }) { album ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val name = buildString {
                            append(album.title.orEmpty())
                            if (!album.authorsText.isNullOrBlank())
                                append(" · ${album.authorsText}")
                        }
                        onAlbumPicked(album.id, name)
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = {
                        val name = buildString {
                            append(album.title.orEmpty())
                            if (!album.authorsText.isNullOrBlank())
                                append(" · ${album.authorsText}")
                        }
                        onAlbumPicked(album.id, name)
                    },
                    icon = R.drawable.album,
                    color = colorPalette().textSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = cleanPrefix(album.title.orEmpty()),
                        style = typography().m.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        text = album.authorsText.orEmpty(),
                        style = typography().xxs.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
