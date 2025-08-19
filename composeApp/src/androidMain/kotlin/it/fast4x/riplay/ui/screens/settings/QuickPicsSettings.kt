package it.fast4x.riplay.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.Database
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.PlayEventsType
import it.fast4x.riplay.ui.components.themed.ConfirmationDialog
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.extensions.preferences.enableQuickPicksPageKey
import it.fast4x.riplay.extensions.preferences.playEventsTypeKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.showChartsKey
import it.fast4x.riplay.extensions.preferences.showMonthlyPlaylistInQuickPicksKey
import it.fast4x.riplay.extensions.preferences.showMoodsAndGenresKey
import it.fast4x.riplay.extensions.preferences.showNewAlbumsArtistsKey
import it.fast4x.riplay.extensions.preferences.showNewAlbumsKey
import it.fast4x.riplay.extensions.preferences.showPlaylistMightLikeKey
import it.fast4x.riplay.extensions.preferences.showRelatedAlbumsKey
import it.fast4x.riplay.extensions.preferences.showSimilarArtistsKey
import it.fast4x.riplay.extensions.preferences.showTipsKey
import kotlinx.coroutines.flow.distinctUntilChanged
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.ui.components.themed.settingsItem

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun  QuickPicsSettings() {
    var playEventType by rememberPreference(
        playEventsTypeKey,
        PlayEventsType.MostPlayed
    )
    var showTips by rememberPreference(showTipsKey, true)
    var showRelatedAlbums by rememberPreference(showRelatedAlbumsKey, true)
    var showSimilarArtists by rememberPreference(showSimilarArtistsKey, true)
    var showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    var showNewAlbums by rememberPreference(showNewAlbumsKey, true)
    var showPlaylistMightLike by rememberPreference(showPlaylistMightLikeKey, true)
    var showMoodsAndGenres by rememberPreference(showMoodsAndGenresKey, true)
    var showMonthlyPlaylistInQuickPicks by rememberPreference(showMonthlyPlaylistInQuickPicksKey, true)
    var showCharts by rememberPreference(showChartsKey, true)
    var enableQuickPicksPage by rememberPreference(enableQuickPicksPageKey, true)
    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)
    var clearEvents by remember { mutableStateOf(false) }
    if (clearEvents) {
        ConfirmationDialog(
            text = stringResource(R.string.do_you_really_want_to_delete_all_playback_events),
            onDismiss = { clearEvents = false },
            onConfirm = { Database.asyncTransaction( Database::clearEvents ) }
        )
    }

    //var isEnabledDiscoveryLangCode by rememberPreference(isEnabledDiscoveryLangCodeKey,   true)

    //var showActionsBar by rememberPreference(showActionsBarKey, true)
    Column(
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
            //.verticalScroll(rememberScrollState())
            /*
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )

             */
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
        ) {
            settingsItem {
                HeaderWithIcon(
                    title = if (!isYouTubeLoggedIn()) stringResource(R.string.quick_picks) else stringResource(
                        R.string.home
                    ),
                    iconId = if (!isYouTubeLoggedIn()) R.drawable.sparkles else R.drawable.internet,
                    enabled = false,
                    showIcon = true,
                    modifier = Modifier,
                    onClick = {}
                )
            }

            settingsItem(
                isHeader = true
            ) {
                SettingsEntryGroupText(title = stringResource(R.string.quick_picks))
            }

            settingsItem {
                SwitchSettingEntry(
                    offline = false,
                    title = stringResource(R.string.enable_quick_picks_page),
                    text = "",
                    isChecked = enableQuickPicksPage,
                    onCheckedChange = {
                        enableQuickPicksPage = it
                    }
                )

                //SettingsGroupSpacer()
                /*
                SwitchSettingEntry(
                    title = stringResource(R.string.show_actions_bar),
                    text = "",
                    isChecked = showActionsBar,
                    onCheckedChange = {
                        showActionsBar = it
                    }
                )
                 */

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.tips)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.tips
                    ),
                    isChecked = showTips,
                    onCheckedChange = {
                        showTips = it
                    }
                )

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.charts)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.charts
                    ),
                    isChecked = showCharts,
                    onCheckedChange = {
                        showCharts = it
                    }
                )

                AnimatedVisibility(
                    visible = showTips,
                    enter = fadeIn(tween(100)),
                    exit = fadeOut(tween(100)),
                ) {
                    EnumValueSelectorSettingsEntry(
                        offline = false,
                        title = stringResource(R.string.tips),
                        selectedValue = playEventType,
                        onValueSelected = { playEventType = it },
                        valueText = {
                            when (it) {
                                PlayEventsType.MostPlayed -> stringResource(R.string.by_most_played_song)
                                PlayEventsType.LastPlayed -> stringResource(R.string.by_last_played_song)
                                PlayEventsType.CasualPlayed -> stringResource(R.string.by_casual_played_song)
                            }
                        }
                    )
                }

                //SettingsGroupSpacer()

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.related_albums)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.related_albums
                    ),
                    isChecked = showRelatedAlbums,
                    onCheckedChange = {
                        showRelatedAlbums = it
                    }
                )

                //SettingsGroupSpacer()

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.similar_artists)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.similar_artists
                    ),
                    isChecked = showSimilarArtists,
                    onCheckedChange = {
                        showSimilarArtists = it
                    }
                )


                //SettingsGroupSpacer()

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums_of_your_artists)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.new_albums_of_your_artists
                    ),
                    isChecked = showNewAlbumsArtists,
                    onCheckedChange = {
                        showNewAlbumsArtists = it
                    }
                )

                SwitchSettingEntry(
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.new_albums)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.new_albums
                    ),
                    isChecked = showNewAlbums,
                    onCheckedChange = {
                        showNewAlbums = it
                    }
                )

                //SettingsGroupSpacer()

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.playlists_you_might_like)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.playlists_you_might_like
                    ),
                    isChecked = showPlaylistMightLike,
                    onCheckedChange = {
                        showPlaylistMightLike = it
                    }
                )

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.moods_and_genres)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.moods_and_genres
                    ),
                    isChecked = showMoodsAndGenres,
                    onCheckedChange = {
                        showMoodsAndGenres = it
                    }
                )

                SwitchSettingEntry(
                    offline = false,
                    title = "${stringResource(R.string.show)} ${stringResource(R.string.monthly_playlists)}",
                    text = stringResource(R.string.disable_if_you_do_not_want_to_see) + " " + stringResource(
                        R.string.monthly_playlists
                    ),
                    isChecked = showMonthlyPlaylistInQuickPicks,
                    onCheckedChange = {
                        showMonthlyPlaylistInQuickPicks = it
                    }
                )

                /*
                SwitchSettingEntry(
                    title = stringResource(R.string.enable_language_in_discovery),
                    text = stringResource(R.string.if_possible_allows_discovery_content_language),
                    isChecked = isEnabledDiscoveryLangCode,
                    onCheckedChange = {
                        isEnabledDiscoveryLangCode = it
                    }
                )
                ImportantSettingsDescription(text = stringResource(R.string.restarting_rimusic_is_required))
                 */

                SettingsEntry(
                    offline = false,
                    title = stringResource(R.string.reset_quick_picks),
                    text = if (eventsCount > 0) {
                        stringResource(R.string.delete_playback_events, eventsCount)
                    } else {
                        stringResource(R.string.quick_picks_are_cleared)
                    },
                    isEnabled = eventsCount > 0,
                    onClick = { clearEvents = true }
                )
            }
        }

//        SettingsGroupSpacer(
//            modifier = Modifier.height(Dimensions.bottomSpacer)
//        )
    }
}
