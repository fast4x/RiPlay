package it.fast4x.riplay.extensions.rewind.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import com.mikepenz.hypnoticcanvas.shaders.GoldenMagma
import com.mikepenz.hypnoticcanvas.shaders.GradientFlow
import com.mikepenz.hypnoticcanvas.shaders.Heat
import com.mikepenz.hypnoticcanvas.shaders.InkFlow
import com.mikepenz.hypnoticcanvas.shaders.OilFlow
import com.mikepenz.hypnoticcanvas.shaders.PurpleLiquid
import it.fast4x.riplay.data.Database
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

fun getFirstRewindYear(): Int {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    //return if (currentMonth == 11) currentYear else 0
    return 0
}

@Composable
fun getRewindYears(limit: Int = 5): List<Int> {
    val firstRewindYear = getFirstRewindYear()
    val yearsList = remember {
        Database.rewindYears(limit = limit)
    }.collectAsState(initial = null, context = Dispatchers.IO)

    return yearsList.value?.filterNot { it == firstRewindYear } ?: emptyList()
}

fun shadersList() = listOf(
    GradientFlow,
    BlackCherryCosmos,
    GoldenMagma,
    Heat(),
    PurpleLiquid,
    InkFlow,
    OilFlow
)