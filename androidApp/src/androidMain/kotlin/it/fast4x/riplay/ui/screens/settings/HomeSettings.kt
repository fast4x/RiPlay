package it.fast4x.riplay.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.HomePagetype
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.components.themed.settingsItem
import it.fast4x.riplay.utils.LazyListContainer
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun  HomeSettings() {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val showTips = appSettings.showTips
    val showRelatedAlbums = appSettings.showRelatedAlbums
    val showSimilarArtists = appSettings.showSimilarArtists
    val showNewAlbumsArtists = appSettings.showNewAlbumsArtists
    val showNewAlbums = appSettings.showNewAlbums
    val showPlaylistMightLike = appSettings.showPipedPlaylists
    val showMoodsAndGenres = appSettings.showMoodAndGenres
    val showMonthlyPlaylistInQuickPicks = appSettings.showMonthlyPlaylistInQuickPicks
    val showCharts = appSettings.showCharts

    val homePageType = appSettings.homePageType

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        val state = rememberLazyListState()
        LazyListContainer(
            state = state
        ) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
            ) {
                settingsItem {
                    HeaderWithIcon(
                        title = stringResource(R.string.home),
                        iconId = if (!isYtLoggedIn()) R.drawable.sparkles else R.drawable.home,
                        enabled = false,
                        showIcon = true,
                        modifier = Modifier,
                        onClick = {}
                    )
                }

                settingsItem(
                    isHeader = true
                ) {
                    SettingsEntryGroupText(title = stringResource(R.string.home))
                }

                settingsItem {
                    Column(
                        modifier = Modifier.padding(start = 12.dp)
                    ) {

                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.homepage_type),
                            selectedValue = homePageType,
                            onValueSelected = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(homePageType = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            },
                            valueText = { it.textName }
                        )

                        /*
                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.listener_levels)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.listener_levels
                            ),
                            isChecked = showListenerLevels,
                            onCheckedChange = {
                                showListenerLevels = it
                            }
                        )
                         */

                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.quick_picks)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.tips
                            ),
                            isChecked = showTips,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(showTips = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }
                        )

                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums_of_your_artists)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.new_albums_of_your_artists
                            ),
                            isChecked = showNewAlbumsArtists,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(showNewAlbumsArtists = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }
                        )

                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.new_albums
                            ),
                            isChecked = showNewAlbums,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(showNewAlbums = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }
                        )

                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.moods_and_genres)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.moods_and_genres
                            ),
                            isChecked = showMoodsAndGenres,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    val new = appSettingsManager.activeSettings.value.copy(showMoodAndGenres = it)
                                    appSettingsManager.updateSettings(new)
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = homePageType == HomePagetype.Extended,
                        ) {
                            Column {
                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.charts)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.charts
                                    ),
                                    isChecked = showCharts,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showCharts = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.related_albums)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.related_albums
                                    ),
                                    isChecked = showRelatedAlbums,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showRelatedAlbums = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.similar_artists)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.similar_artists
                                    ),
                                    isChecked = showSimilarArtists,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showSimilarArtists = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.playlists_you_might_like)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.playlists_you_might_like
                                    ),
                                    isChecked = showPlaylistMightLike,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new = appSettingsManager.activeSettings.value.copy(showPlaylistMightLike = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.monthly_playlists
                                    ),
                                    isChecked = showMonthlyPlaylistInQuickPicks,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val new =
                                                appSettingsManager.activeSettings.value.copy(showMonthlyPlaylistInQuickPicks = it)
                                            appSettingsManager.updateSettings(new)
                                        }
                                    }
                                )
                            }

                        }
                    }

                }
            }
        }

    }
}
