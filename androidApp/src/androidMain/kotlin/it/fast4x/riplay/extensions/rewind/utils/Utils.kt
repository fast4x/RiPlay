package it.fast4x.riplay.extensions.rewind.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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
import kotlin.collections.listOf

fun getRewindYear(): Int {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    return when (currentMonth) {
        0 -> currentYear - 1
        11 -> currentYear
        else -> 0
    }
}

@Composable
fun getRewindYears(limit: Int = 5): List<Int> {
    val yearsList = remember {
        Database.rewindYears(limit = limit)
    }.collectAsState(initial = null, context = Dispatchers.IO)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return yearsList.value?.dropWhile { if(currentMonth in listOf(10,11)) false else it == currentYear } ?: emptyList()
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

fun colorsList() = listOf(
    listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5)),
    listOf(Color(0xFFE7D858), Color(0xFF733B81)),
    listOf(Color(0xFF5A6CD2), Color(0xFF1DB954)),
    listOf(Color(0xFF2196F3), Color(0xFF3F51B5)),
    listOf(Color(0xFFFF9800), Color(0xFFFF5722)),
    listOf(Color(0xFFE91E63), Color(0xFF9C27B0)),
    listOf(Color(0xFF1DB954), Color(0xFFBBA0A0)),
)