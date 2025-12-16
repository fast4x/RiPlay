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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.fast4x.riplay.LocalPlayerAwareWindowInsets
import it.fast4x.riplay.R
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.enums.NavRoutes
import it.fast4x.riplay.extensions.timeline.AnimatedVerticalTimeline
import it.fast4x.riplay.extensions.timeline.TimelinePoint
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.Calendar

@Composable
fun monthlyListenerLevel(
    y: Int = Calendar.getInstance().get(Calendar.YEAR),
    m: Int = Calendar.getInstance().get(Calendar.MONTH)
): Triple<MonthlyListenerLevel, MonthlyListenerLevel, Float> {

    val minutes = remember {
        Database.minutesListenedByYearMonth(y, m)
    }.collectAsState(initial = 0, context = Dispatchers.IO)

    val level = MonthlyListenerLevel.getLevelByMinutes(minutes.value.toInt())
    val nextLevel = MonthlyListenerLevel.getNextLevel(level)

    val progress = minutes.value.toFloat() / MonthlyListenerLevel.getRangeLevel(level).second.toFloat()
    Timber.d("monthlyListenerLevel minutes ${minutes.value} level ${level.name} nextLevel ${nextLevel.name} progress $progress rangeLevel = ${MonthlyListenerLevel.getRangeLevel(level)}")

    return Triple(
        level,
        nextLevel,
        progress
    )

}

@Composable
fun annualListenerLevel(
    y: Int = Calendar.getInstance().get(Calendar.YEAR)
): Triple<AnnualListenerLevel, AnnualListenerLevel, Float> {

    val minutes = remember {
        Database.minutesListenedByYear(y)
    }.collectAsState(initial = 0, context = Dispatchers.IO)

    Timber.d("annuallyListenerLevel minutes ${minutes.value}")
    val level = AnnualListenerLevel.getLevelByMinutes(minutes.value.toInt())
    val nextLevel = AnnualListenerLevel.getNextLevel(level)

    val progress = minutes.value.toFloat() / AnnualListenerLevel.getRangeLevel(level).second.toFloat()
    Timber.d("annualListenerLevel minutes ${minutes.value} level ${level.name} nextLevel ${nextLevel.name} progress $progress rangeLevel = ${AnnualListenerLevel.getRangeLevel(level)}")

    return Triple(
        level,
        nextLevel,
        progress
    )

}

@Composable
fun LevelProgress(progress: Float, showTitle: Boolean = true) {
    Row (
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (showTitle)
            Text(
                modifier = Modifier.padding(end = 10.dp),
                text = stringResource(R.string.ll_to_next_level),
                style = typography().xxs
            )

        LinearProgressIndicator(
            color = colorPalette().accent,
            progress = { progress }
        )
    }

}

@Composable
fun MonthlyLevelBadge(
    modifier: Modifier = Modifier.fillMaxWidth(),
    level: MonthlyListenerLevel? = null,
    showTitle: Boolean = false,
    showProgress: Boolean = false,
){
    val data = if (level == null) monthlyListenerLevel() else Triple(level, level, 0f)
    val mon = data.first
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(modifier = Modifier.padding(all = 12.dp))  {
            mon.badge
        }
        Column {
            if (showTitle)
                Text(
                    text = stringResource(R.string.mll_your_monthly_level),
                    style = typography().xxs.bold
                )

            Text(
                text = mon.levelName,
                style = typography().m.bold
            )
            Text(
                text = mon.levelDescription,
                style = typography().xxs
            )

            if (showProgress)
                LevelProgress(data.third,)

        }
    }
}


@Composable
fun AnnualLevelBadge(
    modifier: Modifier = Modifier.fillMaxWidth(),
    level: AnnualListenerLevel? = null,
    showTitle: Boolean = false,
    showProgress: Boolean = false,
){
    val data = if (level == null) annualListenerLevel() else Triple(level, level, 0f)
    val ann = data.first
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
                    text = stringResource(R.string.ll_your_annual_level),
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

            if (showProgress)
                LevelProgress(data.third,)

        }
    }
}


@Composable
fun MonthlyLevelChart(level: MonthlyListenerLevel? = null) {
    val data = if (level == null) monthlyListenerLevel() else Triple(level, level, 0)
    val mont = data.first

    MonthlyListenerLevel.entries.forEach { level ->
        val modifier = if (level == mont) Modifier.background(colorPalette().accent, shape = CircleShape) else Modifier

        Box(modifier = modifier) {
            MonthlyLevelBadge(level = level, showTitle = level == mont)
        }

    }
}

@Composable
fun AnnualLevelChart(level: AnnualListenerLevel? = null) {
    val data = if (level == null) annualListenerLevel() else Triple(level, level, 0)
    val ann = data.first

    AnnualListenerLevel.entries.forEach { level ->
        val modifier = if (level == ann) Modifier.background(colorPalette().accent, shape = CircleShape) else Modifier

        Box(modifier = modifier) {
            AnnualLevelBadge(level = level, showTitle = level == ann)
        }

    }
}

@Composable
fun MonthlyListenerHistoryItems(): MutableList<TimelinePoint> {
    //var points by remember { mutableStateOf(emptyList<@Composable () -> Unit>() )}
    var timelinePoints by remember { mutableStateOf(emptyList<TimelinePoint>() )}

    for (i in 0..Calendar.getInstance().get(Calendar.MONTH)) {
        val mont = monthlyListenerLevel(m = i)

        val content = @Composable {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (i) {
                        0 -> stringResource(R.string.month_january_s, ", ${mont.first.levelName}")
                        1 -> stringResource(R.string.month_february_s, ", ${mont.first.levelName}")
                        2 -> stringResource(R.string.month_march_s, ", ${mont.first.levelName}")
                        3 -> stringResource(R.string.month_april_s, ", ${mont.first.levelName}")
                        4 -> stringResource(R.string.month_may_s, ", ${mont.first.levelName}")
                        5 -> stringResource(R.string.month_june_s, ", ${mont.first.levelName}")
                        6 -> stringResource(R.string.month_july_s, ", ${mont.first.levelName}")
                        7 -> stringResource(R.string.month_august_s, ", ${mont.first.levelName}")
                        8 -> stringResource(R.string.month_september_s, ", ${mont.first.levelName}")
                        9 -> stringResource(R.string.month_october_s, ", ${mont.first.levelName}")
                        10 -> stringResource(R.string.month_november_s, ", ${mont.first.levelName}")
                        11 -> stringResource(R.string.month_december_s, ", ${mont.first.levelName}")
                        else -> ""
                    },
                    style = typography().xxs,
                    modifier = Modifier.padding(end = 10.dp)
                )
                IconBadge(level = mont.first, size = 40)

            }
        }
        timelinePoints = timelinePoints + TimelinePoint(point = content, marker = mont.first.marker)


    }
    return timelinePoints.distinctBy { it.point } .toMutableList()
}

@Composable
fun HomepageListenerLevelBadges(navController: NavController){
    val ann = annualListenerLevel()
    val mont = monthlyListenerLevel()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate(NavRoutes.listenerLevel.name)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(modifier = Modifier.padding(all = 12.dp), horizontalAlignment = Alignment.CenterHorizontally)  {
            IconBadge(mont.first, 40, 3)
            Text(
                text = stringResource(R.string.ll_your_monthly_level),
                style = typography().xxs
            )
            Text(
                text = mont.first.levelName,
                style = typography().xxs
            )
        }
        Column(modifier = Modifier.padding(all = 12.dp), horizontalAlignment = Alignment.CenterHorizontally)  {
            IconBadge(ann.first, 40, 3)
            Text(
                text = stringResource(R.string.ll_your_annual_level),
                style = typography().xxs
            )
            Text(
                text = ann.first.levelName,
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
            text = stringResource(R.string.ll_listener_level_charts),
            style = typography().xl,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        Text(
            text = stringResource(R.string.ll_your_monthly_level),
            style = typography().l
        )

        Row( verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable{ showMonthlyChart = !showMonthlyChart}) {
            MonthlyLevelBadge(modifier = Modifier.fillMaxWidth(.9f), showProgress = true)
            Image(
                painter = painterResource(if (showMonthlyChart) R.drawable.chevron_up else R.drawable.chevron_down),
                contentDescription = "showMonthlyChart",
                modifier = Modifier
                    .padding(all = 10.dp)
                    .size(40.dp),
                colorFilter = ColorFilter.tint(colorPalette().accent),
            )
        }
        AnimatedVisibility(showMonthlyChart) {
            Column {
                MonthlyLevelChart()
                Text(
                    text = stringResource(R.string.history),
                    style = typography().m
                )
                AnimatedVerticalTimeline(MonthlyListenerHistoryItems())
            }
        }

        Text(
            text = "Your Annual Level",
            style = typography().l
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable{ showAnnualChart = !showAnnualChart}) {
            AnnualLevelBadge(modifier = Modifier.fillMaxWidth(.9f), showProgress = true)
            Image(
                painter = painterResource(if (showAnnualChart) R.drawable.chevron_up else R.drawable.chevron_down),
                contentDescription = "showAnnualChart",
                modifier = Modifier
                    .padding(all = 10.dp)
                    .size(40.dp),
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
