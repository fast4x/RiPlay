package it.fast4x.riplay.extensions.listenerlevel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.getCalculatedMonths
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

@Composable
fun monthlyListenerLevel(): MonthlyListenerLevel {
    val ym by remember { mutableStateOf(getCalculatedMonths(0)) }
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
    val ym by remember { mutableStateOf(getCalculatedMonths(0)) }
    val y by remember { mutableLongStateOf( ym?.substring(0,4)?.toLong() ?: 0) }

    val minutes = remember {
        Database.minutesListenedByYear(y)
    }.collectAsState(initial = 0, context = Dispatchers.IO)

    Timber.d("annuallyListenerLevel minutes ${minutes.value}")

    return AnnualListenerLevel.getLevelByMinutes(minutes.value.toInt())

}

@Composable
fun MonthlyLevelBadge(level: MonthlyListenerLevel? = null, showTitle: Boolean = false,
                      modifier: Modifier = Modifier.fillMaxWidth()
){
    val mont = level ?: monthlyListenerLevel()
    Row(
        modifier = modifier,
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
                    style = typography().xxs.bold
                )

            Text(
                text = mont.levelName,
                style = typography().m.bold
            )
            Text(
                text = mont.levelDescription,
                style = typography().xxs
            )
        }
    }
}

@Composable
fun MonthlyLevelsList(){
    MonthlyLevelBadge(MonthlyListenerLevel.SoundCheck)
    MonthlyLevelBadge(MonthlyListenerLevel.TheMonthlyExplorer)
    MonthlyLevelBadge(MonthlyListenerLevel.TheDJofYourDay)
    MonthlyLevelBadge(MonthlyListenerLevel.FrequencyDominator)
    MonthlyLevelBadge(MonthlyListenerLevel.VibeMaster)
    MonthlyLevelBadge(MonthlyListenerLevel.MonthlyIcon)
}

@Composable
fun AnnualLevelBadge(level: AnnualListenerLevel? = null, showTitle: Boolean = false,
                     modifier: Modifier = Modifier.fillMaxWidth()
){
    val ann = level ?: annualListenerLevel()
    Row(
        modifier = modifier,
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
                    style = typography().xxs.bold
                )

            Text(
                text = ann.levelName,
                style = typography().m.bold
            )
            Text(
                text = ann.levelDescription,
                style = typography().xxs
            )
        }
    }
}

@Composable
fun AnnualLevelsList(){
    AnnualLevelBadge(AnnualListenerLevel.SonicWhisper)
    AnnualLevelBadge(AnnualListenerLevel.TheSoundExplorer)
    AnnualLevelBadge(AnnualListenerLevel.TheDailyWanderer)
    AnnualLevelBadge(AnnualListenerLevel.SoulNavigator)
    AnnualLevelBadge(AnnualListenerLevel.TheSonicOracle)
    AnnualLevelBadge(AnnualListenerLevel.TheLegend)
}

@Composable
fun MonthlyLevelChart(level: MonthlyListenerLevel? = null) {
    val mont = level ?: monthlyListenerLevel()

    MonthlyListenerLevel.entries.forEach { level ->
        val modifier = if (level == mont) Modifier.background(colorPalette().accent, shape = CircleShape) else Modifier

        Box(modifier = modifier) {
            MonthlyLevelBadge(level, level == mont)
        }

    }
}

@Composable
fun AnnualLevelChart(level: AnnualListenerLevel? = null) {
    val ann = level ?: annualListenerLevel()

    AnnualListenerLevel.entries.forEach { level ->
        val modifier = if (level == ann) Modifier.background(colorPalette().accent, shape = CircleShape) else Modifier

        Box(modifier = modifier) {
            AnnualLevelBadge(level, level == ann)
        }

    }
}

@Composable
fun ListenerLevelBadges(navController: NavController){
    val ann = annualListenerLevel()
    val mont = monthlyListenerLevel()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{
                navController.navigate(NavRoutes.listenerLevel.name)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(modifier = Modifier.padding(all = 12.dp), horizontalAlignment = Alignment.CenterHorizontally)  {
            IconBadge(mont, 40, 3)
            Text(
                text = "Your Monthly Level:",
                style = typography().xxs
            )
            Text(
                text = mont.levelName,
                style = typography().xxs
            )
        }
        Column(modifier = Modifier.padding(all = 12.dp), horizontalAlignment = Alignment.CenterHorizontally)  {
            IconBadge(ann, 40, 3)
            Text(
                text = "Your Annual Level:",
                style = typography().xxs
            )
            Text(
                text = ann.levelName,
                style = typography().xxs
            )
        }

    }
}

@Composable
fun ListenerLevelCharts() {
    val scrollState = rememberScrollState()
    val windowInsets = LocalPlayerAwareWindowInsets.current
    Column (modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(windowInsets.asPaddingValues())
        .padding(horizontal = 12.dp)
    ) {
        var showMonthlyChart by remember { mutableStateOf(false) }
        var showAnnualChart by remember { mutableStateOf(false) }

        Text(
            text = "Listener Level Charts",
            style = typography().xl,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        Text(
            text = "Your Monthly Level",
            style = typography().l
        )

        Row( verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable{ showMonthlyChart = !showMonthlyChart}) {
            MonthlyLevelBadge(modifier = Modifier.fillMaxWidth(.9f))
            Image(
                painter = painterResource(if (showMonthlyChart) R.drawable.chevron_up else R.drawable.chevron_down),
                contentDescription = "showMonthlyChart",
                modifier = Modifier.padding(all = 10.dp).size(40.dp),
                colorFilter = ColorFilter.tint(colorPalette().accent),
            )
        }
        AnimatedVisibility(showMonthlyChart) {
            Column {
                MonthlyLevelChart()
            }
        }

        Text(
            text = "Your Annual Level",
            style = typography().l
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable{ showAnnualChart = !showAnnualChart}) {
            AnnualLevelBadge(modifier = Modifier.fillMaxWidth(.9f))
            Image(
                painter = painterResource(if (showAnnualChart) R.drawable.chevron_up else R.drawable.chevron_down),
                contentDescription = "showAnnualChart",
                modifier = Modifier.padding(all = 10.dp).size(40.dp),
                colorFilter = ColorFilter.tint(colorPalette().accent),
            )
        }
        AnimatedVisibility(showAnnualChart) {
            Column {
                AnnualLevelChart()
            }
        }

    }

}
