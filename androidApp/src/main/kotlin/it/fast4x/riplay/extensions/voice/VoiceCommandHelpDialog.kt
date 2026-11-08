package it.fast4x.riplay.extensions.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.DefaultDialog
import it.fast4x.riplay.ui.components.themed.DialogTextButton
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.color
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

/**
 * Lists the sentences the player understands and lets the user keep the
 * microphone open while the player screen is visible.
 */
@Composable
fun VoiceCommandHelpDialog(
    isHandsFree: Boolean,
    onHandsFreeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val examples = listOf(
        stringResource(R.string.voice_command_example_play),
        stringResource(R.string.voice_command_example_playlist),
        stringResource(R.string.voice_command_example_pause),
        stringResource(R.string.voice_command_example_skip),
        stringResource(R.string.voice_command_example_replay),
        stringResource(R.string.voice_command_example_loop),
        stringResource(R.string.voice_command_example_shuffle),
    )

    DefaultDialog(onDismiss = onDismiss) {
        BasicText(
            text = stringResource(R.string.voice_command_title),
            style = typography().l.bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BasicText(
            text = stringResource(R.string.voice_command_help_intro),
            style = typography().s.medium,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        examples.forEach { example ->
            BasicText(
                text = "•  $example",
                style = typography().s.medium.color(colorPalette().textSecondary),
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }

        HorizontalDivider(
            color = colorPalette().textDisabled,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(R.string.voice_hands_free),
                    style = typography().s.semiBold
                )
                BasicText(
                    text = stringResource(R.string.voice_hands_free_description),
                    style = typography().xs.medium.color(colorPalette().textSecondary)
                )
            }
            Switch(
                checked = isHandsFree,
                onCheckedChange = onHandsFreeChange,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            DialogTextButton(
                text = stringResource(R.string.voice_command_close),
                primary = true,
                onClick = onDismiss
            )
        }
    }
}
