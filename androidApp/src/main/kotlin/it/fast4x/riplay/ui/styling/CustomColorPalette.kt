package it.fast4x.riplay.ui.styling

import android.content.Context
import androidx.compose.ui.graphics.Color
import it.fast4x.riplay.MainApplication
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.utils.appContext


fun customColorPalette(colorPalette: ColorPalette, context: Context, isSystemInDarkTheme: Boolean, colorPaletteMode: ColorPaletteMode): ColorPalette {
    val appearanceSettingsmanager = (appContext() as MainApplication).appearanceSettingsManager
    val appearanceSettings = appearanceSettingsmanager.activeSettings.value

    val customThemeLight = colorPalette.copy(
        background0 = Color(appearanceSettings.customThemeLight_Background0),
        background1 = Color(appearanceSettings.customThemeLight_Background1),
        background2 = Color(appearanceSettings.customThemeLight_Background2),
        background3 = Color(appearanceSettings.customThemeLight_Background3),
        background4 = Color(appearanceSettings.customThemeLight_Background4),
        text = Color(appearanceSettings.customThemeLight_Text),
        textSecondary = Color(appearanceSettings.customThemeLight_TextSecondary),
        textDisabled = Color(appearanceSettings.customThemeLight_TextDisabled),
        iconButtonPlayer = Color(appearanceSettings.customThemeLight_IconButtonPlayer),
        accent = Color(appearanceSettings.customThemeLight_Accent)
    )

    val customThemeDark = colorPalette.copy(
        background0 = Color(appearanceSettings.customThemeDark_Background0),
        background1 = Color(appearanceSettings.customThemeDark_Background1),
        background2 = Color(appearanceSettings.customThemeDark_Background2),
        background3 = Color(appearanceSettings.customThemeDark_Background3),
        background4 = Color(appearanceSettings.customThemeDark_Background4),
        text = Color(appearanceSettings.customThemeDark_Text),
        textSecondary = Color(appearanceSettings.customThemeDark_TextSecondary),
        textDisabled = Color(appearanceSettings.customThemeDark_TextDisabled),
        iconButtonPlayer = Color(appearanceSettings.customThemeDark_IconButtonPlayer),
        accent = Color(appearanceSettings.customThemeDark_Accent)
    )

    return when (colorPaletteMode) {
        ColorPaletteMode.Dark, ColorPaletteMode.PitchBlack -> customThemeDark
        ColorPaletteMode.Light -> customThemeLight
        ColorPaletteMode.System -> when (isSystemInDarkTheme) {
            true -> customThemeDark
            false -> customThemeLight
        }
    }
}
