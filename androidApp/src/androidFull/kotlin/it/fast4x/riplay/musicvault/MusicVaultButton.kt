package it.fast4x.riplay.musicvault

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.MusicVaultState
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.isExclusivelyLocal
import timber.log.Timber

/**
 * Pulsante Music Vault.
 *
 * Con la delega al plugin esterno (protocollo "MusicVault")
 *
 * Visibilita': il pulsante esiste solo se Music Vault e' attivo (flag) e se il
 * plugin compatibile e' installato sul dispositivo (check con cache TTL, vedi
 * VaultPluginClient.isPluginAvailable).
 */
@Composable
fun MusicVaultButton(
    song: Song,
    context: Context = LocalContext.current,
    size: Dp = 20.dp
) {
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val musicVaultEnabled = appSettings.musicVaultEnabled
    if (!musicVaultEnabled) return

    // Nessun plugin compatibile installato: niente pulsante (il flag e' comunque
    // nascosto in impostazioni; qui si copre il caso plugin disinstallato dopo)
    if (!isMusicVaultPluginAvailable(context)) return

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

    IconButton(
        onClick = {
            when (state) {
                MusicVaultState.NONE,
                MusicVaultState.FAILED,
                MusicVaultState.FILE_MISSING -> MusicVaultService.enqueue(context, song)
                MusicVaultState.RUNNING,
                MusicVaultState.QUEUED -> MusicVaultService.cancel(context, song.id)
                MusicVaultState.COMPLETED -> {
                    Timber.d("MusicVaultButton COMPLETED > deleteSong ${song.id}")
                    songState?.let { MusicVaultService.deleteSong(context, it) }
                }
            }
        }
    ) {
        when (state) {
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

            MusicVaultState.RUNNING -> Icon(
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
