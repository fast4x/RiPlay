package it.fast4x.riplay.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.common.util.UnstableApi
import it.fast4x.riplay.MainActivity
import it.fast4x.riplay.R
import timber.log.Timber

@UnstableApi
class PlayerVerticalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerVerticalWidget()
}

@UnstableApi
class PlayerVerticalWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()

            val title = prefs[stringPreferencesKey("title")] ?: ""
            val artist = prefs[stringPreferencesKey("artist")] ?: ""
            val isPlaying = prefs[booleanPreferencesKey("isPlaying")] == true
            val artworkBase64 = prefs[stringPreferencesKey("artworkBase64")]
            val safeBase64 = if (!artworkBase64.isNullOrEmpty() && artworkBase64.length > 2000) {
                artworkBase64
            } else {
                null
            }

            val coverProvider = if (safeBase64 != null) {
                try {
                    val decodedBytes = Base64.decode(safeBase64, Base64.DEFAULT)
                    val rawBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (rawBitmap != null) {
                        val safeBitmap = rawBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        rawBitmap.recycle()
                        ImageProvider(safeBitmap)
                    } else {
                        ImageProvider(R.drawable.app_icon)
                    }
                } catch (e: Exception) {
                    ImageProvider(R.drawable.app_icon)
                }
            } else {
                ImageProvider(R.drawable.app_icon)
            }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = title,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = artist,
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            // Un colore più "scialbo" per l'artista (dipende dal tuo tema,
                            // ma di solito si usa un colore secondario)
                            //color = GlanceTheme.colors.onSurface.copy(alpha = 0.7f)
                        ),
                        maxLines = 1,
                    )

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
                            modifier = GlanceModifier.clickable(actionRunCallback(PreviousAction::class.java))
                        )

                        Image(
                            provider = ImageProvider(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = "play/pause",
                            modifier = GlanceModifier.padding(horizontal = 20.dp)
                                .clickable(
                                    if (isPlaying) actionRunCallback(PauseAction::class.java)
                                    else actionRunCallback(PlayAction::class.java)
                                )
                        )

                        Image(
                            provider = ImageProvider(R.drawable.play_skip_forward),
                            contentDescription = "next",
                            modifier = GlanceModifier.clickable(actionRunCallback(NextAction::class.java))
                        )
                    }

                    Image(
                        provider = coverProvider,
                        contentDescription = "cover",
                        modifier = GlanceModifier.padding(horizontal = 5.dp)
                            .width(160.dp).height(160.dp)
                            .cornerRadius(8.dp)
                            .clickable(actionStartActivity<MainActivity>())
                    )
                }
            }
        }
    }

}