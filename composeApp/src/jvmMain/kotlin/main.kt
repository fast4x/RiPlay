import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
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
    // STEP 2: installazione/inizializzazione KCEF in background (non bloccante)
    KcefEngine.start()
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
        initializeEnvironment()
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
