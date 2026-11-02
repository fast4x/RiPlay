package it.fast4x.riplay.ui.components.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.utils.colorPalette

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Framework di navigazione per Android TV (D-pad / telecomando)
 * ═══════════════════════════════════════════════════════════════════
 *
 *  Fornisce:
 *   - [LocalIsTv]: true quando l'app sta girando con l'interfaccia TV.
 *     Viene fornito una sola volta in MainActivity (StartApp) e letto
 *     dai componenti per attivare i comportamenti D-pad.
 *
 *   - [LocalTvNavFocusSignal]: contatore di "richieste di rifocus".
 *     Quando l'Activity rileva una pressione del D-pad senza alcun
 *     nodo in focus (stato "morto"), incrementa il segnale; la navbar
 *     lo osserva e riprende il focus sulla prima voce.
 *
 *   - [Modifier.tvFocusRing]: indicatore visuale universale del focus
 *     (bordo + alone di sfondo + scala animata). NON rende l'elemento
 *     focusabile: osserva soltanto il focus creato da `clickable`,
 *     `combinedClickable` o `focusable`.
 *
 *  ⚠️ ORDINE DEI MODIFICATORI: `tvFocusRing` DEVE essere applicato
 *  PRIMA di `clickable`/`combinedClickable`/`focusable` nella catena,
 *  altrimenti l'osservatore non riceve gli eventi di focus:
 *
 *      Modifier
 *          .clip(shape)
 *          .tvFocusRing(shape = shape)   // ✅ prima
 *          .clickable { ... }            // crea il focus target
 *
 */

@Stable
val LocalIsTv = staticCompositionLocalOf { false }

/**
 * Segnale (incrementale) per richiedere il focus sulla prima voce della navbar.
 * È un MutableState condiviso: l'Activity lo incrementa, la navbar lo osserva.
 */
@Stable
val LocalTvNavFocusSignal = staticCompositionLocalOf { mutableIntStateOf(0) }

/**
 * Flag di sessione: il focus automatico "all'avvio" sulla navbar deve
 * avvenire una sola volta per processo, non a ogni navigazione.
 */
@Stable
var tvStartupFocusDone = false

/**
 * Stato condiviso: true quando il miniplayer sta EFFETTIVAMENTE disegnando
 * la sua UI (coda attiva). Viene impostato da [UnifiedMiniPlayer] tramite
 * un DisposableEffect e resettato quando esce dalla composizione.
 *
 * Su TV serve ai container (ScreenContainer / PageContainer) per aggiungere
 * un padding sotto le liste: così l'elemento in focus non finisce mai
 * nascosto dietro al miniplayer sovrapposto.
 */
@Stable
val tvMiniPlayerVisible = mutableStateOf(false)

/**
 * Selettore: il miniplayer è mostrato sull'interfaccia TV?
 *
 * Default: **false** — su TV il miniplayer ha poco senso (schermo grande,
 * navigazione a fuoco) e la sua posizione sopra la bottom bar creava un
 * disallineamento visivo. Il player TV si apre direttamente col telecomando:
 *  - tasto **MENU** (o INFO): apre il player da chiuso / apre-chiude la coda nel player
 *  - tasto **verde** (PROG_GREEN): apre la coda direttamente
 *  - tasto **rosso** (PROG_RED): apre/chiude il player
 *
 * Per riattivare il miniplayer su TV basta portare questo flag a `true`.
 */
@Stable
var tvMiniPlayerEnabled = false

/**
 * Contatore di richieste di toggle della CODA TV, provenienti dai tasti
 * globali del telecomando (MainActivity, tasto verde / INFO con player aperto).
 * [it.fast4x.riplay.ui.screens.player.unified.TvUnifiedPlayer] osserva il
 * contatore e apre/chiude l'overlay della coda.
 */
@Stable
val tvQueueToggleRequest = mutableIntStateOf(0)

/**
 * True quando una RIGA della coda ha il focus. Serve al player TV per
 * instradare il tasto MENU: se una riga è focusata, MENU apre il menu
 * contestuale della riga (gestito dalla riga stessa); altrimenti MENU
 * apre/chiude l'overlay della coda.
 */
@Stable
val tvQueueRowFocused = mutableStateOf(false)

/**
 * Indicatore visuale del focus per TV.
 *
 * Disegna un bordo (default: colore accent del tema), un eventuale alone
 * di sfondo e applica una leggera scala quando l'elemento ha il focus.
 * Tutte le transizioni sono animate (120 ms) per un movimento "morbido".
 *
 * Non modifica la semantica del focus: non aggiunge nodi focusabili.
 *
 * @param shape           forma del bordo/alone (passare la stessa forma usata in `clip`)
 * @param focusedScale    scala applicata quando l'elemento è focusato (1f = nessuna scala)
 * @param borderWidth     spessore del bordo di focus
 * @param borderColor     colore del bordo; se null usa colorPalette().accent
 * @param backgroundColor colore dell'alone; se null usa colorPalette().text al 10%
 */
fun Modifier.tvFocusRing(
    shape: Shape = RoundedCornerShape(8.dp),
    focusedScale: Float = 1.03f,
    borderWidth: Dp = 2.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
): Modifier = composed {
    val isFocused = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused.value) focusedScale else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "tvFocusRingScale"
    )
    val ringColor = borderColor ?: colorPalette().accent
    val glowColor = backgroundColor ?: colorPalette().text.copy(alpha = 0.10f)

    this
        // L'osservatore DEVE precedere il focus target (clickable/focusable) nella catena
        .onFocusChanged { isFocused.value = it.isFocused }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .background(color = if (isFocused.value) glowColor else Color.Transparent, shape = shape)
        .border(
            width = if (isFocused.value) borderWidth else 0.dp,
            color = if (isFocused.value) ringColor else Color.Transparent,
            shape = shape
        )
}
