package it.fast4x.riplay.extensions.qrcodeanalyzer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.QrType
import it.fast4x.riplay.extensions.preferences.PreferenceKey
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.ui.components.ActionPillButton
import it.fast4x.riplay.ui.components.LocalGlobalSheetState
import it.fast4x.riplay.ui.components.SheetBody
import it.fast4x.riplay.utils.colorPalette

@Composable
fun GenerateQrButton(
    modifier: Modifier,
    type: QrType = QrType.unknown,
    value: String
) {
    var enabledQrCodeActions by rememberPreference(PreferenceKey.QR_CODE_TO_ACTIONS.key, true)
    if (!enabledQrCodeActions) return

    if (type == QrType.unknown) return

    val sheet = LocalGlobalSheetState.current
    ActionPillButton(
        modifier = modifier,
        icon = R.drawable.qr_code,
        enabled = true,
        color = colorPalette().text,
        onClick = {
            sheet.display {
                SheetBody {
                    GenerateQrScreen("${type.content}$value")
                }
            }
        },
    )
}