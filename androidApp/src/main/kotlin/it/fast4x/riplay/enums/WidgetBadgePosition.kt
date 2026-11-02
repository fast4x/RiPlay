package it.fast4x.riplay.enums

/**
 * Angolo del widget in cui compare il badge dei brani in coda;
 * il logo RiPlay occupa automaticamente l'angolo diagonale opposto,
 * così i due badge non si sovrappongono mai.
 * Nel widget Mini 2x2 l'impostazione non si applica: il badge resta
 * nella riga superiore, tra i pulsanti Shuffle e Like.
 */
enum class WidgetBadgePosition {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd
}
