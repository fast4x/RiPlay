package it.fast4x.riplay.enums

enum class HomeScreenTabs {
    Default,
    LocalSongs,
    Songs,
    Artists,
    Albums,
    Playlists,
    Search;

    val index: Int
        get() = when (this) {
            Default -> 100
            LocalSongs -> 0
            Songs -> 1
            Artists -> 2
            Albums -> 3
            Playlists -> 4
            Search -> 5
        }

}