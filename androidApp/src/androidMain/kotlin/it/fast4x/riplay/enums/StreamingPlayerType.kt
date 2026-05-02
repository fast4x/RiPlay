package it.fast4x.riplay.enums

import it.fast4x.riplay.R
import it.fast4x.riplay.utils.appContext

enum class StreamingPlayerType {
    //TODO: add Auto type
    //Auto,
    Default,
    Next,
    Advanced;

    val displayName: String
        get() = when (this) {
            //Auto -> appContext().resources.getString(R.string.streaming_player_type_auto)
            Default -> appContext().resources.getString(R.string.streaming_player_type_default)
            Next -> appContext().resources.getString(R.string.streaming_player_type_next)
            Advanced -> appContext().resources.getString(R.string.streaming_player_type_advanced)

        }
}