package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class CastType(val title: Int, val description: Int) {
    RITUNECAST(
        title = R.string.ritune_cast,
        description = R.string.ritune_cast_info,
    ),
//    CHROMECAST(
//        title = R.string.chrome_cast,
//        description = R.string.chrome_cast_info,
//    ),
    NONE(
        title = R.string.none,
        description = R.string.none_info,
    );

    val displayName: String
        @Composable
        get() = stringResource(title)
}
