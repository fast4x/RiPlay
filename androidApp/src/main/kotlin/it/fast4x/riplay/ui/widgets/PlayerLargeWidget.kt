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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import it.fast4x.riplay.MainActivity

/**
 * Widget grande 4x3: copertina grande, testi, badge della coda,
 * barra di avanzamento live (aggiornata a ogni secondo dal [WidgetUpdater])
 * e pulsantiera completa. È il widget "da media center" per la schermata home.
 */
class PlayerLargeWidget : RiPlayGlanceWidget() {

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
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                // Copertina con logo RiPlay e badge coda negli angoli scelti
                // nelle impostazioni (di default logo in basso a sinistra)
                Box(
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = cover,
                        contentDescription = "cover",
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(12.dp)
                            .clickable(actionStartActivity<MainActivity>())
                    )

                    CornerBadges(data, cornerPadding = 8.dp, logoSize = 18.dp)
                }

                Spacer(modifier = GlanceModifier.width(14.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MediaText(
                        palette = palette,
                        data = data,
                        titleSize = 16.sp,
                        artistSize = 13.sp,
                        centerAlign = true
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    ControlRow(
                        palette = palette,
                        data = data,
                        iconSize = 22.dp,
                        playIconSize = 32.dp,
                        spacing = 12.dp
                    )
                }
            }

            if (data.showProgress) {
                Spacer(modifier = GlanceModifier.height(12.dp))
                ProgressSection(palette = palette, data = data)
            }
        }
    }

    companion object {
        /** Istanza condivisa usata anche da [PlayerLargeWidgetReceiver] e [WidgetUpdater]. */
        val instance = PlayerLargeWidget()
    }
}

class PlayerLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerLargeWidget.instance
}
