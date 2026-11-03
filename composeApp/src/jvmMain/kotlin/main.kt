import androidx.compose.ui.window.application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import it.fast4x.environment.Environment
import it.fast4x.riplay.i18n.DesktopI18n
import it.fast4x.riplay.i18n.ds
import it.fast4x.riplay.getAsyncImageLoader
import it.fast4x.riplay.ui.ThreeColumnsApp
import it.fast4x.riplay.ui.initialWindowState
import it.fast4x.riplay.ui.saveDesktopWindowPrefs
import it.fast4x.riplay.ui.theme.DesktopTheme
import it.fast4x.riplay.commonutils.initializeEnvironment
import it.fast4x.riplay.player.kcef.KcefEngine
import it.fast4x.riplay.player.kcef.KcefState
import it.fast4x.riplay.utils.DesktopAppInfo
import it.fast4x.riplay.utils.DesktopCloseToTray
import it.fast4x.riplay.utils.DesktopMiniPlayer
import it.fast4x.riplay.utils.DesktopMiniPlayerWindow
import it.fast4x.riplay.utils.DesktopTray
import it.fast4x.riplay.utils.WindowsAppIdentity
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import it.fast4x.riplay.ui.components.RiPlayAccent


@OptIn(ExperimentalCoilApi::class)
fun main() {
    // STEP 4g (fix "log all'infinito"): TUTTA l'inizializzazione e' qui,
    // nel main() PIANO, PRIMA di application{}: gira ESATTAMENTE una volta
    // per processo. Prima (fino allo step4f) le stesse chiamate stavano
    // dentro il content di application{}, che e' un AMBIENTE COMPOSABLE:
    // la lambda viene RIESeguita a ogni ricomposizione dello scope radice.
    // Con la splashscreen (step4f) lo scope si ricompone piu' spesso e, in
    // alcune condizioni su Windows, in continuazione: il risultato era
    //  - il blocco [RiPlay-Identity]/[RiPlay-Env]/[RiPlay-I18n]/[RiPlay-Account]
    //    stampato all'infinito nel log;
    //  - restoreAtStartup()/DesktopI18n eseguiti in loop (ricarichi continui
    //    di home e player, KCEF sotto pressione: copertina bianca nel
    //    miniplayer, controlli del video che lampeggiano).
    // In piu' ogni bootstrap e' IDEMPOTENTE (guardie run-once dentro le
    // singole funzioni): anche uno richiamo accidentale non puo' ripetere
    // gli effetti.
    bootstrapDesktopApp()

    application {
        // NB: questo scope NON deve contenere effetti collaterali: solo UI.
        // setSingletonImageLoaderFactory DEVE stare in composizione (e' una
        // funzione @Composable) ed e' idempotente (SingletonImageLoader.setSafe).
        setSingletonImageLoaderFactory { context ->
            getAsyncImageLoader(context)
        }

        // STEP 3h (migliorie visive): la finestra RICORDA come era stata
        // lasciata (massimizzata o con la sua dimensione). Il salvataggio
        // avviene alla chiusura (pulsante chiudi della title bar o Alt+F4:
        // entrambi passano da onCloseRequest).
        val windowState = remember { initialWindowState() }

        // STEP 9 (richiesta utente): TRAY ICON. La visibilita' della finestra
        // e' ora uno stato: chiudere (X della title bar o Alt+F4) NASCONDE
        // l'app nel tray (la musica continua, come nei player desktop), il
        // doppio click sull'icona o "Mostra RiPlay" la riapre, "Esci da
        // RiPlay" nel menu del tray esce DAVVERO. Se il sistema non ha tray,
        // la chiusura torna a uscire dall'app (comportamento precedente).
        // STEP 12b (richiesta utente): il nascondimento nel tray NON e' piu'
        // automatico: avviene SOLO con l'interruttore "Icona nel tray alla
        // chiusura" ATTIVO (Impostazioni -> Generale, default OFF, stato in
        // DesktopCloseToTray). Con l'opzione spenta la X ESCE dall'app, come
        // un programma normale; con l'opzione attiva vale tutto lo step 9.
        var windowVisible by remember { mutableStateOf(true) }

        // Callback per il tray: registrate UNA volta (remember). showWindow
        // riporta la finestra visibile, de-minimizzata e in primo piano;
        // requestExit e' l'uscita completa (salva le preferenze finestra).
        remember {
            DesktopTray.showWindow = {
                windowVisible = true
                windowState.isMinimized = false
                DesktopTray.bringToFrontLater()
            }
            DesktopTray.requestExit = {
                saveDesktopWindowPrefs(windowState)
                exitApplication()
            }
            true
        }

        fun closeWindowOrHide() {
            saveDesktopWindowPrefs(windowState)
            if (DesktopCloseToTray.enabled && DesktopTray.running) {
                // STEP 12b: opzione ATTIVA -> nasconde nel tray: la
                // riproduzione prosegue, il doppio click sull'icona riapre
                windowVisible = false
            } else {
                // opzione OFF (default) oppure tray non disponibile:
                // uscita vera dall'app
                exitApplication()
            }
        }

        // STEP 4f (richiesta utente n.6): SPLASHSCREEN di avvio. Finestra
        // senza bordi SEMPRE OPACA (niente transparent: su alcune
        // configurazioni Windows la trasparenza non e' supportata e mostra
        // uno sfondo bianco; opaca e' garantita ovunque) con logo, nome e
        // versione: copre la fase di composizione/primo caricamento.
        // La finestra principale viene resa visibile SUBITO (compone sotto
        // la splash, che e' alwaysOnTop) e la splash esce dopo
        // SPLASH_MIN_VISIBLE_MS dall'avvio.
        var splashVisible by remember { mutableStateOf(true) }
        var mainVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            mainVisible = true
            delay(SPLASH_MIN_VISIBLE_MS)
            splashVisible = false
        }

        if (splashVisible) {
            Window(
                onCloseRequest = { /* la splash non e' chiudibile: esce da sola */ },
                title = "RiPlay",
                icon = painterResource(Res.drawable.app_icon),
                undecorated = true,
                resizable = false,
                alwaysOnTop = true,
                state = rememberWindowState(
                    width = 440.dp,
                    height = 320.dp,
                    position = WindowPosition(Alignment.Center)
                )
            ) {
                DesktopSplash()
            }
        }

        Window(
            icon = painterResource(Res.drawable.app_icon),
            visible = mainVisible && windowVisible,
            onCloseRequest = { closeWindowOrHide() },
            state = windowState,
            title = "RiPlay",
            undecorated = true
        ) {
            DesktopTheme {
                ThreeColumnsApp(
                    windowScope = this,
                    windowState = windowState,
                    onMinimize = {
                        windowState.isMinimized = true
                    },
                    onToggleMaximize = {
                        windowState.placement =
                            if (windowState.placement == WindowPlacement.Maximized)
                                WindowPlacement.Floating
                            else
                                WindowPlacement.Maximized
                        saveDesktopWindowPrefs(windowState)
                    },
                    onClose = {
                        closeWindowOrHide()
                    }
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // STEP 12 (richiesta utente): MODALITA' MINIMALE stile PiP.
        // La mini player esiste SOLO quando:
        //  - l'opzione e' attiva nelle Impostazioni (tab Generale, default OFF);
        //  - la finestra principale e' ICONIFICATA (windowState.isMinimized
        //    e' uno stato Compose osservabile: ogni minimizzazione/ripristino
        //    ricompone QUESTO scope e apre/chiude la mini da solo);
        //  - la principale e' effettivamente attiva (non nascosta nel tray:
        //    in quel caso l'utente ha scelto "app nel solo tray", e il doppio
        //    click sull'icona del tray resta il modo per tornare).
        // Il click sulla copertina della mini (e il doppio click sul tray e il
        // click sulla taskbar) riportano la principale a schermo: isMinimized
        // torna false e la mini esce dalla composizione -> si chiude da sola.
        if (DesktopMiniPlayer.enabled &&
            windowState.isMinimized &&
            mainVisible && windowVisible
        ) {
            DesktopMiniPlayerWindow(
                onRestore = {
                    windowState.isMinimized = false
                    DesktopTray.bringToFrontLater()
                }
            )
        }
    }
}

/** durata minima visiva dello splashscreen (STEP 4f) */
private const val SPLASH_MIN_VISIBLE_MS = 1_600L

/**
 * STEP 4g: bootstrap NON-composable dell'app, una sola volta per processo
 * (guardia atomica: chiamata ripetuta = no-op). L'ORDINE e' lo stesso che
 * aveva l'inizializzazione dentro application{}:
 *  1. WindowsAppIdentity.setup(): AUMID + DisplayName di registro per la
 *     notifica multimediale di Windows ("app sconosciuta" -> "RiPlay");
 *  2. initializeEnvironment(): DEVE precedere QUALUNQUE accesso
 *     all'object Environment (bug "login perso" / host vuoto: vedi
 *     commento storico step3ak) - qui e' ancora piu' presto di prima;
 *  3. DesktopI18n.initialize(): lingua UI = scelta salvata o di sistema,
 *     prima della prima UI;
 *  4. KcefEngine.start(): installazione/inizializzazione KCEF in background;
 *  5. restoreAtStartup(): account YouTube Music salvato -> Environment.cookie.
 */
private val BOOTSTRAP_DONE = java.util.concurrent.atomic.AtomicBoolean(false)

private fun bootstrapDesktopApp() {
    if (!BOOTSTRAP_DONE.compareAndSet(false, true)) return

    WindowsAppIdentity.setup()

    initializeEnvironment()
    println("[RiPlay-Env] endpoint host='${Environment._1Vv31MecRl}' accountMenu='${Environment._qkHMinedvm}' origin='${Environment._XsHo8IdebO}'")
    if (Environment._1Vv31MecRl.isBlank())
        println("[RiPlay-Env] ATTENZIONE: host vuoto -> controllare le chiavi env_* (es. CrQ0JjAXgv) in gradle.properties")

    DesktopI18n.initialize()

    // STEP 9: icona nel TRAY con mini-controlli (parte qui: DesktopI18n e'
    // gia' pronto per le etichette; i comandi player arrivano piu' tardi
    // da ThreeColumnsApp via DesktopPlaybackHub, la finestra si aggancia
    // nella composizione qui sotto)
    DesktopTray.start()

    KcefEngine.start()

    it.fast4x.riplay.ui.pages.DesktopAccountManager.restoreAtStartup()

    // STEP 5e (diagnosi "app sconosciuta" tornata): quando l'init CEF
    // completa, KCEF/Chromium puo' aver RISCRITTO l'AUMID del processo.
    // Un thread daemon attende lo stato Ready (o un timeout generoso) e poi
    // VERIFICA con GetCurrentProcessExplicitAppUserModelID che l'AUMID sia
    // ancora il nostro, ripristinandolo se e' cambiato (log chiaro nel
    // terminale per la diagnosi remota). Nessun impatto su Linux/macOS.
    Thread {
        runCatching {
            val deadline = System.currentTimeMillis() + 120_000L
            while (System.currentTimeMillis() < deadline) {
                val s = KcefEngine.state.value
                if (s is KcefState.Ready || s is KcefState.Failed) break
                Thread.sleep(1_000L)
            }
            WindowsAppIdentity.reassertAfterCef()
            // ultima verifica a freddo (l'utente puo' aver gia' avviato un
            // brano: qui l'identita' del processo deve essere quella giusta)
            Thread.sleep(30_000L)
            WindowsAppIdentity.reassertAfterCef()
            // STEP 7a: diagnostica delle sessioni SMTC REALI di sistema: enumera
            // le sessioni multimediali e stampa il SourceAppUserModelId di ognuna
            // (l'AUMID che Windows usa per cercare il nome dell'app nella
            // notifica). Con un brano in riproduzione vediamo l'id effettivo
            // della sessione: se e' diverso dal nostro, sappiamo cosa registrare.
            // Due letture (+45s e +105s) per dare il tempo di avviare un brano.
            Thread.sleep(15_000L)
            WindowsAppIdentity.runSmtcSessionDiagnostic("45s")
            Thread.sleep(60_000L)
            WindowsAppIdentity.runSmtcSessionDiagnostic("105s")
        }.onFailure { it.printStackTrace() }
    }.apply {
        name = "riplay-identity-watch"
        isDaemon = true
        start()
    }
}

/**
 * STEP 4f/4g: contenuto dello splashscreen: sfondo scuro PIASTRELLATO a
 * tutta finestra (la finestra e' OPACA: nessuna dipendenza dal supporto
 * trasparenza del sistema) con card arrotondata, logo, nome, versione e
 * indicatore di caricamento. Nessun accesso a Environment o al database:
 * la splash e' puramente visiva.
 */
@androidx.compose.runtime.Composable
private fun DesktopSplash() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B10)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF15151F))
                .border(1.dp, RiPlayAccent.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = "RiPlay",
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "RiPlay",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "v${DesktopAppInfo.VERSION} (Desktop) ${DesktopAppInfo.TAGLINE}",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = RiPlayAccent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = ds("splash.loading"),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
