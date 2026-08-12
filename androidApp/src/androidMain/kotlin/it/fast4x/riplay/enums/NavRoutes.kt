package it.fast4x.riplay.enums

import androidx.navigation.NavController
import timber.log.Timber

enum class NavRoutes {
    home,
    album,
    artist,
    history,
    localPlaylist,
    mood,
    player,
    playlist,
    queue,
    search,
    settings,
    statistics,
    newAlbums,
    moodsPage,
    podcast,
    videoOrSongInfo,
    onDeviceAlbum,
    onDeviceArtist,
    welcome,
    musicIdentifier,
    rewind,
    listenerLevel,
    chip,
    onDevicePlaylist,
    blacklist,
    onBoarding,
    albumInsights,
    artistInsights;

    companion object {
        fun current(navController: NavController) = navController.currentBackStackEntry?.destination?.route
    }

    fun isHere(navController: NavController): Boolean {
        val currentRoute = current(navController) ?: return true

        // Caso 1: La rotta è esattamente il nome dell'enum (es. "home")
        val isExactMatch = currentRoute == this.name

        // Caso 2: La rotta ha argomenti Path (es. "album/{albumId}")
        val isPathArgument = currentRoute.startsWith("${this.name}/")

        // Caso 3: La rotta ha argomenti Query (es. "playlist?playlistId=123")
        val isQueryArgument = currentRoute.startsWith("${this.name}?")

        return isExactMatch || isPathArgument || isQueryArgument
    }

    fun isNotHere(navController: NavController) = !isHere(navController)
}