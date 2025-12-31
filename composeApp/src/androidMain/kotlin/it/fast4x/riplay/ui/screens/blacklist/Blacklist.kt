package it.fast4x.riplay.ui.screens.blacklist

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persist
import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.requests.HistoryPage
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.commonutils.EXPLICIT_PREFIX
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.data.models.DateAgo
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.components.themed.NonQueuedMediaItemMenuLibrary
import it.fast4x.riplay.ui.components.themed.NowPlayingSongIndicator
import it.fast4x.riplay.ui.components.themed.Title
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.parentalControlEnabledKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.HistoryType
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.screens.settings.isLoggedIn
import it.fast4x.riplay.extensions.preferences.historyTypeKey
import it.fast4x.riplay.ui.components.tab.TabHeader
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderInfo
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.TimeZone
import kotlin.collections.map

@kotlin.OptIn(ExperimentalTextApi::class)
@OptIn(UnstableApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun Blacklist(
    navController: NavController
) {
    var list: List<Blacklist> by remember { mutableStateOf(emptyList()) }
    var currentBlacklist: Blacklist? by remember { mutableStateOf(null) }

    val buttonsList = remember { mutableStateOf<List<Pair<BlacklistType, String>>>(
        BlacklistType.entries.map { Pair(it, appContext().resources.getString(it.title)) }
    ) }
    var showStringRemoveDialog by remember {
        mutableStateOf(false)
    }
    var blacklistType by remember { mutableStateOf(BlacklistType.Album) }

    LaunchedEffect(Unit, blacklistType) {
        Database.blacklists(blacklistType.name).collect {
            list = it
        }
    }

    if (showStringRemoveDialog) {
        ConfirmationDialog(
            text = "Remove",
            onDismiss = { showStringRemoveDialog = false },
            onConfirm = {
                CoroutineScope(Dispatchers.IO).launch {
                    currentBlacklist?.let {
                        Database.delete(it)
                    }
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            TabHeader(R.string.blacklist) {
                HeaderInfo(list.size.toString(), R.drawable.alert_circle)
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
            ) {
                ButtonsRow(
                    buttons = buttonsList.value,
                    currentValue = blacklistType,
                    onValueUpdate = { blacklistType = it }
                )
            }

            val state = rememberLazyListState()
            LazyListContainer(
                state = state,
            ) {
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                ) {
                    items(
                        items = list,
                        key = Blacklist::id
                    ) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            Image(
                                painter = painterResource(when(item.type) {
                                    BlacklistType.Folder.name -> R.drawable.folder
                                    BlacklistType.Artist.name -> R.drawable.artist
                                    BlacklistType.Album.name -> R.drawable.album
                                    BlacklistType.Song.name -> R.drawable.musical_note
                                    BlacklistType.Playlist.name -> R.drawable.music_library
                                    BlacklistType.Video.name -> R.drawable.video
                                    else -> R.drawable.text
                                }),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(if (item.isEnabled) colorPalette().text else colorPalette().textDisabled),
                                modifier = Modifier
                                    .size(24.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                BasicText(
                                    text = cleanPrefix(item.name ?: stringResource(R.string.unknown_title)),
                                    style = typography().xs.semiBold.copy(color = if (item.isEnabled) colorPalette().text else colorPalette().textDisabled),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    modifier = Modifier
                                )
                                BasicText(
                                    text = item.path,
                                    style = typography().xxxs.semiBold.copy(color = if (item.isEnabled) colorPalette().text else colorPalette().textDisabled),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    modifier = Modifier
                                )
                            }

                            Image(
                                painter = painterResource(if (item.isEnabled) R.drawable.eye else R.drawable.eye_off),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(if (item.isEnabled) colorPalette().text else colorPalette().textDisabled),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            Database.update(item.toggleEnabled())
                                        }
                                    }
                            )
                            Image(
                                painter = painterResource(R.drawable.trash),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette().red),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        currentBlacklist = item
                                        showStringRemoveDialog = true
                                    }
                            )

                        }

                    }
                }
            }

        }
    }
}

