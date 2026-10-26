package it.fast4x.riplay.player.kcef

import it.fast4x.riplay.i18n.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Superficie video YouTube per desktop: incorpora il browser CEF (KCEF)
 * nel layout Compose tramite SwingPanel e mostra lo stato di installazione
 * del bundle (il download ~120-200 MB avviene solo al primo avvio).
 *
 * STEP 3d (logica display Android a livello SWING):
 * il browser CEF in rendering "windowed" e' un componente AWT nativo che
 * sta SEMPRE sopra il contenuto Compose (per questo badge e banner erano
 * coperti). La v21 nascondeva il componente AWT per far vedere la cover
 * Compose: in prova il risultato era un PANNELLO BIANCO (contenitore
 * Swing di interop scoperto) e la riproduzione NON partiva (browser
 * nascosto durante il caricamento). Ora la cover e' disegnata DENTRO il
 * livello Swing ([KcefPlayerHostPanel]: browser SOTTO + canvas SOPRA):
 *  - [showCover] TRUE  -> la canvas copre il browser con la copertina del
 *    brano ([coverUrl]) oppure il segnaposto RiPlay se [coverUrl] e' null;
 *    il browser resta VIVO e VISIBILE sotto di essa (l'audio e il
 *    meccanismo step2r restano identici, come la copertina Android sopra
 *    il player che continua a suonare);
 *  - [errorText] non nullo -> la canvas mostra il messaggio d'errore
 *    YouTube (il vecchio banner Compose era comunque coperto dal browser);
 *  - [overlayOpen] TRUE -> l'intero livello Swing viene nascosto sotto il
 *    player espandibile: accade solo con l'audio GIA' avviato, che
 *    prosegue come la riproduzione in background;
 *  - [onCoverClick] -> la canvas e' cliccabile (apre il player espandibile).
 *
 * STEP 3g (dalla prova utente: "con il player aperto si vede un pannello
 * bianco posto esattamente sopra il browser"): nascondere il pannello
 * ospite NON basta. SwingPanel incasta il componente in un wrapper di
 * interop (SwingInteropViewGroup, un JPanel) che:
 *  - dipinge il PROPRI sfondo di default (bianco/grigio) quando il figlio
 *    e' nascosto -> era il pannello bianco;
 *  - viene RI-MOSTRATO da Compose a ogni pass di layout
 *    (setVisible(!clip.isEmpty)): nasconderlo dall'esterno e' inutile.
 *  In piu' Compose CANCELLA (BlendMode.Clear) i propri pixel nella regione
 *  interop: l'overlay Compose NON puo' apparire li' finche' la regione
 *  interop esiste. La soluzione e' RIDURRE a zero il nodo SwingPanel
 *  mentre l'overlay e' aperto: Compose nasconde da solo il wrapper (clip
 *  vuota) e l'overlay si vede in TUTTA la finestra. Il browser NON viene
 *  staccato dalla gerarchia AWT (niente re-parenting, che in JCEF avviene
 *  una sola volta dopo la creazione e distruggerebbe la finestra nativa):
 *  la finestra nativa resta solo nascosta, lo stato gia' provato dalle
 *  versioni precedenti con l'audio che prosegue.
 */
@Composable
fun KcefVideoSurface(
    modifier: Modifier = Modifier,
    controller: KcefYouTubePlayer,
    showCover: Boolean = false,
    coverUrl: String? = null,
    overlayOpen: Boolean = false,
    errorText: String? = null,
    onCoverClick: (() -> Unit)? = null
) {
    val kcefState by KcefEngine.state.collectAsState()

    // Sicurezza: se main() non l'ha gia' fatto, avvia il motore (idempotente)
    LaunchedEffect(Unit) { KcefEngine.start() }

    when (val s = kcefState) {
        is KcefState.Ready -> Box(modifier) {
            SwingPanel(
                factory = { controller.attachBrowser() },
                update = { component ->
                    // Il wrapper di interop (SwingInteropViewGroup) non deve
                    // MAI mostrare il proprio sfondo di default: se per un
                    // frame restasse visibile con il figlio nascosto, si
                    // vede NERO (sfondo dell'app) e non bianco.
                    (component.parent as? javax.swing.JPanel)?.background =
                            java.awt.Color.BLACK
                    // L'intero livello Swing scompare SOLO sotto l'overlay
                    // espanso (audio gia' in corso, che prosegue)
                    component.isVisible = !overlayOpen
                    (component as? KcefPlayerHostPanel)?.apply {
                        showCover(showCover, coverUrl)
                        setErrorText(errorText)
                        setOnCoverClick(onCoverClick)
                    }
                },
                // STEP 3g: con l'overlay aperto il nodo SwingPanel si riduce
                // a ZERO: Compose nasconde da solo il wrapper di interop
                // (setVisible(false) su clip vuota) e l'overlay a tutto
                // schermo si vede anche nella regione del browser. Il
                // browser resta nella gerarchia AWT: nessun re-parenting,
                // nessun riavvio della riproduzione.
                modifier = if (overlayOpen)
                    Modifier.align(Alignment.TopStart).size(0.dp)
                else
                    Modifier.fillMaxSize()
            )
        }

        is KcefState.Failed -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(ds("kcef.unavailable"), color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                s.message,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                ds("kcef.details_log"),
                color = Color(0xFF8A8A8A),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { KcefEngine.retry() }) {
                Text(ds("kcef.retry"))
            }
        }

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stateLabel(s), color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            if (s is KcefState.Downloading) {
                LinearProgressIndicator(
                    progress = s.percent / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("${s.percent.toInt()}%", color = Color(0xFFB0B0B0))
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (KcefEngine.needsRestartAfterFirstDownload) {
                Spacer(Modifier.height(12.dp))
                Text(
                    ds("kcef.jdk_conflict"),
                    color = Color(0xFFFFC107),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Cover del brano in COMPOSE (usata dal pannello VLCJ, dove i frame sono
 * pixel-buffer Compose e lo z-order normale funziona). Per il pannello
 * KCEF la cover e' invece la Canvas Swing di KcefPlayerHostPanel.
 */
@Composable
internal fun CoverOverlay(coverUrl: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = ds("player.song_cover"),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(6.dp)
        )
    }
}

@Composable
private fun stateLabel(s: KcefState): String = when (s) {
    KcefState.NotStarted, KcefState.Locating -> ds("kcef.preparing")
    is KcefState.Downloading -> ds("kcef.downloading")
    KcefState.Extracting -> ds("kcef.extracting")
    KcefState.Installing -> ds("kcef.installing")
    KcefState.Initializing -> ds("kcef.starting")
    else -> ds("kcef.preparing_short")
}
