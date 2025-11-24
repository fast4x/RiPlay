package it.fast4x.riplay.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.month_april_s
import riplay.composeapp.generated.resources.month_august_s
import riplay.composeapp.generated.resources.month_december_s
import riplay.composeapp.generated.resources.month_february_s
import riplay.composeapp.generated.resources.month_january_s
import riplay.composeapp.generated.resources.month_july_s
import riplay.composeapp.generated.resources.month_june_s
import riplay.composeapp.generated.resources.month_march_s
import riplay.composeapp.generated.resources.month_may_s
import riplay.composeapp.generated.resources.month_november_s
import riplay.composeapp.generated.resources.month_october_s
import riplay.composeapp.generated.resources.month_september_s

@Composable
fun getTitleMonthlyPlaylist(playlist: String): String {

    val y = playlist.substring(0,4)
    val m = playlist.substring(5,7).toInt()
    return when (m) {
        1 -> stringResource(Res.string.month_january_s).format(y)
        2 -> stringResource(Res.string.month_february_s).format(y)
        3 -> stringResource(Res.string.month_march_s).format(y)
        4 -> stringResource(Res.string.month_april_s).format(y)
        5 -> stringResource(Res.string.month_may_s).format(y)
        6 -> stringResource(Res.string.month_june_s).format(y)
        7 -> stringResource(Res.string.month_july_s).format(y)
        8 -> stringResource(Res.string.month_august_s).format(y)
        9 -> stringResource(Res.string.month_september_s).format(y)
        10 -> stringResource(Res.string.month_october_s).format(y)
        11 -> stringResource(Res.string.month_november_s).format(y)
        12 -> stringResource(Res.string.month_december_s).format(y)
        else -> playlist
    }
}