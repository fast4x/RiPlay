package it.fast4x.riplay.enums

import androidx.navigation.NavController

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
    searchResults,
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
        fun current( navController: NavController ) = navController.currentBackStackEntry?.destination?.route
    }

    fun isHere(navController: NavController): Boolean {
        val currentRoute = current(navController) ?: return false
        return currentRoute == this.name || currentRoute.startsWith("${this.name}?")
    }

    fun isNotHere( navController: NavController ) = !isHere( navController )
}
