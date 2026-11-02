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
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.LocalSize
import it.fast4x.riplay.MainActivity

/**
 * Widget orizzontale: copertina a sinistra, testi e controlli a destra.
 * La copertina si adatta all'altezza reale del widget; se lo spazio
 * orizzontale è sufficiente e l'opzione è attiva nelle impostazioni,
 * mostra anche la barra di avanzamento del brano.
 * I badge logo RiPlay e brani in coda sono sovrapposti agli angoli della
 * copertina scelti nelle impostazioni: così non coprono mai testi,
 * controlli o progress bar.
 */
class PlayerHorizontalWidget : RiPlayGlanceWidget() {

    @Composable
    override fun Content(
        palette: WidgetPalette,
        data: WidgetData,
        cover: ImageProvider,
    ) {
        val size = LocalSize.current
        val showProgressHere = data.showProgress && size.width >= 250.dp
        val coverSize = when {
            size.height >= 130.dp -> 110.dp
            size.height >= 90.dp -> 88.dp
            else -> 64.dp
        }

        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = GlanceModifier.size(coverSize)
            ) {
                Image(
                    provider = cover,
                    contentDescription = "cover",
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                )

                CornerBadges(data, cornerPadding = 4.dp, logoSize = 14.dp, badgeTextSize = 8.sp)
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MediaText(
                    palette = palette,
                    data = data,
                    titleSize = 13.sp,
                    artistSize = 11.sp,
                    centerAlign = true
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                ControlRow(
                    palette = palette,
                    data = data,
                    iconSize = 18.dp,
                    playIconSize = 26.dp,
                    spacing = 10.dp
                )

                if (showProgressHere) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    ProgressSection(palette = palette, data = data)
                }
            }
        }
    }

    companion object {
        /** Istanza condivisa usata anche da [PlayerHorizontalWidgetReceiver] e [WidgetUpdater]. */
        val instance = PlayerHorizontalWidget()
    }
}

class PlayerHorizontalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerHorizontalWidget.instance
}
