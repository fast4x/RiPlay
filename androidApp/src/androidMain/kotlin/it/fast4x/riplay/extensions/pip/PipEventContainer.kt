package it.fast4x.riplay.extensions.pip

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import it.fast4x.riplay.utils.PinchDirection
import it.fast4x.riplay.utils.pinchToToggle

@Composable
fun PipEventContainer(
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    onPipOutAction: () -> Unit = {},
    content: @Composable () -> Unit
){
    val pipHandler = rememberPipHandler()

    Pip(
        numerator = 1,
        denominator = 1,
        modifier = modifier
    ) {
        Box(
            modifier = modifier
                .pinchToToggle(
                    key = enable,
                    direction = PinchDirection.Out,
                    threshold = 1.05f,
                    onPinch = { onPipOutAction() }
                )
                .pinchToToggle(
                    key = enable,
                    direction = PinchDirection.In,
                    threshold = .95f,
                    onPinch = { pipHandler.enterPictureInPictureMode() }
                )

        ) {
            content()
        }
    }
}
