package it.fast4x.riplay.ui.screens.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.NavigationBarPosition
import it.fast4x.riplay.enums.WidgetAccentColor
import it.fast4x.riplay.enums.WidgetBackground
import it.fast4x.riplay.enums.WidgetBadgePosition
import it.fast4x.riplay.enums.WidgetTheme
import it.fast4x.riplay.extensions.appsettings.models.AppSettings
import it.fast4x.riplay.ui.components.themed.Search
import it.fast4x.riplay.ui.components.themed.HeaderWithIcon
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.LocalAppearance
import it.fast4x.riplay.ui.widgets.WidgetUpdater
import kotlinx.coroutines.launch
import it.fast4x.riplay.ui.components.themed.settingsSearchBarItem
import it.fast4x.riplay.ui.components.themed.settingsHeaderItem
import it.fast4x.riplay.ui.components.themed.settingsGroupHeader
import it.fast4x.riplay.ui.components.themed.settingsGroupCard

/**
 * Sezione Impostazioni > Widgets.
 *
 * Consente di configurare l'aspetto di tutti i widget RiPlay senza
 * rimuoverli e riaggiungerli dalla home:
 *  - tema (Sistema, Chiaro, Scuro, Nero puro OLED);
 *  - colore d'accento (barra di avanzamento e cuore Like pieno);
 *  - sfondo (solido, semi-trasparente, trasparente);
 *  - posizione del badge dei brani in coda (angolo del widget);
 *  - mostrare/nascondere la copertina;
 *  - mostrare/nascondere i pulsanti Like e Shuffle;
 *  - mostrare/nascondere la barra di avanzamento (widget orizzontale e grande);
 *  - forzare un aggiornamento immediato di tutti i widget aggiunti.
 *
 * Ogni modifica ripubblica subito lo stato nei widget tramite [WidgetUpdater].
 */
@androidx.annotation.OptIn(UnstableApi::class)
@ExperimentalAnimationApi
@Composable
fun WidgetsSettings(
    navController: NavController
) {
    val context = LocalContext.current
    val search = Search.init(alwaysVisible = true)
    val (colorPalette, _, _) = LocalAppearance.current
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value
    val coroutineScope = rememberCoroutineScope()

    val navigationBarPosition = appSettings.navigationBarPosition

    fun updateWidgetSettings(transform: (AppSettings) -> AppSettings) {
        coroutineScope.launch {
            appSettingsManager.updateSettings(transform(appSettingsManager.activeSettings.value))
        }
    }

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer)
        ) {
            settingsHeaderItem {
                HeaderWithIcon(
                    title = stringResource(R.string.tab_widgets),
                    iconId = R.drawable.grid_view,
                    enabled = false,
                    showIcon = true,
                    modifier = Modifier,
                    onClick = {},
                    navController = navController
                )
            }

            settingsSearchBarItem {
                search.SearchBar(this)
            }



            if (search.matchesAny(R.string.settings_widgets_appearance, R.string.settings_widgets_theme, R.string.settings_widgets_theme_desc, R.string.settings_widgets_theme_system, R.string.settings_widgets_theme_light, R.string.settings_widgets_theme_dark, R.string.settings_widgets_theme_black, R.string.settings_widgets_accent, R.string.settings_widgets_accent_desc, R.string.settings_widgets_accent_default, R.string.settings_widgets_accent_orange, R.string.settings_widgets_accent_red, R.string.settings_widgets_accent_pink, R.string.settings_widgets_accent_purple, R.string.settings_widgets_accent_blue, R.string.settings_widgets_accent_teal, R.string.settings_widgets_accent_green, R.string.settings_widgets_background, R.string.settings_widgets_background_desc, R.string.settings_widgets_background_solid, R.string.settings_widgets_background_semi, R.string.settings_widgets_background_transparent, R.string.settings_widgets_badge, R.string.settings_widgets_badge_desc, R.string.settings_widgets_badge_top_start, R.string.settings_widgets_badge_top_end, R.string.settings_widgets_badge_bottom_start, R.string.settings_widgets_badge_bottom_end, R.string.settings_widgets_show_artwork, R.string.settings_widgets_show_artwork_desc, R.string.settings_widgets_show_like, R.string.settings_widgets_show_like_desc, R.string.settings_widgets_show_shuffle, R.string.settings_widgets_show_shuffle_desc, R.string.settings_widgets_show_progress, R.string.settings_widgets_show_progress_desc)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_widgets_appearance))


                settingsGroupCard {

                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.settings_widgets_theme),
                        text = stringResource(R.string.settings_widgets_theme_desc),
                        selectedValue = appSettings.widgetTheme,
                        onValueSelected = { value ->
                            updateWidgetSettings { it.copy(widgetTheme = value) }
                        },
                        valueText = { theme ->
                            stringResource(
                                when (theme) {
                                    WidgetTheme.System -> R.string.settings_widgets_theme_system
                                    WidgetTheme.Light -> R.string.settings_widgets_theme_light
                                    WidgetTheme.Dark -> R.string.settings_widgets_theme_dark
                                    WidgetTheme.PureBlack -> R.string.settings_widgets_theme_black
                                }
                            )
                        }, icon = R.drawable.color_palette,
                    )
            



                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.settings_widgets_accent),
                        text = stringResource(R.string.settings_widgets_accent_desc),
                        selectedValue = appSettings.widgetAccentColor,
                        onValueSelected = { value ->
                            updateWidgetSettings { it.copy(widgetAccentColor = value) }
                        },
                        valueText = { accent ->
                            stringResource(
                                when (accent) {
                                    WidgetAccentColor.Default -> R.string.settings_widgets_accent_default
                                    WidgetAccentColor.Orange -> R.string.settings_widgets_accent_orange
                                    WidgetAccentColor.Red -> R.string.settings_widgets_accent_red
                                    WidgetAccentColor.Pink -> R.string.settings_widgets_accent_pink
                                    WidgetAccentColor.Purple -> R.string.settings_widgets_accent_purple
                                    WidgetAccentColor.Blue -> R.string.settings_widgets_accent_blue
                                    WidgetAccentColor.Teal -> R.string.settings_widgets_accent_teal
                                    WidgetAccentColor.Green -> R.string.settings_widgets_accent_green
                                }
                            )
                        }, icon = R.drawable.color_palette,
                    )
            



                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.settings_widgets_background),
                        text = stringResource(R.string.settings_widgets_background_desc),
                        selectedValue = appSettings.widgetBackground,
                        onValueSelected = { value ->
                            updateWidgetSettings { it.copy(widgetBackground = value) }
                        },
                        valueText = { background ->
                            stringResource(
                                when (background) {
                                    WidgetBackground.Solid -> R.string.settings_widgets_background_solid
                                    WidgetBackground.SemiTransparent -> R.string.settings_widgets_background_semi
                                    WidgetBackground.Transparent -> R.string.settings_widgets_background_transparent
                                }
                            )
                        }, icon = R.drawable.color_palette,
                    )
            



                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.settings_widgets_badge),
                        text = stringResource(R.string.settings_widgets_badge_desc),
                        selectedValue = appSettings.widgetBadgePosition,
                        onValueSelected = { value ->
                            updateWidgetSettings { it.copy(widgetBadgePosition = value) }
                        },
                        valueText = { position ->
                            stringResource(
                                when (position) {
                                    WidgetBadgePosition.TopStart -> R.string.settings_widgets_badge_top_start
                                    WidgetBadgePosition.TopEnd -> R.string.settings_widgets_badge_top_end
                                    WidgetBadgePosition.BottomStart -> R.string.settings_widgets_badge_bottom_start
                                    WidgetBadgePosition.BottomEnd -> R.string.settings_widgets_badge_bottom_end
                                }
                            )
                        }, icon = R.drawable.star,
                    )
            



                    SwitchSettingEntry(
                        title = stringResource(R.string.settings_widgets_show_artwork),
                        text = stringResource(R.string.settings_widgets_show_artwork_desc),
                        isChecked = appSettings.widgetShowArtwork,
                        onCheckedChange = { value ->
                            updateWidgetSettings { it.copy(widgetShowArtwork = value) }
                        }, icon = R.drawable.picture,
                    )
            



                    SwitchSettingEntry(
                        title = stringResource(R.string.settings_widgets_show_like),
                        text = stringResource(R.string.settings_widgets_show_like_desc),
                        isChecked = appSettings.widgetShowLikeButton,
                        onCheckedChange = { value ->
                            updateWidgetSettings { it.copy(widgetShowLikeButton = value) }
                        }, icon = R.drawable.heart,
                    )
            



                    SwitchSettingEntry(
                        title = stringResource(R.string.settings_widgets_show_shuffle),
                        text = stringResource(R.string.settings_widgets_show_shuffle_desc),
                        isChecked = appSettings.widgetShowShuffleButton,
                        onCheckedChange = { value ->
                            updateWidgetSettings { it.copy(widgetShowShuffleButton = value) }
                        }, icon = R.drawable.shuffle,
                    )
            



                    SwitchSettingEntry(
                        title = stringResource(R.string.settings_widgets_show_progress),
                        text = stringResource(R.string.settings_widgets_show_progress_desc),
                        isChecked = appSettings.widgetShowProgress,
                        onCheckedChange = { value ->
                            updateWidgetSettings { it.copy(widgetShowProgress = value) }
                        }, icon = R.drawable.horizontal_bold_line,
                    )
            
                }
            }


            if (search.matchesAny(R.string.settings_widgets_maintenance, R.string.settings_widgets_refresh_now, R.string.settings_widgets_refresh_now_desc, R.string.settings_widgets_info, R.string.settings_widgets_info_transparent, R.string.settings_widgets_badge_note)) {
                settingsGroupHeader(title = context.resources.getString(R.string.settings_widgets_maintenance))


                settingsGroupCard {

                    ButtonBarSettingEntry(
                        title = stringResource(R.string.settings_widgets_refresh_now),
                        text = stringResource(R.string.settings_widgets_refresh_now_desc),
                        icon = R.drawable.grid_view,
                        onClick = {
                            WidgetUpdater.forceRefresh()
                        }
                    )
            



                    SettingsDescription(
                        text = stringResource(R.string.settings_widgets_info)
                    )
            



                    SettingsDescription(
                        text = stringResource(R.string.settings_widgets_info_transparent)
                    )
            



                    SettingsDescription(
                        text = stringResource(R.string.settings_widgets_badge_note)
                    )
            

            }
            }
    }
        }
}
