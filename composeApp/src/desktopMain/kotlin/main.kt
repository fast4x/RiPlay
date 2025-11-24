import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import it.fast4x.riplay.getAsyncImageLoader
import it.fast4x.riplay.ui.ThreeColumnsApp
import it.fast4x.riplay.ui.theme.DesktopTheme
import it.fast4x.riplay.commonutils.initializeEnvironment
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon


@OptIn(ExperimentalCoilApi::class)
fun main() = application {
    setSingletonImageLoaderFactory { context ->
        getAsyncImageLoader(context)
    }
    Window(
       icon = painterResource(Res.drawable.app_icon),
        onCloseRequest = ::exitApplication,
        state = WindowState(
            placement = WindowPlacement.Maximized,
        ),
        title = "RiPlay Desktop Multiplatform",
        undecorated = true
    ) {
        initializeEnvironment()
        DesktopTheme {
            ThreeColumnsApp()
        }
    }
}