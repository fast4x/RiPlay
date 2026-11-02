package it.fast4x.riplay.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import it.fast4x.riplay.MainActivity

/**
 * Widget verticale: testi in alto, pulsantiera al centro e copertina
 * che occupa tutto lo spazio residuo. Il tocco sulla copertina apre l'app.
 * Sulla copertina compaiono il logo RiPlay e il badge dei brani in coda,
 * negli angoli scelti nelle impostazioni (predefiniti: logo in basso a
 * sinistra, coda in basso a destra).
 */
class PlayerVerticalWidget : RiPlayGlanceWidget() {

    @Composable
    override fun Content(
        palette: WidgetPalette,
        data: WidgetData,
        cover: ImageProvider,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MediaText(
                palette = palette,
                data = data,
                titleSize = 14.sp,
                artistSize = 12.sp,
                centerAlign = true
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            ControlRow(
                palette = palette,
                data = data,
                iconSize = 20.dp,
                playIconSize = 28.dp,
                spacing = 12.dp
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Copertina a peso pieno con logo e badge coda sovrapposti negli
            // angoli scelti: in questa zona non ci sono testi né barra di
            // avanzamento, quindi i badge non coprono mai informazioni.
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                Image(
                    provider = cover,
                    contentDescription = "cover",
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                )

                CornerBadges(data, cornerPadding = 6.dp, logoSize = 16.dp)
            }
        }
    }

    companion object {
        /** Istanza condivisa usata anche da [PlayerVerticalWidgetReceiver] e [WidgetUpdater]. */
        val instance = PlayerVerticalWidget()
    }
}

class PlayerVerticalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerVerticalWidget.instance
}
