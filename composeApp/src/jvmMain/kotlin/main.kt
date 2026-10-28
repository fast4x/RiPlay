import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import it.fast4x.environment.Environment
import it.fast4x.riplay.i18n.DesktopI18n
import it.fast4x.riplay.getAsyncImageLoader
import it.fast4x.riplay.ui.ThreeColumnsApp
import it.fast4x.riplay.ui.initialWindowState
import it.fast4x.riplay.ui.saveDesktopWindowPrefs
import it.fast4x.riplay.ui.theme.DesktopTheme
import it.fast4x.riplay.commonutils.initializeEnvironment
import it.fast4x.riplay.player.kcef.KcefEngine
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement


@OptIn(ExperimentalCoilApi::class)
fun main() = application {
    // STEP 3ak (bug "login perso" / "il cookie potrebbe essere scaduto" /
    // home sezioni=0): initializeEnvironment() DEVE essere la PRIMA
    // istruzione dell'app, prima di QUALUNQUE accesso all'object
    // Environment. Fino alla v54 era chiamata dentro il content della
    // Window: con un account salvato su disco, restoreAtStartup() ->
    // Environment.cookie = ... triggerava la class-init dell'object
    // PRIMA che EnvironmentPreferences fosse popolato -> le val degli
    // endpoint (_1Vv31MecRl = host, _qkHMinedvm = accountMenu, ...)
    // si congelavano a stringa vuota -> defaultRequest host="" ->
    // OGNI chiamata Environment (browse, accountInfo, ...) andava a
    // localhost:443 (ConnectException "Failed to connect to
    // localhost/[0:0:0:0:0:0:0:1]:443"): verifica sempre fallita e
    // login "perso" a ogni riavvio, anche se il cookie era salvato.
    // NB: le val non cambiano piu' DOPO la class-init dell'object:
    // per questo NON basta chiamare initializeEnvironment piu' avanti,
    // deve stare qui come prima riga.
    initializeEnvironment()
    println("[RiPlay-Env] endpoint host='${Environment._1Vv31MecRl}' accountMenu='${Environment._qkHMinedvm}' origin='${Environment._XsHo8IdebO}'")
    if (Environment._1Vv31MecRl.isBlank())
        println("[RiPlay-Env] ATTENZIONE: host vuoto -> controllare le chiavi env_* (es. CrQ0JjAXgv) in gradle.properties")

    // STEP 3s (richiesta utente): lingua UI desktop = scelta salvata o
    // lingua DEL SISTEMA (fallback inglese), PRIMA della prima UI
    DesktopI18n.initialize()

    // STEP 2: installazione/inizializzazione KCEF in background (non bloccante)
    KcefEngine.start()

    // STEP 3ae: ACCOUNT YouTube Music salvato -> Environment.cookie ecc.
    // (come le preferenze ytCookie Android all'avvio del servizio)
    it.fast4x.riplay.ui.pages.DesktopAccountManager.restoreAtStartup()

    setSingletonImageLoaderFactory { context ->
        getAsyncImageLoader(context)
    }

    // STEP 3h (migliorie visive): la finestra RICORDA come era stata
    // lasciata (massimizzata o con la sua dimensione). Prima nasceva
    // sempre massimizzata. Il salvataggio avviene alla chiusura (che sia
    // dal pulsante chiudi della title bar o da Alt+F4: entrambi passano
    // da onCloseRequest).
    val windowState = remember { initialWindowState() }

    Window(
       icon = painterResource(Res.drawable.app_icon),
        onCloseRequest = {
            saveDesktopWindowPrefs(windowState)
            exitApplication()
        },
        state = windowState,
        title = "RiPlay",
        undecorated = true
    ) {
        // STEP 3ak: initializeEnvironment() spostata a inizio main()
        // (prima di qualunque accesso a Environment, vedi commento sopra)
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
                    saveDesktopWindowPrefs(windowState)
                    exitApplication()
                }
            )
        }
    }
}
