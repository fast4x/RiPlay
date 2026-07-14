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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.LocalAppSettings
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.HomePagetype
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.PreferenceKey.HOME_PAGE_TYPE
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_CHARTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MONTHLY_PLAYLIST_IN_QUICK_PICKS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_MOODS_AND_GENRES
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEW_ALBUMS_ARTISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_NEW_ALBUMS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_PLAYLIST_MIGHT_LIKE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_RELATED_ALBUMS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_SIMILAR_ARTISTS
import it.fast4x.riplay.extensions.preferences.PreferenceKey.SHOW_TIPS
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.ui.components.themed.settingsItem
import it.fast4x.riplay.utils.LazyListContainer

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun  HomeSettings() {
    val appSettingsVieModel = LocalAppSettings.current
    val appSettings = appSettingsVieModel.activeSettings.collectAsState().value

    //var showListenerLevels by rememberPreference(showListenerLevelsKey.key, true)
    //var showTips by rememberPreference(SHOW_TIPS.key, true)
    val showTips = appSettings.showTips
    //var showRelatedAlbums by rememberPreference(SHOW_RELATED_ALBUMS.key, true)
    val showRelatedAlbums = appSettings.showRelatedAlbums
    //var showSimilarArtists by rememberPreference(SHOW_SIMILAR_ARTISTS.key, true)
    val showSimilarArtists = appSettings.showSimilarArtists
    //var showNewAlbumsArtists by rememberPreference(SHOW_NEW_ALBUMS_ARTISTS.key, true)
    val showNewAlbumsArtists = appSettings.showNewAlbumsArtists
    //var showNewAlbums by rememberPreference(SHOW_NEW_ALBUMS.key, true)
    val showNewAlbums = appSettings.showNewAlbums
    //var showPlaylistMightLike by rememberPreference(SHOW_PLAYLIST_MIGHT_LIKE.key, true)
    val showPlaylistMightLike = appSettings.showPipedPlaylists
    //var showMoodsAndGenres by rememberPreference(SHOW_MOODS_AND_GENRES.key, true)
    val showMoodsAndGenres = appSettings.showMoodAndGenres
    //var showMonthlyPlaylistInQuickPicks by rememberPreference(SHOW_MONTHLY_PLAYLIST_IN_QUICK_PICKS.key, true)
    val showMonthlyPlaylistInQuickPicks = appSettings.showMonthlyPlaylistInQuickPicks
    //var showCharts by rememberPreference(SHOW_CHARTS.key, true)
    val showCharts = appSettings.showCharts

    //var homePageType by rememberPreference(HOME_PAGE_TYPE.key, HomePagetype.Classic)
    val homePageType = appSettings.homePageType

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
                                val new = appSettings.copy(homePageType = it)
                                appSettingsVieModel.updateSettings(new)
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
                                val new = appSettings.copy(showTips = it)
                                appSettingsVieModel.updateSettings(new)
                            }
                        )

                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums_of_your_artists)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.new_albums_of_your_artists
                            ),
                            isChecked = showNewAlbumsArtists,
                            onCheckedChange = {
                                val new = appSettings.copy(showNewAlbumsArtists = it)
                                appSettingsVieModel.updateSettings(new)
                            }
                        )

                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.new_albums
                            ),
                            isChecked = showNewAlbums,
                            onCheckedChange = {
                                val new = appSettings.copy(showNewAlbums = it)
                                appSettingsVieModel.updateSettings(new)
                            }
                        )

                        SwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.moods_and_genres)}",
                            text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                R.string.moods_and_genres
                            ),
                            isChecked = showMoodsAndGenres,
                            onCheckedChange = {
                                val new = appSettings.copy(showMoodAndGenres = it)
                                appSettingsVieModel.updateSettings(new)
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
                                        val new = appSettings.copy(showCharts = it)
                                        appSettingsVieModel.updateSettings(new)
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.related_albums)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.related_albums
                                    ),
                                    isChecked = showRelatedAlbums,
                                    onCheckedChange = {
                                        val new = appSettings.copy(showRelatedAlbums = it)
                                        appSettingsVieModel.updateSettings(new)
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.similar_artists)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.similar_artists
                                    ),
                                    isChecked = showSimilarArtists,
                                    onCheckedChange = {
                                        val new = appSettings.copy(showSimilarArtists = it)
                                        appSettingsVieModel.updateSettings(new)
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.playlists_you_might_like)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.playlists_you_might_like
                                    ),
                                    isChecked = showPlaylistMightLike,
                                    onCheckedChange = {
                                        val new = appSettings.copy(showPlaylistMightLike = it)
                                        appSettingsVieModel.updateSettings(new)
                                    }
                                )

                                SwitchSettingEntry(
                                    title = "${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}",
                                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                                        R.string.monthly_playlists
                                    ),
                                    isChecked = showMonthlyPlaylistInQuickPicks,
                                    onCheckedChange = {
                                        val new = appSettings.copy(showMonthlyPlaylistInQuickPicks = it)
                                        appSettingsVieModel.updateSettings(new)
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
