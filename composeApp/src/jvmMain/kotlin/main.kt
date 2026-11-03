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
import it.fast4x.riplay.styling.DesktopAccent
import it.fast4x.riplay.commonutils.initializeEnvironment
import it.fast4x.riplay.player.kcef.KcefEngine
import it.fast4x.riplay.player.kcef.KcefState
import it.fast4x.riplay.utils.DesktopAppInfo
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

    bootstrapDesktopApp()

    application {
        // NB: questo scope NON deve contenere effetti collaterali: solo UI.
        // setSingletonImageLoaderFactory DEVE stare in composizione (e' una
        // funzione @Composable) ed e' idempotente (SingletonImageLoader.setSafe).
        setSingletonImageLoaderFactory { context ->
            getAsyncImageLoader(context)
        }


        val windowState = remember { initialWindowState() }

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
            if (DesktopTray.running) {
                // nasconde nel tray: la riproduzione prosegue
                windowVisible = false
            } else {
                exitApplication()
            }
        }


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
    }
}

/** durata minima visiva dello splashscreen (STEP 4f) */
private const val SPLASH_MIN_VISIBLE_MS = 1_600L


private val BOOTSTRAP_DONE = java.util.concurrent.atomic.AtomicBoolean(false)

private fun bootstrapDesktopApp() {
    if (!BOOTSTRAP_DONE.compareAndSet(false, true)) return

    WindowsAppIdentity.setup()

    initializeEnvironment()
    println("[RiPlay-Env] endpoint host='${Environment._1Vv31MecRl}' accountMenu='${Environment._qkHMinedvm}' origin='${Environment._XsHo8IdebO}'")
    if (Environment._1Vv31MecRl.isBlank())
        println("[RiPlay-Env] ATTENZIONE: host vuoto -> controllare le chiavi env_* (es. CrQ0JjAXgv) in gradle.properties")

    DesktopI18n.initialize()

    DesktopAccent.initialize()

    DesktopTray.start()

    KcefEngine.start()

    it.fast4x.riplay.ui.pages.DesktopAccountManager.restoreAtStartup()


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
