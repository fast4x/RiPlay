package it.fast4x.riplay.enums

/**
 * Tema applicato ai widget della home screen.
 * System segue la modalità notte del dispositivo,
 * Light e Dark forzano rispettivamente la palette chiara o scura,
 * PureBlack usa uno sfondo nero assoluto pensato per display OLED.
 */
enum class WidgetTheme {
    System,
    Light,
    Dark,
    PureBlack
}
