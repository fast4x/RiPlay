package it.fast4x.riplay.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import it.fast4x.riplay.MainActivity

/**
 * Widget compatto 4x1 "Quick Controls": solo pulsantiera (shuffle, previous,
 * play/pause, next, like in base alle impostazioni), senza copertina né testi.
 * Perfetto per chi vuole solo i comandi rapidi occupando una sola riga.
 * Il logo RiPlay e il badge dei brani in coda occupano due angoli opposti,
 * scelti nelle impostazioni (predefiniti: logo in alto a sinistra, coda
 * in basso a destra). Il tocco sullo sfondo apre l'app.
 */
class PlayerCompactWidget : RiPlayGlanceWidget() {

    @Composable
    override fun Content(
        palette: WidgetPalette,
        data: WidgetData,
        cover: ImageProvider,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            ControlRow(
                palette = palette,
                data = data,
                iconSize = 22.dp,
                playIconSize = 30.dp,
                spacing = 18.dp
            )

            // Firma dell'app e brani in coda in due angoli opposti scelti
            // nelle impostazioni; il logo resta visibile anche senza player,
            // perché nel compatto non c'è copertina da duplicare
            CornerBadges(
                data,
                cornerPadding = 6.dp,
                logoSize = 16.dp,
                showLogo = true
            )
        }
    }

    companion object {
        /** Istanza condivisa usata anche da [PlayerCompactWidgetReceiver] e [WidgetUpdater]. */
        val instance = PlayerCompactWidget()
    }
}

class PlayerCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerCompactWidget.instance
}
