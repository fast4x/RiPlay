package it.fast4x.riplay.utils


import android.annotation.SuppressLint
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import it.fast4x.riplay.extensions.preferences.rememberObservedPreference
import it.fast4x.riplay.extensions.preferences.shortOnDeviceFolderNameKey
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale.getDefault
import kotlin.time.Duration.Companion.minutes
import androidx.compose.runtime.saveable.Saver


const val EXPLICIT_BUNDLE_TAG = "is_explicit"


inline val isAtLeastAndroid6
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

inline val isAtLeastAndroid7
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

inline val isAtLeastAndroid8
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

inline val isAtLeastAndroid81
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

inline val isAtLeastAndroid9
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

inline val isAtLeastAndroid10
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

inline val isAtLeastAndroid11
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

inline val isAtLeastAndroid12
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

inline val isAtLeastAndroid13
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

inline val isAtLeastAndroid14
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

inline val isAtLeastAndroid15
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM


fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int): MutableList<T> {
    add(toIndex, removeAt(fromIndex))
    return this
}

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex().matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}

fun String.capitalized(): String =
    this.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase(getDefault())
        else it.toString()
    }

fun formatAsDuration(millis: Long) = DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")

fun formatAsTime(millis: Long): String {
    val timePart1 = Duration.ofMillis(millis).toMinutes().minutes
    val timePart2 = Duration.ofMillis(millis).seconds % 60

    return "$timePart1 ${timePart2}s"
}

fun formatTimelineSongDurationToTime(millis: Long) =
    Duration.ofMillis(millis*1000).toMinutes().minutes.toString()

@SuppressLint("SimpleDateFormat")
fun getCalculatedMonths( month: Int): String? {
    val c: Calendar = GregorianCalendar()
    c.add(Calendar.MONTH, -month)
    val sdfr = SimpleDateFormat("yyyy-MM")
    return sdfr.format(c.time).toString()
}

fun numberFormatter(n: Int) =
    DecimalFormat("#,###")
        .format(n)
        .replace(",", ".")

inline fun <reified T : Throwable> Throwable.findCause(): T? {
    if (this is T) return this

    var th = cause
    while (th != null) {
        if (th is T) return th
        th = th.cause
    }

    return null
}

fun isValidHex(hex: String): Boolean {
    return hex.length == 7 && hex.startsWith("#")
}

@Composable
fun isCompositionLaunched(): Boolean {
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isLaunched = true
    }
    return isLaunched
}

inline val Int.asBoolean: Boolean
    get() = this == 1

@Composable
fun String.cleanOnDeviceName(): String {
    val shortOnDeviceFolderName by rememberObservedPreference(shortOnDeviceFolderNameKey, false)
    return if (shortOnDeviceFolderName)
         this.substringAfterLast("/") else this
}

fun String.htmlToJson(): String {
    var rawString = this.trim()

    if (rawString.startsWith("\"") && rawString.endsWith("\""))
        rawString = rawString.substring(1, this.length - 1)

    if (rawString.endsWith("\\n"))
        rawString = rawString.take(this.length - 3)

    rawString = rawString.replace("\\", "").replace("n[", "\n[")

    return rawString
}

fun String.decodeHtmlAndUnicode(): String {
    var result = this

    result = result
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    val unicodeRegex = """\\?u([0-9a-fA-F]{4})""".toRegex()

    result = unicodeRegex.replace(result) { matchResult ->
        val hexCode = matchResult.groupValues[1]
        try {
            val charCode = hexCode.toInt(16)
            charCode.toChar().toString()
        } catch (e: Exception) {
            matchResult.value
        }
    }

    return result
}

@Composable
fun rememberSavableAnimatable(initialValue: Float): Animatable<Float, AnimationVector1D> {
    val animatableSaver = Saver<Animatable<Float, AnimationVector1D>, Float>(
        save = { it.value },
        restore = { savedValue ->
            Animatable(savedValue)
        }
    )

    return rememberSaveable(saver = animatableSaver) {
        Animatable(initialValue)
    }
}

fun formatTime(seconds: Float): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return String.format("%02d:%02d", mins, secs)
}