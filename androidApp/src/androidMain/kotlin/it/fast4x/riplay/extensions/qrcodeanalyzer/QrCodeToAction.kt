package it.fast4x.riplay.extensions.qrcodeanalyzer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.environment.Environment
import it.fast4x.environment.requests.song
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.enums.PopupType
import it.fast4x.riplay.extensions.preferences.PreferenceKey.PARENTAL_CONTROL_ENABLED
import it.fast4x.riplay.extensions.preferences.preferences
import it.fast4x.riplay.services.playback.PlayerService
import it.fast4x.riplay.ui.components.themed.SmartMessage
import it.fast4x.riplay.utils.LOCAL_KEY_PREFIX
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.forcePlay
import it.fast4x.riplay.utils.isExplicit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(UnstableApi::class)
suspend fun qrCodeToAction(content: String, context: Context, binder: PlayerService.Binder, navController: NavController){
    if (content.isNotEmpty()) {
        val parts = content.split(":")

        if (parts.size >= 3) {
            val action = parts[1]
            val isLocal = parts[2] == LOCAL_KEY_PREFIX
            val mediaId = if (isLocal) parts[3] else parts[2]

            Timber.d("MainActivity LaunchedEffect intentUriData scheme riplay parts = $parts")
            when(action) {
                "play" -> {
                    val mediaItem = if (!isLocal)
                        Environment.song(mediaId)?.getOrNull()?.asMediaItem
                    else Database.songDao().getById(mediaId)?.asMediaItem

                    mediaItem?.let { media ->
                        withContext(Dispatchers.Main) {
                            if (!media.isExplicit && !context.preferences.getBoolean(
                                    PARENTAL_CONTROL_ENABLED.key,
                                    false
                                )
                            )
                                binder.player.forcePlay(media)
                            else
                                SmartMessage(
                                    "Parental control is enabled",
                                    PopupType.Warning,
                                    context = context
                                )
                        }
                    }
                }
                "artist" -> { navController?.navigate(route = "${NavRoutes.artist.name}/$mediaId")}
                "album" -> { navController?.navigate(route = "${NavRoutes.album.name}/$mediaId")}
                "localPlaylist" -> { navController?.navigate(route = "${NavRoutes.localPlaylist.name}/$mediaId") }
                "playlist" -> { navController?.navigate(route = "${NavRoutes.playlist.name}/$mediaId") }

            }

        }
    }
}