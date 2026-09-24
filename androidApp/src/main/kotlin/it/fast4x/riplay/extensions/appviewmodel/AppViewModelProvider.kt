package it.fast4x.riplay.extensions.appviewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import it.fast4x.riplay.MainApplication

val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> {
    error("AppViewModel not available non fornito — avvolgi il NavHost con AppViewModelProvider")
}

@Composable
fun AppViewModelProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MainApplication).appViewModelFactory
    val appViewModel: AppViewModel = viewModel(factory = factory)

    CompositionLocalProvider(LocalAppViewModel provides appViewModel) {
        content()
    }
}