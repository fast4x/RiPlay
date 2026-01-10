package it.fast4x.riplay.extensions.ritune.improved

fun formatTime(seconds: Float): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return String.format("%02d:%02d", mins, secs)
}
