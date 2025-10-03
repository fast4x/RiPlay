package it.fast4x.riplay.ui.screens.welcome

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.TitleMiniSection
import it.fast4x.riplay.ui.screens.settings.isLoggedIn
import it.fast4x.riplay.ytAccountName
import java.text.SimpleDateFormat
import java.util.Calendar

@Composable
fun WelcomeMessage(){
    val hour =
        remember {
            val date = Calendar.getInstance().time
            @SuppressLint("SimpleDateFormat")
            val formatter = SimpleDateFormat("HH")
            formatter.format(date).toInt()
        }

    val message = when (hour) {
        in 6..12 -> {
            stringResource(R.string.good_morning)
        }

        in 13..17 -> {
            stringResource(R.string.good_afternoon)
        }

        in 18..23 -> {
            stringResource(R.string.good_evening)
        }

        else -> {
            stringResource(R.string.good_night)
        }
    }.let {
        if (isLoggedIn()) "$it, ${ytAccountName()}"
        else it
    }

    TitleMiniSection(
        title = message,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}