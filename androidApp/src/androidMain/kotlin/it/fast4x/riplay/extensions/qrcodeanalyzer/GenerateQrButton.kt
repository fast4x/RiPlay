package it.fast4x.riplay.extensions.qrcodeanalyzer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.fast4x.riplay.LocalAppSettingsManager
import it.fast4x.riplay.R
import it.fast4x.riplay.enums.QrType
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
    val appSettingsManager = LocalAppSettingsManager.current
    val appSettings = appSettingsManager.activeSettings.collectAsStateWithLifecycle().value

    val enabledQrCodeActions = appSettings.qrCodeToActions
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