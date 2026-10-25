package it.fast4x.riplay.ui.screens.welcome

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import it.fast4x.riplay.ui.components.PageContainer

@Composable
fun WelcomeScreen(navController: NavController) {
    PageContainer(navController = navController) {
        WelcomePage()
    }
}
