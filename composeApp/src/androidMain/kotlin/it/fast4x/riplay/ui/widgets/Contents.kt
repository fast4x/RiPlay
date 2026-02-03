package it.fast4x.riplay.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.R
import it.fast4x.riplay.service.PlayerService
import it.fast4x.riplay.service.PlayerService.Action
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget.Companion.isPlayingKey
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget.Companion.songArtistKey
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget.Companion.songTitleKey
import it.fast4x.riplay.ui.widgets.PlayerHorizontalWidget.Companion.widgetBitmap


@OptIn(UnstableApi::class)
@Composable
fun WidgetActiveVerticalContent(context: Context) {
    val preferences = currentState<Preferences>()
    Column(
        modifier = GlanceModifier.fillMaxWidth()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(4.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = preferences[PlayerVerticalWidget.songTitleKey] ?: "", modifier = GlanceModifier)
        Text(text = preferences[PlayerVerticalWidget.songArtistKey] ?: "", modifier = GlanceModifier)
        //Text(text = "isPlaying: ${preferences[isPlayingKey]}", modifier = GlanceModifier)

        Row(
            modifier = GlanceModifier.fillMaxWidth()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                provider = ImageProvider(R.drawable.play_skip_back),
                contentDescription = "back",
                modifier = GlanceModifier
                    .clickable {
                        PlayerVerticalWidget.widgetPlayer.seekToPrevious()
                    }
            )

            Image(
                provider = ImageProvider(
                    if (preferences[PlayerVerticalWidget.isPlayingKey] == true) {
                        R.drawable.pause
                    } else {
                        R.drawable.play
                    }
                ),
                contentDescription = "play/pause",
                modifier = GlanceModifier.padding(horizontal = 20.dp)
                    .clickable {
                        if (preferences[PlayerVerticalWidget.isPlayingKey] == true) {
                            PlayerVerticalWidget.widgetPlayer.pause()
                        } else {
                            PlayerVerticalWidget.widgetPlayer.play()
                        }
                    }
            )

            Image(
                provider = ImageProvider(R.drawable.play_skip_forward),
                contentDescription = "next",
                modifier = GlanceModifier
                    .clickable {
                        PlayerVerticalWidget.widgetPlayer.seekToNext()
                    }
            )

        }


        Image(
            provider = ImageProvider(PlayerVerticalWidget.widgetBitmap!!),
            contentDescription = "cover",
            modifier = GlanceModifier.padding(horizontal = 5.dp)
                .clickable (
                    onClick = actionStartActivity<MainActivity>()
                    /*
                    onClick = actionStartActivity(
                        Intent( context, MainActivity::class.java)
                            .putExtra("expandPlayerBottomSheet", true)

                    )
                     */
                )

        )


    }

}
