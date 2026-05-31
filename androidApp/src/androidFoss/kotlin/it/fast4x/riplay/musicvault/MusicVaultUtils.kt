package it.fast4x.riplay.musicvault

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.models.Song

fun checkAndStartMusicVault() = null

@Composable
fun MusicVaultFolderSetting() {}

@Composable
fun MusicVaultDisclaimerDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {}

@Composable
fun MusicVaultButton(
    song: Song,
    context: Context = LocalContext.current,
    size: Dp = 20.dp
) {}