package it.fast4x.riplay.ui.styling

import android.content.Context
import androidx.compose.ui.graphics.Color
import it.fast4x.riplay.enums.ColorPaletteMode
import it.fast4x.riplay.extensions.preferences.PreferenceKey.COLOR_PALETTE_MODE
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_0
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_1
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_2
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_3
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_BACKGROUND_4
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_ACCENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_ICON_BUTTON_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_TEXT_DISABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_DARK_TEXT_SECONDARY
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_0
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_1
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_2
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_3
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_BACKGROUND_4
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_TEXT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_ACCENT
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_ICON_BUTTON_PLAYER
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_TEXT_DISABLED
import it.fast4x.riplay.extensions.preferences.PreferenceKey.CUSTOM_THEME_LIGHT_TEXT_SECONDARY
import it.fast4x.riplay.extensions.preferences.getEnum
import it.fast4x.riplay.extensions.preferences.preferences


fun customColorPalette(colorPalette: ColorPalette, context: Context, isSystemInDarkTheme: Boolean): ColorPalette {
    val colorPaletteMode = context.preferences.getEnum(COLOR_PALETTE_MODE.key, ColorPaletteMode.Dark)

    val customThemeLight = colorPalette.copy(
        background0 = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_BACKGROUND_0.key, DefaultLightColorPalette.background0.hashCode())),
        background1 = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_BACKGROUND_1.key, DefaultLightColorPalette.background1.hashCode())),
        background2 = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_BACKGROUND_2.key, DefaultLightColorPalette.background2.hashCode())),
        background3 = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_BACKGROUND_3.key, DefaultLightColorPalette.background3.hashCode())),
        background4 = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_BACKGROUND_4.key, DefaultLightColorPalette.background4.hashCode())),
        text = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_TEXT.key, DefaultLightColorPalette.text.hashCode())),
        textSecondary = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_TEXT_SECONDARY.key, DefaultLightColorPalette.textSecondary.hashCode())),
        textDisabled = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_TEXT_DISABLED.key, DefaultLightColorPalette.textDisabled.hashCode())),
        iconButtonPlayer = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_ICON_BUTTON_PLAYER.key, DefaultLightColorPalette.iconButtonPlayer.hashCode())),
        accent = Color(context.preferences.getInt(CUSTOM_THEME_LIGHT_ACCENT.key, DefaultLightColorPalette.accent.hashCode()))
    )

    val customThemeDark = colorPalette.copy(
        background0 = Color(context.preferences.getInt(CUSTOM_THEME_DARK_BACKGROUND_0.key, DefaultDarkColorPalette.background0.hashCode())),
        background1 = Color(context.preferences.getInt(CUSTOM_THEME_DARK_BACKGROUND_1.key, DefaultDarkColorPalette.background1.hashCode())),
        background2 = Color(context.preferences.getInt(CUSTOM_THEME_DARK_BACKGROUND_2.key, DefaultDarkColorPalette.background2.hashCode())),
        background3 = Color(context.preferences.getInt(CUSTOM_THEME_DARK_BACKGROUND_3.key, DefaultDarkColorPalette.background3.hashCode())),
        background4 = Color(context.preferences.getInt(CUSTOM_THEME_DARK_BACKGROUND_4.key, DefaultDarkColorPalette.background4.hashCode())),
        text = Color(context.preferences.getInt(CUSTOM_THEME_DARK_TEXT.key, DefaultDarkColorPalette.text.hashCode())),
        textSecondary = Color(context.preferences.getInt(CUSTOM_THEME_DARK_TEXT_SECONDARY.key, DefaultDarkColorPalette.textSecondary.hashCode())),
        textDisabled = Color(context.preferences.getInt(CUSTOM_THEME_DARK_TEXT_DISABLED.key, DefaultDarkColorPalette.textDisabled.hashCode())),
        iconButtonPlayer = Color(context.preferences.getInt(CUSTOM_THEME_DARK_ICON_BUTTON_PLAYER.key, DefaultDarkColorPalette.iconButtonPlayer.hashCode())),
        accent = Color(context.preferences.getInt(CUSTOM_THEME_DARK_ACCENT.key, DefaultDarkColorPalette.accent.hashCode()))
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
