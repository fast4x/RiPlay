package it.fast4x.riplay.extensions.experimental.musicvalt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.components.themed.DialogTextButton
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun MusicVaultDisclaimerDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val typography = typography()
    val colorPalette = colorPalette()

    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = colorPalette.background1,
        title = {
            Text(text = "Before you use Music Vault", style = typography.xl.semiBold)
        },
        text = {
            Column {
                Text(
                    text = "Music Vault lets you save audio from YouTube solely for personal, offline listening.",
                    style = typography.m
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Please be aware that:",
                    style = typography.s.semiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "Downloading YouTube content without a Premium subscription may violate YouTube's Terms of Service.",
                    "Saved content is for personal use only. Redistribution or commercial use is not permitted.",
                    "This feature is provided as-is. The developer is not responsible for any misuse."
                ).forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•  ",
                            style = typography.xs,
                            color = colorPalette.text
                        )
                        Text(
                            text = item,
                            style = typography.xs,
                            color = colorPalette.text
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "By continuing, you confirm that you will use this feature responsibly and solely for personal use.",
                    style = typography.s.semiBold,
                    color = colorPalette.text
                )
            }
        },
        confirmButton = {
            DialogTextButton(
                text = "I Understand",
                onClick = onAccept
            )
        },
        dismissButton = {
            DialogTextButton(
                text = "Decline",
                onClick = onDecline
            )
        }
    )
}