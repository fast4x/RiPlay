package it.fast4x.riplay.extensions.qrcodeanalyzer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.R

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier
) {
    // Densità dello schermo per convertire i DP in pixel per la Canvas
    val density = LocalDensity.current

    // Dimensioni dei dettagli
    val cornerLength = with(density) { 50.dp.toPx() }
    val strokeWidth = with(density) { 3.dp.toPx() }
    val cornerRadius = with(density) { 4.dp.toPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            val holeSize = minOf(size.width, size.height) * 0.6f
            val left = (size.width - holeSize) / 2f
            val top = (size.height - holeSize) / 2f
            val right = left + holeSize
            val bottom = top + holeSize

            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = size
            )

            // Lato in alto
            drawLine(Color.White, Offset(left, top), Offset(right, top), strokeWidth)

            // Lato a destra
            drawLine(Color.White, Offset(right, top), Offset(right, bottom), strokeWidth)

            // Lato in basso
            drawLine(Color.White, Offset(right, bottom), Offset(left, bottom), strokeWidth)

            // Lato a sinistra
            drawLine(Color.White, Offset(left, bottom), Offset(left, top), strokeWidth)
        }

        // Testo di aiuto in basso
        Text(
            text = stringResource(R.string.qr_code_frame_the_qr_code),
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
