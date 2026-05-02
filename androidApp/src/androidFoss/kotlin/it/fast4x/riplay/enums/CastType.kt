package it.fast4x.riplay.enums

import it.fast4x.riplay.R

enum class CastType(val title: Int, val description: Int) {
    RITUNECAST(
        title = R.string.ritune_cast,
        description = R.string.ritune_cast_info,
    ),
    NONE(
        title = R.string.none,
        description = R.string.none_info,
    )
}