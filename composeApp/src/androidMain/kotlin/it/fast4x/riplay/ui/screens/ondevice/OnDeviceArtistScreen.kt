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

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    Skeleton(
        navController,
        0,
        onTabChanged = {},
        miniPlayer,
        navBarContent = {}
    ) { currentTabIndex ->
                    when (currentTabIndex) {
                        0 -> {
                            OnDeviceArtistDetails(
                                navController = navController,
                                artistId = artistId,
                                disableScrollingText = disableScrollingText
                            )
                        }
                    }
            }

}
