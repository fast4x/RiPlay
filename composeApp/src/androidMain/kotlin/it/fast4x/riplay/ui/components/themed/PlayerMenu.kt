package it.fast4x.riplay.ui.components.themed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.utils.globalContext
import it.fast4x.riplay.enums.MenuStyle
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.ui.screens.settings.isSyncEnabled
import it.fast4x.riplay.utils.addSongToYtPlaylist
import it.fast4x.riplay.utils.addToYtLikedSong
import it.fast4x.riplay.utils.addToYtPlaylist
import org.dailyislam.android.utilities.isNetworkConnected
import it.fast4x.riplay.extensions.preferences.menuStyleKey
import it.fast4x.riplay.utils.rememberEqualizerLauncher
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.utils.removeYTSongFromPlaylist
import it.fast4x.riplay.utils.seamlessPlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalTextApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun PlayerMenu(
    navController: NavController,
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onClosePlayer: () -> Unit,
    onMatchingSong: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onSelectUnselect: (() -> Unit)? = null,
    disableScrollingText: Boolean
    ) {

    val menuStyle by rememberPreference(
        menuStyleKey,
        MenuStyle.List
    )

    //val context = LocalContext.current

    val launchEqualizer by rememberEqualizerLauncher(audioSessionId = { binder.player.audioSessionId })

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    var isHiding by remember {
        mutableStateOf(false)
    }

    var deleteAlsoPlayTimes by remember {
        mutableStateOf(false)
    }

    if (isHiding) {
        ConfirmationDialog(
            text = stringResource(R.string.update_song),
            onDismiss = {
                isHiding = false
            },
            checkBoxText = stringResource(R.string.also_delete_playback_data),
            onCheckBox = {
                deleteAlsoPlayTimes = it
            },
            onConfirm = {
                onDismiss()
                mediaItem.mediaId.let {
                    try {
                        binder.cache.removeResource(it) //try to remove from cache if exists
                    } catch (e: Exception) {
                        Timber.e("PlayerMenu cache resource removeResource ${e.stackTraceToString()}")
                    }

                }

                if (deleteAlsoPlayTimes)
                    Database.asyncTransaction {
                        println("PlayerMenu deleteAlsoPlayTimes")
                        resetTotalPlayTimeMs(mediaItem.mediaId)
                    }

                binder.player.seekTo(0L)
            }
        )
    }


    if (menuStyle == MenuStyle.Grid) {
        BaseMediaItemGridMenu(
            navController = navController,
            mediaItem = mediaItem,
            onDismiss = onDismiss,
            onStartRadio = {
                binder.stopRadio()
                binder.player.seamlessPlay(mediaItem)
                binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
            },
            onGoToEqualizer = launchEqualizer,
            /*
            onGoToEqualizer = {
                try {
                    activityResultLauncher.launch(
                        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    SmartMessage(context.resources.getString(R.string.info_not_find_application_audio), type = PopupType.Warning, context = context)
                }
            },
             */
            onHideFromDatabase = { isHiding = true },
            onClosePlayer = onClosePlayer,
            onInfo = onInfo,
            disableScrollingText = disableScrollingText
        )
    } else {
        BaseMediaItemMenu(
            navController = navController,
            mediaItem = mediaItem,
            onStartRadio = {
                binder.stopRadio()
                binder.player.seamlessPlay(mediaItem)
                binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
            },
            onGoToEqualizer = launchEqualizer,
            onShowSleepTimer = {},
            onHideFromDatabase = { isHiding = true },
            onDismiss = onDismiss,
            onAddToPreferites = {
                if (!isNetworkConnected(globalContext()) && isSyncEnabled()){
                    SmartMessage(globalContext().resources.getString(R.string.no_connection), context = globalContext(), type = PopupType.Error)
                } else if (!isSyncEnabled()){
                    Database.asyncTransaction {
                        like(
                            mediaItem.mediaId,
                            System.currentTimeMillis()
                        )
                    }
                }
                else {
                    CoroutineScope(Dispatchers.IO).launch {
                        addToYtLikedSong(mediaItem)
                    }
                }
            },
            onClosePlayer = onClosePlayer,
            onMatchingSong = onMatchingSong,
            onInfo = onInfo,
            onSelectUnselect = onSelectUnselect,
            disableScrollingText = disableScrollingText
        )
    }

}


@ExperimentalTextApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun MiniPlayerMenu(
    navController: NavController,
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onClosePlayer: () -> Unit,
    onInfo: (() -> Unit)? = null,
    disableScrollingText: Boolean
) {

    val menuStyle by rememberPreference(
        menuStyleKey,
        MenuStyle.List
    )

    if (menuStyle == MenuStyle.Grid) {
        MiniMediaItemGridMenu(
            navController = navController,
            mediaItem = mediaItem,
            onGoToPlaylist = {
                onClosePlayer()
            },
            onAddToPreferites = {
                if (!isNetworkConnected(globalContext()) && isSyncEnabled()){
                    SmartMessage(globalContext().resources.getString(R.string.no_connection), context = globalContext(), type = PopupType.Error)
                } else if (!isSyncEnabled()){
                    Database.asyncTransaction {
                        like(
                            mediaItem.mediaId,
                            System.currentTimeMillis()
                        )
                    }
                }
                else {
                    CoroutineScope(Dispatchers.IO).launch {
                        addToYtLikedSong(mediaItem)
                    }
                }
            },
            onDismiss = onDismiss,
            disableScrollingText = disableScrollingText
        )
    } else {
        MiniMediaItemMenu(
            navController = navController,
            mediaItem = mediaItem,
            onGoToPlaylist = {
                onClosePlayer()
            },
            onAddToPreferites = {
                if (!isNetworkConnected(globalContext()) && isSyncEnabled()){
                    SmartMessage(globalContext().resources.getString(R.string.no_connection), context = globalContext(), type = PopupType.Error)
                } else if (!isSyncEnabled()){
                    Database.asyncTransaction {
                        like(
                            mediaItem.mediaId,
                            System.currentTimeMillis()
                        )
                    }
                }
                else {
                    CoroutineScope(Dispatchers.IO).launch {
                        addToYtLikedSong(mediaItem)
                    }
                }
            },
            onInfo = onInfo,
            onDismiss = onDismiss,
            disableScrollingText = disableScrollingText
        )
    }

}

@ExperimentalTextApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun AddToPlaylistPlayerMenu(
    navController: NavController,
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onClosePlayer: () -> Unit,
) {

    AddToPlaylistItemMenu(
        navController = navController,
        mediaItem = mediaItem,
        onGoToPlaylist = {
            onClosePlayer()
        },
        onAddToPlaylist = { playlist, position ->
            if (!isSyncEnabled() || !playlist.isYoutubePlaylist){
                Database.asyncTransaction {
                    insert(mediaItem)
                    insert(
                        SongPlaylistMap(
                            songId = mediaItem.mediaId,
                            playlistId = insert(playlist).takeIf { it != -1L } ?: playlist.id,
                            position = position
                        ).default()
                    )
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    addSongToYtPlaylist(playlist.id, position, playlist.browseId ?: "", mediaItem)
                }
            }
        },
        onRemoveFromPlaylist = { playlist ->
            if(isSyncEnabled() && playlist.isYoutubePlaylist && playlist.isEditable) {
                Database.asyncTransaction {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (removeYTSongFromPlaylist(
                                mediaItem.mediaId,
                                playlist.browseId ?: "",
                                playlist.id
                            )
                        )
                            deleteSongFromPlaylist(mediaItem.mediaId, playlist.id)

                    }
                }
            } else {
                Database.asyncTransaction {
                    deleteSongFromPlaylist(mediaItem.mediaId, playlist.id)
                }
            }
        },
        onDismiss = onDismiss,
    )
}

@ExperimentalTextApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun AddToPlaylistArtistSongs(
    navController: NavController,
    mediaItems: List<MediaItem>,
    onDismiss: () -> Unit,
    onClosePlayer: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var position by remember {
        mutableIntStateOf(0)
    }
    AddToPlaylistArtistSongsMenu(
        navController = navController,
        onGoToPlaylist = {
            onClosePlayer()
        },
        onAddToPlaylist = { playlistPreview ->
            position = playlistPreview.songCount.minus(1)
            if (position > 0) position++ else position = 0
            mediaItems.forEachIndexed { index, mediaItem ->
                if (!isSyncEnabled() || !playlistPreview.playlist.isYoutubePlaylist){
                    Database.asyncTransaction {
                        insert(mediaItem)
                        insert(
                            SongPlaylistMap(
                                songId = mediaItem.mediaId,
                                playlistId = playlistPreview.playlist.id,
                                position = position + index
                            ).default()
                        )
                    }
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        addToYtPlaylist(playlistPreview.playlist.id, position, playlistPreview.playlist.browseId ?: "", mediaItems)
                    }
                }

            }
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}