package it.fast4x.riplay.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import it.fast4x.riplay.ui.components.RiPlayAccent
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import androidx.compose.ui.text.font.FontWeight

/**
 * STEP 3h (migliorie visive): TITLE BAR CUSTOM per la finestra senza
 * decorazioni (undecorated = true in main.kt: prima NON c'erano NE'
 * pulsanti NE' zona di trascinamento: l'unico modo per chiudere era
 * Alt+F4 e la finestra non si poteva spostare).
 *
 * Include:
 *  - logo + nome "RiPlay" a sinistra, su una zona DRAGGIBILE
 *    (WindowDraggableArea: trascina la finestra, doppio click =
 *    massimizza/ripristina);
 *  - a destra i tre pulsanti finestra: MINIMIZZA, MASSIMIZZA/RIPRISTINA,
 *    CHIUDI (rosso al passaggio, comportamento standard Windows).
 *
 * La barra e' alta 38dp: e' la striscia piu' in alto della finestra e
 * sta FUORI dallo Scaffold (nel Box di ThreeColumnsApp prima di esso),
 * quindi non tocca la regione interop del browser (che comincia piu'
 * in basso, dentro il pannello player).
 */
@Composable
fun RiPlayWindowTitleBar(
    windowScope: androidx.compose.ui.window.WindowScope?,
    maximized: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentToggle by rememberUpdatedState(onToggleMaximize)

    Row(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFF0A0C0A)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zona trascinabile (logo + titolo + tutto lo spazio libero):
        // dentro WindowDraggableArea il doppio click alterna massimizza/
        // ripristina (i click semplici e i drag restano al gestore finestra)
        val dragContent: @Composable () -> Unit = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { currentToggle() }
                        )
                    }
                    .padding(horizontal = 12.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    colorFilter = ColorFilter.tint(RiPlayAccent),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "RiPlay",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (windowScope != null) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                // WindowDraggableArea e' un'estensione di WindowScope:
                // qui la finestra esiste sempre (windowScope passa da main.kt)
                with(windowScope) {
                    WindowDraggableArea(Modifier.fillMaxSize()) {
                        dragContent()
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxHeight()) { dragContent() }
        }

        WindowControlButton(
            icon = Icons.Outlined.Remove,
            label = "minimizza",
            onClick = onMinimize
        )
        WindowControlButton(
            icon = if (maximized) Icons.Outlined.FilterNone else Icons.Outlined.CropSquare,
            label = if (maximized) "ripristina" else "massimizza",
            onClick = onToggleMaximize
        )
        WindowControlButton(
            icon = Icons.Outlined.Close,
            label = "chiudi",
            danger = true,
            onClick = onClose
        )
    }
}

/**
 * Pulsante finestra stile desktop: tutta l'altezza della barra, sfondo
 * che appare al passaggio (rosso standard #E81123 per la chiusura).
 */
@Composable
private fun WindowControlButton(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    var hover by remember { mutableStateOf(false) }
    val bg = when {
        danger && hover -> Color(0xFFE81123)
        hover -> Color.White.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .fillMaxHeight()
            .width(46.dp)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = if (danger && hover) 1f else 0.72f),
            modifier = Modifier.size(15.dp)
        )
    }
}
