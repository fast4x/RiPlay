package it.fast4x.riplay.utils

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.MonthlyListenerLevel
import it.fast4x.riplay.ui.components.themed.Title2Actions
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

@Composable
fun monthlyListenerLevel(): MonthlyListenerLevel {
    val ym by remember { mutableStateOf( getCalculatedMonths(1)) }
    val y by remember { mutableLongStateOf( ym?.substring(0,4)?.toLong() ?: 0) }
    val m by remember { mutableLongStateOf( ym?.substring(5,7)?.toLong() ?: 0) }

    val minutes = remember {
        Database.minutesListenedByYearMonth(y, m)
    }.collectAsState(initial = 0, context = Dispatchers.IO)

    Timber.d("monthlyListenerLevel minutes ${minutes.value}")

    return MonthlyListenerLevel.getLevelByMinutes(minutes.value.toInt())

}

@Composable
fun ListenerLevel(){
    Title2Actions(
        title = monthlyListenerLevel().levelName,
        icon2 = R.drawable.trophy,
        onClick1 = {},
        onClick2 = {}
    )
}