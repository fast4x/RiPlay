package it.fast4x.riplay.ui.screens.ondevice

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.valentinilk.shimmer.shimmer
import it.fast4x.compose.persist.persist
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.ArtistPage
import it.fast4x.environment.requests.ArtistSection
import it.fast4x.riplay.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.cleanPrefix
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.models.Artist
import it.fast4x.riplay.ui.components.themed.Header
import it.fast4x.riplay.ui.components.themed.HeaderIconButton
import it.fast4x.riplay.ui.components.themed.HeaderPlaceholder
import it.fast4x.riplay.ui.components.themed.SecondaryTextButton
import it.fast4x.riplay.ui.components.themed.adaptiveThumbnailContent
import it.fast4x.riplay.utils.disableScrollingTextKey
import it.fast4x.riplay.utils.parentalControlEnabledKey
import it.fast4x.riplay.utils.rememberPreference
import it.fast4x.riplay.utils.thumbnailRoundnessKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.ui.components.Skeleton
import it.fast4x.riplay.ui.screens.artist.ArtistLocalSongs
import it.fast4x.riplay.ui.screens.artist.ArtistOverview
import kotlinx.coroutines.flow.firstOrNull

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun OnDeviceArtistScreen(
    navController: NavController,
    artistId: String,
    miniPlayer: @Composable () -> Unit = {},
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    var tabIndex by rememberSaveable {
        mutableStateOf(0)
    }

    var artist by persist<Artist?>("artist/$artistId/artist")

    //var artistPage by persist<ArtistPage?>("artist/$artistId/artistPage")

    val context = LocalContext.current

    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )
    var changeShape by remember {
        mutableStateOf(false)
    }

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

//    var artistInDatabase by remember { mutableStateOf<Artist?>(null) }
//
//    Database.asyncTransaction {
//        CoroutineScope(Dispatchers.IO).launch {
//            artistInDatabase = artist(artistId).firstOrNull()
//        }
//    }

    LaunchedEffect(Unit) {

        //artistPage = YtMusic.getArtistPage(browseId)

        Database
            .artist(artistId)
            //.combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { currentArtist ->
                artist = currentArtist

            }
    }

    val listMediaItems = remember { mutableListOf<MediaItem>() }

    var artistItemsSection by remember { mutableStateOf<ArtistSection?>(null) }

            val thumbnailContent =
                adaptiveThumbnailContent(
                    artist?.timestamp == null,
                    artist?.thumbnailUrl,
                    //CircleShape
                    onClick = { changeShape = !changeShape },
                    shape = if (changeShape) CircleShape else thumbnailRoundness.shape(),
                )

            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit =
                { textButton ->
                    if (artist?.timestamp == null) {
                        HeaderPlaceholder(
                            modifier = Modifier
                                .shimmer()
                        )
                    } else {
                        Header(title = cleanPrefix(artist?.name ?: "Unknown"), actionsContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(top = 50.dp)
                                        .padding(horizontal = 12.dp)
                                ) {
                                    textButton?.invoke()

                                    Spacer(
                                        modifier = Modifier
                                            .weight(0.2f)
                                    )

//                                    SecondaryTextButton(
//                                        text = if (artist?.bookmarkedAt == null) stringResource(R.string.follow) else stringResource(
//                                            R.string.following
//                                        ),
//                                        onClick = {
//                                            val bookmarkedAt =
//                                                if (artist?.bookmarkedAt == null) System.currentTimeMillis() else null
//
//                                            Database.asyncTransaction {
//                                                artist?.copy( bookmarkedAt = bookmarkedAt )
//                                                      ?.let( ::update )
//                                            }
//                                        },
//                                        alternative = artist?.bookmarkedAt == null
//                                    )
//
//                                    HeaderIconButton(
//                                        icon = R.drawable.share_social,
//                                        color = colorPalette().text,
//                                        onClick = {
//                                            val sendIntent = Intent().apply {
//                                                Intent.setAction = Intent.ACTION_SEND
//                                                Intent.setType = "text/plain"
//                                                putExtra(
//                                                    Intent.EXTRA_TEXT,
//                                                    "https://music.youtube.com/channel/$artistId"
//                                                )
//                                            }
//
//                                            context.startActivity(
//                                                Intent.createChooser(
//                                                    sendIntent,
//                                                    null
//                                                )
//                                            )
//                                        }
//                                    )
                                }
                            },
                            disableScrollingText = disableScrollingText)
                    }
                }

    Skeleton(
        navController,
        tabIndex,
        onTabChanged = { tabIndex = it },
        miniPlayer,
        navBarContent = {}
    ) { currentTabIndex ->
                //saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> {
                            OnDeviceArtistDetails(
                                navController = navController,
                                artistId = artistId,
                                onItemsPageClick = {},
                                disableScrollingText = disableScrollingText
                            )
                        }
                    }
                //}
            }

}
