package it.fast4x.riplay.ui.screens.blacklist

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.BlacklistType
import it.fast4x.riplay.ui.styling.Dimensions
import kotlinx.coroutines.Dispatchers
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.components.ButtonsRow
import it.fast4x.riplay.ui.components.tab.TabHeader
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderInfo
import it.fast4x.riplay.ui.items.BlacklistItem
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.appContext
import it.fast4x.riplay.utils.asBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
                        BlacklistItem(
                            blacklistedItem = item,
                            enabled = item.enabled.asBoolean,
                            onEnable = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    Database.update(item.toggleEnabled())
                                }
                            },
                            onRemove = {
                                currentBlacklist = item
                                showStringRemoveDialog = true
                            },
                            onClick = {
                                val destination =  when (item.type) {
                                    BlacklistType.Song.name, BlacklistType.Video.name -> "${NavRoutes.videoOrSongInfo.name}/${item.path}"
                                    BlacklistType.Album.name -> "${NavRoutes.album.name}/${item.path}"
                                    BlacklistType.Artist.name -> "${NavRoutes.artist.name}/${item.path}"
                                    BlacklistType.Playlist.name -> "${NavRoutes.localPlaylist.name}/${item.path}"
                                    else -> null
                                }
                                if (destination != null) {
                                    navController.navigate(destination)
                                }
                            }
                        )
                    }
                }
            }

        }
    }
}

