package it.fast4x.riplay.ui

import it.fast4x.riplay.i18n.DesktopI18n

import it.fast4x.riplay.enums.Languages

import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.outlined.Language

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import it.fast4x.riplay.ui.components.RiPlayAccent
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import androidx.compose.ui.text.font.FontWeight
import java.awt.Window

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
 * STEP 3o (dalla prova utente della v32: "il trascinamento sfarfallia
 * nello schermo, praticamente impazzisce"): la formula a ACCUMULO era
 * instabile. Il delta era misurato in coordinate LOCALI (puntatore -
 * posizione di partenza) e sommato a ogni evento, ma la posizione
 * della finestra viene applicata dal sistema con un RITARDO variabile
 * rispetto al flusso degli eventi: se due eventi arrivano prima che il
 * sistema abbia mosso la finestra, il delta locale contiene DUE volte
 * il movimento e il totale applicato cresce oltre il reale; all'evento
 * dopo il sistema "recupera" e il delta locale diventa negativo:
 * oscillazione = sfarfallio. 
 *
 * FIX: la posizione della finestra non si accumula piu': e' una FUNZIONE
 * PURA della posizione ASSOLUTA del mouse sullo schermo
 * (java.awt.MouseInfo, coordinate schermo indipendenti da dove si trova
 * la finestra): windowPos = windowStart + (mouseOra - mouseInizio). Un
 * ritardo del sistema puo' al massimo ritardare un singolo spostamento,
 * ma l'evento successivo ricalcola la posizione GIUSTA da zero: nessun
 * loop di feedback possibile, nessuna deriva, nessun sfarfallio.
 *  - pressione + movimento oltre la soglia = TRASCINAMENTO;
 *  - rilascio SENZA movimento entro l'intervallo del doppio click =
 *    massimizza/ripristina (come prima).
 * Se la finestra e' MASSIMIZZATA il trascinamento non parte (il doppio
 * click continua a ripristinarla), evitando stati strani di AWT.
 * La title bar funziona su QUALSIASI JVM (non dipende da JBR).
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
        // STEP 3n: gestore manuale (drag + doppio click, vedi KDoc sopra)
        val dragContent: @Composable () -> Unit = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
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
            // STEP 3o: trascinamento manuale della finestra (lo scope esiste
            // sempre: windowScope arriva da main.kt). Il doppio click resta
            // qui dentro (rilascio pulito x2): massimizza/ripristina.
            // isMaximized = non trascinare una finestra massimizzata.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .riplayTitleBarDrag(windowScope.window, maximized) { currentToggle() }
            ) {
                dragContent()
            }
        } else {
            Box(Modifier.weight(1f).fillMaxHeight()) { dragContent() }
        }

        LanguageSelectorButton()

        WindowControlButton(
            icon = Icons.Outlined.Remove,
            label = ds("titlebar.minimize"),
            onClick = onMinimize
        )
        WindowControlButton(
            icon = if (maximized) Icons.Outlined.FilterNone else Icons.Outlined.CropSquare,
            label = if (maximized) ds("titlebar.restore") else ds("titlebar.maximize"),
            onClick = onToggleMaximize
        )
        WindowControlButton(
            icon = Icons.Outlined.Close,
            label = ds("titlebar.close"),
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

/**
 * STEP 3o: gestore del TRASCINAMENTO della finestra dalla title bar
 * (perche' la formula della v32 sfarfallava e il nuovo algoritmo:
 * nel KDoc di testa del file).
 *
 * Algoritmo (NESSUN accumulo, NESSUN feedback):
 *  - pressione (down NON consumato: nessun conflitto con altri gesture
 *    detector della barra);
 *  - movimento oltre la soglia (touchSlop) = inizio trascinamento: SI
 *    FISSANO la posizione della finestra (windowStart) e la posizione
 *    ASSOLUTA del mouse sullo schermo (screenStart, java.awt.MouseInfo);
 *  - a ogni evento la finestra viene posta a
 *    windowStart + (mouseOra - screenStart): la destinazione dipende SOLO
 *    da dove si trova il mouse, mai da quanto ne e' gia' stato applicato,
 *    quindi un ritardo del sistema non puo' amplificare nulla (l'evento
 *    dopo ricalcola tutto da zero);
 *  - MouseInfo non disponibile (caso limite) = l'evento viene ignorato:
 *    il successivo ricalcola la posizione corretta;
 *  - rilascio senza trascinamento entro l'intervallo del doppio click =
 *    secondo click di un doppio click -> [onDoubleTap] (massimizza/
 *    ripristina). Un solo click non fa nulla (comportamento standard).
 *
 * [isMaximized] TRUE (finestra massimizzata) = il trascinamento non
 * parte: il doppio click resta l'unica azione (ripristina), come la
 * prudenza di AWT suggerisce (spostare una finestra massimizzata porta
 * a stati indefiniti dell'extendedState).
 */
@Composable
private fun Modifier.riplayTitleBarDrag(
    window: Window,
    isMaximized: Boolean,
    onDoubleTap: () -> Unit
): Modifier {
    // sempre aggiornato all'ultimo lambda ricevuto, senza riavviare il
    // pointerInput (le chiavi restano solo sulla finestra);
    // isMaximized via rememberUpdatedState: letto al GESTO, non in
    // composizione (evita di ricreare il pointerInput a ogni massimizza)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentMaximized by rememberUpdatedState(isMaximized)
    return pointerInput(window) {
        // ora dell'ultimo tap "pulito": persiste tra i gesti (dichiarata
        // fuori da awaitEachGesture, dentro il pointerInput che vive per
        // tutta la vita del composable)
        var lastTapTime = 0L
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            var moved = false
            var screenStart: java.awt.Point? = null
            var windowStart: java.awt.Point? = null
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) {
                    // rilascio: click "pulito" (nessun trascinamento) =>
                    // riconosci il doppio click (2 click entro l'intervallo)
                    if (!moved) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime <= viewConfiguration.doubleTapTimeoutMillis) {
                            lastTapTime = 0L
                            currentDoubleTap()
                        } else {
                            lastTapTime = now
                        }
                    }
                    break
                }
                val delta = change.position - down.position
                if (!moved) {
                    if (delta.getDistance() > slop && !currentMaximized) {
                        moved = true
                        // fissati UNA sola volta all'inizio del drag:
                        // coordinate SCHERMO (assolute) del mouse + posizione
                        // corrente della finestra. Tutto il resto e' funzione
                        // pura di questi quattro numeri.
                        screenStart = java.awt.MouseInfo.getPointerInfo()?.location
                        windowStart = window.location
                    }
                }
                if (moved) {
                    val mouse = java.awt.MouseInfo.getPointerInfo()?.location
                    val s0 = screenStart
                    val w0 = windowStart
                    if (mouse != null && s0 != null && w0 != null) {
                        window.setLocation(
                            w0.x + (mouse.x - s0.x),
                            w0.y + (mouse.y - s0.y)
                        )
                    }
                    change.consume()
                }
            }
        }
    }
}


/**
 * STEP 3s (richiesta utente): SELETTORE LINGUA della title bar (icona globo).
 * Elenco = le STESSE lingue della versione Android (Languages enum, voci con
 * il proprio nome nativo) + "lingua di sistema". Il cambio e' IMMEDIATO
 * (DesktopI18n.language e' uno state Compose: tutta la UI si ricompone) e
 * viene PERSISTITO in ~/.riplay/app.properties (ui.language).
 */
@Composable
private fun LanguageSelectorButton() {
    var open by remember { mutableStateOf(false) }
    val current = DesktopI18n.languageState.value
    Box {
        Box(
            Modifier
                .fillMaxHeight()
                .width(38.dp)
                .background(if (open) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                .clickable { open = !open },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Language,
                contentDescription = ds("settings.language"),
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(15.dp)
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.height(380.dp)
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(
                        ds("settings.system") + " (" + ds("settings.language") + ")",
                        color = if (current == Languages.System) RiPlayAccent else Color.White,
                        fontSize = 13.sp
                    )
                },
                onClick = {
                    DesktopI18n.setLanguageChoice(Languages.System)
                    open = false
                },
                trailingIcon = {
                    if (current == Languages.System)
                        Icon(Icons.Filled.Check, null, tint = RiPlayAccent, modifier = Modifier.size(14.dp))
                }
            )
            for (lang in Languages.entries) {
                if (lang == Languages.System) continue
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            lang.nativeName,
                            color = if (current == lang) RiPlayAccent else Color.White,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        DesktopI18n.setLanguageChoice(lang)
                        open = false
                    },
                    trailingIcon = {
                        if (current == lang)
                            Icon(Icons.Filled.Check, null, tint = RiPlayAccent, modifier = Modifier.size(14.dp))
                    }
                )
            }
        }
    }
}
