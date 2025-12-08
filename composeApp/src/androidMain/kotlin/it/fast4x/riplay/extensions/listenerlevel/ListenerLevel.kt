package it.fast4x.riplay.extensions.listenerlevel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.getCalculatedMonths
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

@Composable
fun monthlyListenerLevel(): MonthlyListenerLevel {
    val ym by remember { mutableStateOf(getCalculatedMonths(1)) }
    val y by remember { mutableLongStateOf( ym?.substring(0,4)?.toLong() ?: 0) }
    val m by remember { mutableLongStateOf( ym?.substring(5,7)?.toLong() ?: 0) }

    val minutes = remember {
        Database.minutesListenedByYearMonth(y, m)
    }.collectAsState(initial = 0, context = Dispatchers.IO)

    Timber.d("monthlyListenerLevel minutes ${minutes.value}")

    return MonthlyListenerLevel.getLevelByMinutes(minutes.value.toInt())

}

@Composable
fun annualListenerLevel(): AnnualListenerLevel {
    val ym by remember { mutableStateOf(getCalculatedMonths(1)) }
    val y by remember { mutableLongStateOf( ym?.substring(0,4)?.toLong() ?: 0) }

    val minutes = remember {
        Database.minutesListenedByYear(y)
    }.collectAsState(initial = 0, context = Dispatchers.IO)

    Timber.d("annuallyListenerLevel minutes ${minutes.value}")

    return AnnualListenerLevel.getLevelByMinutes(minutes.value.toInt())

}

@Composable
fun MonthlyLevel(level: MonthlyListenerLevel? = null, showTitle: Boolean = false){
    val mont = level ?: monthlyListenerLevel()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(modifier = Modifier.padding(all = 12.dp))  {
            mont.badge
        }
        Column {
            if (showTitle)
                Text(
                    text = "Your Monthly Level",
                    style = typography().xxs
                )

            Text(
                text = mont.levelName,
                style = typography().l.bold
            )
            Text(
                text = mont.levelDescription,
                style = typography().xs
            )
        }
    }
}

@Composable
fun MonthlyLevelsList(){
    MonthlyLevel(MonthlyListenerLevel.SoundCheck)
    MonthlyLevel(MonthlyListenerLevel.TheMonthlyExplorer)
    MonthlyLevel(MonthlyListenerLevel.TheDJofYourDay)
    MonthlyLevel(MonthlyListenerLevel.FrequencyDominator)
    MonthlyLevel(MonthlyListenerLevel.VibeMaster)
    MonthlyLevel(MonthlyListenerLevel.MonthlyIcon)
}

@Composable
fun AnnualLevel(level: AnnualListenerLevel? = null, showTitle: Boolean = false){
    val ann = level ?: annualListenerLevel()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(modifier = Modifier.padding(all = 12.dp))  {
            ann.badge
        }
        Column {
            if (showTitle)
                Text(
                    text = "Your Annual Level",
                    style = typography().xxs
                )

            Text(
                text = ann.levelName,
                style = typography().l.bold
            )
            Text(
                text = ann.levelDescription,
                style = typography().xs
            )
        }
    }
}

@Composable
fun AnnualLevelsList(){
    AnnualLevel(AnnualListenerLevel.SonicWhisper)
    AnnualLevel(AnnualListenerLevel.TheSoundExplorer)
    AnnualLevel(AnnualListenerLevel.TheDailyWanderer)
    AnnualLevel(AnnualListenerLevel.SoulNavigator)
    AnnualLevel(AnnualListenerLevel.TheSonicOracle)
    AnnualLevel(AnnualListenerLevel.TheLegend)
}