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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
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
            Text(text = stringResource(R.string.disclaimer_music_vault_title_before_you_use_music_vault), style = typography.xl.semiBold)
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.disclaimer_music_vault_info_lets_you_save_audio_from_youtube_solely_for_personal_offline_listening),
                    style = typography.m
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.disclaimer_music_vault_info1_please_be_aware_that),
                    style = typography.s.semiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    stringResource(R.string.disclaimer_music_vault_info2_downloading_youtube_content_without_a_premium_subscription_may_violate_youtube_s_terms_of_service),
                    stringResource(R.string.disclaimer_music_vault_info3_saved_content_is_for_personal_use_only_redistribution_or_commercial_use_is_not_permitted),
                    stringResource(R.string.disclaimer_music_vault_info4_this_feature_is_provided_as_is_the_developer_is_not_responsible_for_any_misuse)
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
                    text = stringResource(R.string.disclaimer_music_vault_info5_by_continuing_you_confirm_that_you_will_use_this_feature_responsibly_and_solely_for_personal_use),
                    style = typography.s.semiBold,
                    color = colorPalette.text
                )
            }
        },
        confirmButton = {
            DialogTextButton(
                text = stringResource(R.string.disclaimer_music_vault_accept_i_understand),
                onClick = onAccept
            )
        },
        dismissButton = {
            DialogTextButton(
                text = stringResource(R.string.disclaimer_music_vault_decline),
                onClick = onDecline
            )
        }
    )
}