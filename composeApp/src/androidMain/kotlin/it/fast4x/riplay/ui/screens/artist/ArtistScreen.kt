package it.fast4x.riplay.ui.screens.artist

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.extensions.preferences.disableScrollingTextKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.PageContainer


@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun ArtistScreen(
    navController: NavController,
    browseId: String,
    miniPlayer: @Composable () -> Unit = {},
) {
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    PageContainer(
        modifier = Modifier,
        navController = navController,
        miniPlayer = miniPlayer,
    ) {
        ArtistOverview(
            navController = navController,
            browseId = browseId,
            disableScrollingText = disableScrollingText
        )
    }

//    val saveableStateHolder = rememberSaveableStateHolder()
//
//    //var tabIndex by rememberPreference(artistScreenTabIndexKey, defaultValue = 0)
//
//    val binder = LocalPlayerServiceBinder.current
//
//    var tabIndex by rememberSaveable {
//        mutableStateOf(0)
//    }

    //PersistMapCleanup(tagPrefix = "artist/$browseId/")
/*
    var artist by persist<Artist?>("artist/$browseId/artist")

    var artistPage by persist<ArtistPage?>("artist/$browseId/artistPage")

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }
    val context = LocalContext.current

    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )
    var changeShape by remember {
        mutableStateOf(false)
    }
    val hapticFeedback = LocalHapticFeedback.current
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var artistInDatabase by remember { mutableStateOf<Artist?>(null) }

    Database.asyncTransaction {
        CoroutineScope(Dispatchers.IO).launch {
            artistInDatabase = artist(browseId).firstOrNull()
        }
    }

    LaunchedEffect(Unit) {

        //artistPage = YtMusic.getArtistPage(browseId)

        Database
            .artist(browseId)
            .combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                if (artistPage == null && (currentArtist?.timestamp == null || mustFetch)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        EnvironmentExt.getArtistPage(browseId = browseId)
                            .onSuccess { currentArtistPage ->
                                artistPage = currentArtistPage

                                Database.upsert(
                                    Artist(
                                        id = browseId,
                                        name = currentArtistPage.artist.info?.name,
                                        thumbnailUrl = currentArtistPage.artist.thumbnail?.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = currentArtist?.bookmarkedAt,
                                        isYoutubeArtist = artistInDatabase?.isYoutubeArtist == true
                                    )
                                )
                            }
                    }
                }
            }
    }
*/
    /*
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

                                    SecondaryTextButton(
                                        text = if (artist?.bookmarkedAt == null) stringResource(R.string.follow) else stringResource(
                                            R.string.following
                                        ),
                                        onClick = {
                                            val bookmarkedAt =
                                                if (artist?.bookmarkedAt == null) System.currentTimeMillis() else null

                                            Database.asyncTransaction {
                                                artist?.copy( bookmarkedAt = bookmarkedAt )
                                                      ?.let( ::update )
                                            }
                                        },
                                        alternative = artist?.bookmarkedAt == null
                                    )

                                    HeaderIconButton(
                                        icon = R.drawable.share_social,
                                        color = colorPalette().text,
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    //"https://music.youtube.com/channel/$browseId"
                                                    "$YTM_ARTIST_SHARE_BASEURL$browseId"
                                                )
                                            }

                                            context.startActivity(
                                                Intent.createChooser(
                                                    sendIntent,
                                                    null
                                                )
                                            )
                                        }
                                    )
                                }
                            },
                            disableScrollingText = disableScrollingText)
                    }
                }

     */

    /*
    ScreenContainer(
        navController,
        tabIndex,
        onTabChanged = { tabIndex = it },
        miniPlayer,
        navBarContent = { Item ->
            Item(0, stringResource(R.string.overview), R.drawable.artist)
            Item(1, stringResource(R.string.library), R.drawable.playlist)
        }
    ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> {
                            ArtistOverview(
                                navController = navController,
                                browseId = browseId,
                                artistPage = artistPage,
                                onItemsPageClick = {
                                    artistItemsSection = it
                                    tabIndex = 2

                                },
                                disableScrollingText = disableScrollingText
                            )
                        }


                        1 -> {
                            ArtistLocalSongs(
                                navController = navController,
                                browseId = browseId,
                                headerContent = headerContent,
                                thumbnailContent = thumbnailContent,
                                onSearchClick = {
                                    //searchRoute("")
                                    navController.navigate(NavRoutes.search.name)
                                },
                                onSettingsClick = {
                                    //settingsRoute()
                                    navController.navigate(NavRoutes.settings.name)
                                }
                            )
                        }
                    }
                }
    }
    */
}
