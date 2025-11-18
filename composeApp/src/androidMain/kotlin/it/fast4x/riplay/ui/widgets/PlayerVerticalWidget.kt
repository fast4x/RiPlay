package it.fast4x.riplay.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import it.fast4x.riplay.cleanPrefix
import androidx.core.graphics.createBitmap

class PlayerVerticalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerVerticalWidget()
}

class PlayerVerticalWidget: GlanceAppWidget() {
    companion object {
        val songTitleKey = stringPreferencesKey("songTitleKey")
        val songArtistKey = stringPreferencesKey("songArtistKey")
        val isPlayingKey = booleanPreferencesKey("isPlayingKey")
        var widgetBitmap: Bitmap? = createBitmap(1, 1)
        lateinit var widgetPlayer: ExoPlayer
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        provideContent {
            GlanceTheme {
                WidgetActiveVerticalContent(context)
            }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun updateInfo(
        context: Context,
        songTitle: String,
        songArtist: String,
        isPlaying: Boolean,
        bitmap: Bitmap?,
        player: ExoPlayer
    ) {

        val glanceId =
            GlanceAppWidgetManager(context).getGlanceIds(PlayerVerticalWidget::class.java).firstOrNull()
                ?: return

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
            preferences.toMutablePreferences().apply {
                this[songTitleKey] = cleanPrefix(songTitle)
                this[songArtistKey] = songArtist
                this[isPlayingKey] = isPlaying
            }
        }

        widgetBitmap = bitmap
        widgetPlayer = player
        PlayerVerticalWidget().update(context, glanceId)
    }

}



@OptIn(UnstableApi::class)
@Composable
fun WidgetContent()  {
    /*
    val playerIsActive = !widgetPlayer.currentMediaItem?.mediaMetadata?.title.isNullOrEmpty()
    println("WidgetContent: playerIsActive: $playerIsActive")
    when (playerIsActive) {
        true -> WidgetActiveContent()
        false -> {
            Text(text = "RiPlay Player is not active", modifier = GlanceModifier.padding(12.dp))
        }
    }

     */




}
/*

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
        Text(text = preferences[songTitleKey] ?: "", modifier = GlanceModifier)
        Text(text = preferences[songArtistKey] ?: "", modifier = GlanceModifier)
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
                        widgetPlayer.seekToPrevious()
                    }
            )

            Image(
                provider = ImageProvider(
                    if (preferences[isPlayingKey] == true) {
                        R.drawable.pause
                    } else {
                        R.drawable.play
                    }
                ),
                contentDescription = "play/pause",
                modifier = GlanceModifier.padding(horizontal = 20.dp)
                    .clickable {
                        if (preferences[isPlayingKey] == true) {
                            widgetPlayer.pause()
                        } else {
                            widgetPlayer.play()
                        }
                    }
            )

            Image(
                provider = ImageProvider(R.drawable.play_skip_forward),
                contentDescription = "next",
                modifier = GlanceModifier
                    .clickable {
                        widgetPlayer.seekToNext()
                    }
            )

        }


        Image(
            provider = ImageProvider(widgetBitmap),
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
*/
