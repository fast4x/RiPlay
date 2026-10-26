package it.fast4x.riplay.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * STEP 3o (richiesta utente n.2 "modificare la pagina di dettaglio di
 * artista, album e playlist a quella lato android"): pulsante icona delle
 * RIGHE DI AZIONI delle pagine di dettaglio, come gli HeaderIconButton di
 * Android (riga con Arrangement.SpaceAround sotto il titolo).
 *
 *  - icona bianca morbida quando abilitata, grigia quando disabilitata;
 *  - [accent] = tinta RiPlayAccent (stato attivo: bookmark salvato ecc.);
 *  - [enabled] = false disabilita il click e attenua l'icona (come
 *    HeaderIconButton di Android con lista vuota).
 */
@Composable
fun HeaderActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    accent: Boolean = false,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = when {
                accent -> RiPlayAccent
                enabled -> Color.White.copy(alpha = 0.85f)
                else -> Color.White.copy(alpha = 0.3f)
            },
            modifier = Modifier.size(iconSize)
        )
    }
}
