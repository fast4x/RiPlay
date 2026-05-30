package it.fast4x.riplay.extensions.experimental.musicvalt

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.preferences.PreferenceKey.MUSIC_VAULT_ENABLED
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.isExclusivelyLocal
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.isMusicVault
import timber.log.Timber

@Composable
fun MusicVaultButton(
    song: Song,
    context: Context = LocalContext.current,
    size: Dp = 20.dp
) {

    if (BuildConfig.FLAVOR == "foss") return

    var musicVaultEnabled by rememberPreference(MUSIC_VAULT_ENABLED.key, false)
    if (!musicVaultEnabled) return

    val sizeModifier = Modifier.size(size)

    // Osserva lo stato dal database
    val songState by Database.song(song.id)
        .collectAsState(initial = song)

    if (songState?.isExclusivelyLocal == true) return // Già local quindi il bottone non serve

    val dbState = songState?.musicVaultState ?: MusicVaultState.NONE

    // Verifica file fisico e se lo stato a db è COMPLETED
    val state = remember(dbState, song.musicVaultFileName) {
        when (dbState) {
            MusicVaultState.COMPLETED -> {
                val fileExists = MusicVaultRepository.fileExists(context, songState!!)
                if (!fileExists) MusicVaultState.FILE_MISSING else dbState
            }
            // Per tutti gli altri stati (DOWNLOADING, QUEUED, ecc.)
            else -> dbState
        }
    }

    // Combina stato WorkManager + stato db
    val workManagerState by MusicVaultService.observeState(context, song.id)
        .collectAsState(initial = state)

    // WorkManager ha priorità quando è attivo (DOWNLOADING/QUEUED)
    val workState = when (workManagerState) {
        MusicVaultState.DOWNLOADING,
        MusicVaultState.QUEUED -> workManagerState
        else -> state  // negli altri casi usa lo stato dal db
    }

    IconButton(
        onClick = {
            when (workState) {
                MusicVaultState.NONE,
                MusicVaultState.FAILED,
                MusicVaultState.FILE_MISSING -> MusicVaultService.enqueue(context, song)
                MusicVaultState.DOWNLOADING,
                MusicVaultState.QUEUED -> MusicVaultService.cancel(context, song.id)
                MusicVaultState.COMPLETED -> {
                    Timber.d("MusicVaultButton COMPLETED > deleteSong ${song.id}")
                    songState?.let { MusicVaultService.deleteSong(context, it) }
                }
            }
        }
    ) {
        when (workState) {
            MusicVaultState.NONE -> Icon(
                painter = painterResource(R.drawable.ic_musicvault_none),
                contentDescription = "Salva in MusicVault",
                tint = colorPalette().accent,
                modifier = sizeModifier
            )
            MusicVaultState.QUEUED -> Icon(
                    painter = painterResource(R.drawable.ic_musicvault_queued),
                    contentDescription = "In attesa",
                    tint = colorPalette().accent,
                modifier = sizeModifier
                )

            MusicVaultState.DOWNLOADING -> Icon(
                    painter = painterResource(R.drawable.ic_musicvault_downloading),
                    contentDescription = "Salvo in MusicVault",
                    tint = colorPalette().accent,
                modifier = sizeModifier
                )
            MusicVaultState.COMPLETED -> Icon(
                painter = painterResource(R.drawable.ic_musicvault_completed),
                contentDescription = "Salvato in MusicVault",
                tint = colorPalette().accent,
                modifier = sizeModifier
            )
            MusicVaultState.FILE_MISSING -> Icon(
                painter = painterResource(R.drawable.ic_musicvault_missing),
                contentDescription = "File mancante, clicca per riscaricarlo",
                tint = colorPalette().accent,
                modifier = sizeModifier
            )
            MusicVaultState.FAILED -> Icon(
                painter = painterResource(R.drawable.ic_musicvault_failed),
                contentDescription = "Non è possibile salvarlo in MusicVault, riprova",
                tint = colorPalette().red,
                modifier = sizeModifier
            )
        }
    }
}