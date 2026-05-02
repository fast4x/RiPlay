package it.fast4x.riplay.extensions.audiotagger

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.fast4x.riplay.extensions.audiotagger.ui.AccountStatsScreen
import it.fast4x.riplay.extensions.audiotagger.ui.ApiInfoScreen
import it.fast4x.riplay.extensions.audiotagger.ui.AudioTaggerMainScreen
import it.fast4x.riplay.extensions.audiotagger.ui.IdentifyFileScreen
import it.fast4x.riplay.extensions.audiotagger.ui.IdentifyRemoteScreen

@Composable
fun AudioTaggerNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            AudioTaggerMainScreen(navController)
        }
        composable(
            "api_info/{apiKey}",
            arguments = listOf(navArgument("apiKey") { type = NavType.StringType })
        ) { backStackEntry ->
            ApiInfoScreen(backStackEntry.arguments?.getString("apiKey") ?: "")
        }
        composable(
            "account_stats/{apiKey}",
            arguments = listOf(navArgument("apiKey") { type = NavType.StringType })
        ) { backStackEntry ->
            AccountStatsScreen(backStackEntry.arguments?.getString("apiKey") ?: "")
        }
        composable(
            "identify_file/{apiKey}",
            arguments = listOf(navArgument("apiKey") { type = NavType.StringType })
        ) { backStackEntry ->
            IdentifyFileScreen(backStackEntry.arguments?.getString("apiKey") ?: "")
        }
        composable(
            "identify_remote/{apiKey}",
            arguments = listOf(navArgument("apiKey") { type = NavType.StringType })
        ) { backStackEntry ->
            IdentifyRemoteScreen(backStackEntry.arguments?.getString("apiKey") ?: "")
        }
    }
}