package it.fast4x.riplay.ui.screens.search

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.persist.persistList
import it.fast4x.environment.models.NavigationEndpoint
import it.fast4x.riplay.LocalAppearanceSettingsManager
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.riplay.ui.components.themed.InHistoryMediaItemMenu
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.asMediaItem
import kotlinx.coroutines.delay
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.LazyListContainer
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.insertOrUpdateBlacklist
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalSerializationApi
@UnstableApi
@Composable
fun LocalSongSearch(
    navController: NavController,
    textFieldValue: TextFieldValue,
    onTextFieldValueChanged: (TextFieldValue) -> Unit,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit,
    onAction1: () -> Unit,
    onAction2: () -> Unit,
    onAction3: () -> Unit,
    onAction4: () -> Unit,
) {

    val appearanceSettingsManager = LocalAppearanceSettingsManager.current
    val appearanceSettings = appearanceSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalGlobalSheetState.current

    var items by persistList<Song>("search/local/songs")

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.length > 1) {
            Database.search("%${textFieldValue.text}%").collect { items = it }
        }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    //val disableScrollingText by rememberPreference(DISABLE_SCROLLING_TEXT.key, false)
    val disableScrollingText = appearanceSettings.disableScrollingText

    val focusRequester = remember {
        FocusRequester()
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        LazyListContainer(
            state = lazyListState,
        ) {
            LazyColumn(
                state = lazyListState,
//                contentPadding = LocalPlayerAwareWindowInsets.current
//                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                /*
                item(
                    key = "header",
                    contentType = 0
                ) {

                    Header(
                        titleContent = {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = onTextFieldValueChanged,
                                textStyle = typography().l.medium.align(TextAlign.Start),
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                cursorBrush = SolidColor(colorPalette().text),
                                decorationBox = decorationBox,
                                modifier = Modifier
                                    .background(
                                        colorPalette().background1,
                                        shape = thumbnailRoundness.shape()
                                    )
                                    .padding(all = 4.dp)
                                    .focusRequester(focusRequester)
                                    .fillMaxWidth()
                            )
                        },
                        actionsContent = {},
                    )
                }

                 */

                items(
                    items = items,
                    key = Song::id,
                ) { song ->
                    SongItem(
                        song = song,
                        thumbnailSizePx = thumbnailSizePx,
                        thumbnailSizeDp = thumbnailSizeDp,
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        InHistoryMediaItemMenu(
                                            navController = navController,
                                            onDismiss = menuState::hide,
                                            song = song,
                                            disableScrollingText = disableScrollingText,
                                            onBlacklist = {
                                                insertOrUpdateBlacklist(song)
                                            },
                                        )
                                    }
                                },
                                onClick = {
                                    val mediaItem = song.asMediaItem
                                    binder?.stopRadio()
                                    binder?.player?.forcePlay(mediaItem)
                                    //fastPlay(mediaItem, binder)
                                    binder?.setupRadio(
                                        NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                    )
                                }
                            )
                            .animateItem(),
                        //disableScrollingText = disableScrollingText,
                        //isNowPlaying = binder?.player?.isNowPlaying(song.id) ?: false
                    )
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }
}
