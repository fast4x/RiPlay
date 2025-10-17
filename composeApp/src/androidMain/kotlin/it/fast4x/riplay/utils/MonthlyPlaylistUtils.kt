package it.fast4x.riplay.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import it.fast4x.riplay.Database
import it.fast4x.riplay.MONTHLY_PREFIX
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.PlaylistWithSongs
import it.fast4x.riplay.data.models.SongPlaylistMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
fun CheckAndCreateMonthlyPlaylist() {
    val ym by remember { mutableStateOf( getCalculatedMonths(1)) }
    val y by remember { mutableLongStateOf( ym?.substring(0,4)?.toLong() ?: 0) }
    val m by remember { mutableLongStateOf( ym?.substring(5,7)?.toLong() ?: 0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Database.playlistWithSongsNoFlow("${MONTHLY_PREFIX}${ym}").let { monthly ->
                if (monthly != null) {
                    Timber.d("CheckAndCreateMonthlyPlaylist Playlist present no creating playlist ${monthly.playlist.name}")
                    return@withContext
                }
                Database.songsMostPlayedByYearMonthNoFlow(y, m).let { songs ->
                    if (songs.isNotEmpty()) {
                        Timber.d("CheckAndCreateMonthlyPlaylist Playlist not present, SongsMostPlayed ${songs.size} creating playlist")
                        Database.asyncTransaction {
                            //val playlistId = insert(Playlist(name = "${MONTHLY_PREFIX}${ym}"))
                            insert(Playlist(name = "${MONTHLY_PREFIX}${ym}")).let {
                                songs.forEachIndexed { position, song ->
                                    insert(
                                        SongPlaylistMap(
                                            songId = song.id,
                                            playlistId = it,
                                            position = position
                                        ).default()
                                    )
                                }
                            }

                        }

                    } else {
                        Timber.d("CheckAndCreateMonthlyPlaylist SongsMostPlayed empty no create playlist")
                    }
                }
            }
        }

    }

}

fun getTitleMonthlyPlaylist(playlist: String, context: Context): String {

    val y = playlist.substring(0,4)
    val m = playlist.substring(5,7).toInt()
    return when (m) {
        1 -> context.resources.getString(R.string.month_january_s).format(y)
        2 -> context.resources.getString(R.string.month_february_s).format(y)
        3 -> context.resources.getString(R.string.month_march_s).format(y)
        4 -> context.resources.getString(R.string.month_april_s).format(y)
        5 -> context.resources.getString(R.string.month_may_s).format(y)
        6 -> context.resources.getString(R.string.month_june_s).format(y)
        7 -> context.resources.getString(R.string.month_july_s).format(y)
        8 -> context.resources.getString(R.string.month_august_s).format(y)
        9 -> context.resources.getString(R.string.month_september_s).format(y)
        10 -> context.resources.getString(R.string.month_october_s).format(y)
        11 -> context.resources.getString(R.string.month_november_s).format(y)
        12 -> context.resources.getString(R.string.month_december_s).format(y)
        else -> playlist
    }
}


@Composable
fun monthlyPLaylist(playlist: String?): State<PlaylistWithSongs?> {
    val monthlyPlaylist = remember {
        Database.playlistWithSongs("${MONTHLY_PREFIX}${playlist}")
    }.collectAsState(initial = null, context = Dispatchers.IO)

    return monthlyPlaylist
}

@Composable
fun monthlyPLaylists(playlist: String? = ""): State<List<PlaylistWithSongs?>?> {
    val monthlyPlaylists = remember {
        Database.monthlyPlaylists(playlist)
    }.collectAsState(initial = null, context = Dispatchers.IO)

    return monthlyPlaylists
}
