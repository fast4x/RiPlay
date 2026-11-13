package it.fast4x.riplay.enums

import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext

enum class ListenBrainzScrobbleType {
    Simple,
    NowPlaying;

    val textName: String
        get() = when (this) {
            Simple -> appContext().getString(R.string.listenbrainz_scrobble_simple)
            NowPlaying -> appContext().getString(R.string.listenbrainz_scrobble_now_playing)
        }
}
