package it.fast4x.riplay.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.Image
import androidx.glance.ImageProvider
import it.fast4x.riplay.MainActivity

/**
 * Widget mini 2x2 "Solo controlli": sfrutta la forma quadrata per disporre
 * i comandi su due righe equilibrate:
 *  - riga superiore: shuffle a sinistra, badge dei brani in coda al centro
 *    (solo con più di un brano) e like a destra, in base alle impostazioni;
 *  - riga inferiore: previous, play/pause e next distribuiti su tutta
 *    la larghezza;
 *  - in basso, il piccolo logo RiPlay come firma dell'app.
 * Nessuna copertina e nessun testo: solo comandi rapidi, il tocco sullo
 * sfondo apre l'app.
 */
class PlayerMiniWidget : RiPlayGlanceWidget() {

    @Composable
    override fun Content(
        palette: WidgetPalette,
        data: WidgetData,
        cover: ImageProvider,
    ) {
        val dark = palette.isDark
        val queue = queueLabel(data)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Riga secondaria: shuffle, eventuale coda al centro e like.
                // Gli spazi a peso vengono inseriti solo tra elementi presenti,
                // così ciò che resta è sempre centrato o distribuito ai poli.
                if (data.showShuffleButton || data.showLikeButton || queue != null) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (data.showShuffleButton) {
                            Image(
                                provider = ImageProvider(
                                    WidgetIcons.shuffle(dark, data.snapshot.shuffleEnabled)
                                ),
                                contentDescription = "shuffle",
                                modifier = GlanceModifier
                                    .size(22.dp)
                                    .clickable(actionRunCallback<ToggleShuffleAction>())
                            )
                        }
                        if (data.showShuffleButton && (queue != null || data.showLikeButton)) {
                            Spacer(modifier = GlanceModifier.defaultWeight())
                        }
                        if (queue != null) {
                            QueueChip(label = queue)
                        }
                        if (queue != null && data.showLikeButton) {
                            Spacer(modifier = GlanceModifier.defaultWeight())
                        }
                        if (data.showLikeButton) {
                            Image(
                                provider = ImageProvider(
                                    WidgetIcons.heart(
                                        data.snapshot.likedState,
                                        dark,
                                        data.accentName
                                    )
                                ),
                                contentDescription = "like",
                                modifier = GlanceModifier
                                    .size(22.dp)
                                    .clickable(actionRunCallback<ToggleLikeAction>())
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))
                }

                // Riga di trasporto: previous, play/pause e next distribuiti
                // sui due spazi di peso, così riempiono la larghezza del widget.
                // Le dimensioni restano compatte (24/34 dp) per non sforare
                // nel 2x2 al minimo: il padding base è già 12.dp per lato.
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(WidgetIcons.previous(dark)),
                        contentDescription = "previous",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(actionRunCallback<PreviousAction>())
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(
                            if (data.snapshot.isPlaying) WidgetIcons.pause(dark)
                            else WidgetIcons.play(dark)
                        ),
                        contentDescription = "play/pause",
                        modifier = GlanceModifier
                            .size(34.dp)
                            .clickable(
                                if (data.snapshot.isPlaying) actionRunCallback<PauseAction>()
                                else actionRunCallback<PlayAction>()
                            )
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(WidgetIcons.next(dark)),
                        contentDescription = "next",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(actionRunCallback<NextAction>())
                    )
                }

                // Firma dell'app: piccolo logo centrato sotto la pulsantiera,
                // dimensioni ridotte per stare nel 2x2 al minimo.
                Spacer(modifier = GlanceModifier.height(4.dp))
                Box(
                    modifier = GlanceModifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LogoChip(size = 12.dp)
                }
            }
        }
    }

    companion object {
        /** Istanza condivisa usata anche da [PlayerMiniWidgetReceiver] e [WidgetUpdater]. */
        val instance = PlayerMiniWidget()
    }
}

class PlayerMiniWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerMiniWidget.instance
}
