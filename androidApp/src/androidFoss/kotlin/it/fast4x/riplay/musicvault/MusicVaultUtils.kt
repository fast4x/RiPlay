package it.fast4x.riplay.musicvault

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.models.Song
import kotlinx.coroutines.CoroutineScope

fun initializeMusicVault(coroutineScope: CoroutineScope, context: Context) = null

// Protocollo MusicVault: il plugin esterno esiste solo nel flavor full.
// Stub richiesti dal main source set per la variante foss.
fun isMusicVaultPluginAvailable(context: Context): Boolean = false

@Composable
fun MusicVaultButton(
    song: Song,
    context: Context = LocalContext.current,
    size: Dp = 20.dp
) {}
