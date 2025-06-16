package it.fast4x.riplay.ui.screens.player.online

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.context
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.enums.PlayerType
import it.fast4x.riplay.models.Info
import it.fast4x.riplay.models.ui.toUiMedia
import it.fast4x.riplay.utils.blurStrengthKey
import it.fast4x.riplay.utils.colorPaletteModeKey
import it.fast4x.riplay.utils.controlsExpandedKey
import it.fast4x.riplay.utils.expandedplayerKey
import it.fast4x.riplay.utils.isExplicit
import it.fast4x.riplay.utils.isShowingLyricsKey
import it.fast4x.riplay.utils.lastVideoIdKey
import it.fast4x.riplay.utils.lastVideoSecondsKey
import it.fast4x.riplay.utils.playerTypeKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.timelineExpandedKey
import it.fast4x.riplay.utils.titleExpandedKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext


@UnstableApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalTextApi
@Composable
fun OnlinePlayer(
    lifecycleOwner: LifecycleOwner,
    showPlayer: Boolean = true,
    onCurrentSecond: (second: Float) -> Unit,
    onVideoDuration: (duration: Float) -> Unit,
    onVideoEnded: () -> Unit,
    onSwitchToAudioPlayer: () -> Unit,
    onDismiss: () -> Unit,
    navController: NavController,
    mediaItem: MediaItem,
) {

    if (!showPlayer) return

    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

    var currentSecond by remember { mutableFloatStateOf(0f) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var positionAndDuration by remember { mutableStateOf(0f to 0f) }

    LaunchedEffect(currentSecond, currentDuration) {
        positionAndDuration = currentSecond to currentDuration

        if (currentSecond >= currentDuration) {
            onVideoEnded()
        }
    }

    var isShowingVisualizer by remember { mutableStateOf(false) }
    val playerType by rememberPreference(playerTypeKey, PlayerType.Essential)
    var expandedplayer by rememberPreference(expandedplayerKey, false)
    var titleExpanded by rememberPreference(titleExpandedKey, false)
    var timelineExpanded by rememberPreference(timelineExpandedKey, false)
    var controlsExpanded by rememberPreference(controlsExpandedKey, false)
    var isShowingLyrics by rememberPreference(isShowingLyricsKey, false)
    val binder = LocalPlayerServiceBinder.current
    var nullableMediaItem by remember {
        mutableStateOf(binder?.player?.currentMediaItem, neverEqualPolicy())
    }
    val mediaItem = nullableMediaItem ?: return
    var albumInfo by remember {
        mutableStateOf(mediaItem.mediaMetadata.extras?.getString("albumId")?.let { albumId ->
            Info(albumId, null)
        })
    }

    var artistsInfo by remember {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.let { artistNames ->
                mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artistIds ->
                    artistNames.zip(artistIds).map { (authorName, authorId) ->
                        Info(authorId, authorName)
                    }
                }
            }
        )
    }

    var updateBrush by remember { mutableStateOf(false) }

    LaunchedEffect(mediaItem.mediaId) {
        withContext(Dispatchers.IO) {
            albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
            artistsInfo = Database.songArtistInfo(mediaItem.mediaId)
        }
        updateBrush = true
    }

    val ExistIdsExtras =
        mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.size.toString()
    val ExistAlbumIdExtras = mediaItem.mediaMetadata.extras?.getString("albumId")

    var albumId = albumInfo?.id
    if (albumId == null) albumId = ExistAlbumIdExtras

    var artistIds = arrayListOf<String>()
    var artistNames = arrayListOf<String>()


    artistsInfo?.forEach { (id) -> artistIds = arrayListOf(id) }
    if (ExistIdsExtras.equals(0)
            .not()
    ) mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.toCollection(artistIds)

    artistsInfo?.forEach { (name) -> artistNames = arrayListOf(name) }
    if (ExistIdsExtras.equals(0)
            .not()
    ) mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.toCollection(artistNames)

    if (artistsInfo?.isEmpty() == true && ExistIdsExtras.equals(0).not()) {
        artistsInfo = artistNames.let { artistNames ->
            artistIds.let { artistIds ->
                artistNames.zip(artistIds).map {
                    Info(it.second, it.first)
                }
            }
        }
    }

    var likedAt by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    LaunchedEffect(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged().collect { likedAt = it }
        updateBrush = true
    }

    val defaultStrength = 25f
    var blurStrength by rememberPreference(blurStrengthKey, defaultStrength)

//    val currentYTVideoId by remember { mutableStateOf(ytVideoId) }
//    println("mediaItem youtubePlayer called currentYTVideoId $currentYTVideoId ytVideoId $ytVideoId lastYTVideoId $lastYTVideoId")

    if (mediaItem.mediaId != lastYTVideoId) lastYTVideoSeconds = 0f

    val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(top = WindowInsets.systemBars
                .asPaddingValues()
                .calculateTopPadding(),
                bottom = WindowInsets.systemBars
                    .asPaddingValues()
                    .calculateBottomPadding()
            )
            .fillMaxSize()

    ) {
//        Box{
//            Image(
//                painter = painterResource(R.drawable.musical_notes),
//                contentDescription = null,
//                colorFilter = ColorFilter.tint(colorPalette().collapsedPlayerProgressBar),
//                modifier = Modifier
//                    .clickable {
//                        onSwitchToAudioPlayer()
//                    }
//                    .padding(top = 30.dp, start = 10.dp)
//                    .size(24.dp)
//            )
//        }
        val onlinePlayerView = YouTubePlayerView(context = context())
        val customPLayerUi = onlinePlayerView.inflateCustomPlayerUi(R.layout.ayp_default_player_ui)
        var player = remember { mutableStateOf<YouTubePlayer?>(null) }
        val playerState = remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }
        var shouldBePlaying by remember { mutableStateOf(false) }

        LaunchedEffect(playerState.value) {
            shouldBePlaying = playerState.value == PlayerConstants.PlayerState.PLAYING
        }


        AndroidView(
            modifier = Modifier,
                //.fillMaxSize(),
                //.padding(8.dp)
                //.clip(RoundedCornerShape(10.dp))
                //.zIndex(2f),
            factory = {





//                val iFramePlayerOptions = IFramePlayerOptions.Builder()
//                    .controls(1) // show/hide controls
//                    .rel(0) // related video at the end
//                    .ivLoadPolicy(0) // show/hide annotations
//                    .ccLoadPolicy(0) // show/hide captions
//                    // Play a playlist by id
//                    //.listType("playlist")
//                    //.list(PLAYLIST_ID)
//                    .build()

                // Disable default view controls to set custom view
                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(0) // show/hide controls
                    .listType("playlist")
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {

                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player.value = youTubePlayer

                        val customPlayerUiController = CustomPlayerUiController(
                            it,
                            customPLayerUi,
                            youTubePlayer,
                            onlinePlayerView
                        )
                        youTubePlayer.addListener(customPlayerUiController)

                        youTubePlayer.loadVideo(mediaItem.mediaId, lastYTVideoSeconds)

                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        currentSecond = second
                        onCurrentSecond(second)
                        lastYTVideoSeconds = second
                        lastYTVideoId = mediaItem.mediaId
                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        super.onVideoDuration(youTubePlayer, duration)
                        currentDuration = duration
                        onVideoDuration(duration)
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        super.onStateChange(youTubePlayer, state)
                        if (state == PlayerConstants.PlayerState.ENDED) {
                            onVideoEnded()
                        }
                        playerState.value = state
                    }


                }

                onlinePlayerView.apply {
                    enableAutomaticInitialization = false

                    lifecycleOwner.lifecycle.addObserver(this)

                    initialize(listener, false, iFramePlayerOptions)
                }

            }
        )

        val controlsContent: @Composable (
            modifier: Modifier
        ) -> Unit = { modifierValue ->
            Controls(
                navController = navController,
                onCollapse = onDismiss,
                expandedplayer = expandedplayer,
                titleExpanded = titleExpanded,
                timelineExpanded = timelineExpanded,
                controlsExpanded = controlsExpanded,
                isShowingLyrics = isShowingLyrics,
                media = mediaItem.toUiMedia(positionAndDuration.second.toLong()),
                mediaItem = mediaItem,
                title = mediaItem.mediaMetadata.title?.toString() ?: "",
                artist = mediaItem.mediaMetadata.artist?.toString(),
                artistIds = artistsInfo,
                albumId = albumId,
                shouldBePlaying = shouldBePlaying,
                position = positionAndDuration.first.toLong(),
                duration = positionAndDuration.second.toLong(),
                modifier = modifierValue,
                onBlurScaleChange = { blurStrength = it },
                isExplicit = mediaItem.isExplicit,
                onPlay = { player.value?.play() },
                onPause = { player.value?.pause() },
            )
        }



        Row {
            controlsContent(
                Modifier
                    .padding(vertical = 8.dp)
                    .border(BorderStroke(1.dp, colorPalette().red))
            )
        }




    }

}
