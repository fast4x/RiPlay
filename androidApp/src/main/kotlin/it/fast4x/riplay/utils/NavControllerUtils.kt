package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavController


/*
@Composable
fun getCurrentRoute (navController: NavController): String? {
    return navController.currentBackStackEntry?.destination?.route
}
 */


@Composable
fun getCurrentRoute (navController: NavController): String? {
    return navController.currentDestination?.route
    // or it's same
    //return navController.currentBackStackEntry?.destination?.route
}
