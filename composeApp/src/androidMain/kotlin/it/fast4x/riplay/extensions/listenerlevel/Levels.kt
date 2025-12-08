package it.fast4x.riplay.extensions.listenerlevel

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import it.fast4x.riplay.R

enum class AnnualListenerLevel {
    SonicWhisper,
    TheSoundExplorer,
    TheDailyWanderer,
    SoulNavigator,
    TheSonicOracle,
    TheLegend;

    val levelName: String
        get() = when (this) {
            SonicWhisper -> "Sonic Whisper"
            TheSoundExplorer -> "The Sound Explorer"
            TheDailyWanderer -> "The Daily Wanderer"
            SoulNavigator -> "Soul Navigator"
            TheSonicOracle -> "The Sonic Oracle"
            TheLegend -> "The Legend"
        }

    val levelDescription: String
        get() = when (this) {
            SonicWhisper -> "Music barely brushes against you, a small taste of a much bigger world. You're just starting your journey."
            TheSoundExplorer -> "You're starting to explore, discovering new sounds and crafting your first soundtracks. Curiosity is your guide."
            TheDailyWanderer -> "Music is your daily companion. Every day has its playlist, and every song a destination."
            SoulNavigator -> "You don't just listen to music; you live it. The notes guide your deepest emotions and memories."
            TheSonicOracle -> "You are a beacon in the world of music. Your listening is a ritual, a deep and constant connection with the art of sound."
            TheLegend -> "You are not just a listener; you are an integral part of the sonic universe. Your name is whispered between the notes."
        }

    val levelApproximateHours: String
        get() = when (this) {
            SonicWhisper -> "Up to 17 total hours"
            TheSoundExplorer -> "From 17 to 83 total hours"
            TheDailyWanderer -> "From 83 to 333 total hours"
            SoulNavigator -> "From 333 to 833 total hours"
            TheSonicOracle -> "From 833 to 1,333 total hours"
            TheLegend -> "Over 1,333 total hours"
        }

    val badge
        @Composable
        get() = when (this) {
            SonicWhisper -> MonthlyIconBadge(
                icon = R.drawable.musical_note,
                colors = listOf(
                    Color(0xFFA0E0E0),
                    Color(0xFF30B0B0)
                )
            )
            TheSoundExplorer -> MonthlyIconBadge(
                icon = R.drawable.headphones,
                colors = listOf(
                    Color(0xFFA2D5A1),
                    Color(0xFF60CC4E)
                )
            )
            TheDailyWanderer -> MonthlyIconBadge(
                icon = R.drawable.music,
                colors = listOf(
                    Color(0xFFE0A17F),
                    Color(0xFFD96334)
                )
            )
            SoulNavigator -> MonthlyIconBadge(
                icon = R.drawable.music_album,
                colors = listOf(
                    Color(0xFFD081DE),
                    Color(0xFFA029B4)
                )
            )
            TheSonicOracle -> MonthlyIconBadge(
                icon = R.drawable.equalizer,
                colors = listOf(
                    Color(0xFF6497D5),
                    Color(0xFF1689D9)
                )
            )
            TheLegend -> MonthlyIconBadge(
                icon = R.drawable.trophy,
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFA500)
                )
            )
        }

    companion object {
        fun getLevelByMinutes(range: Int): AnnualListenerLevel {
            return when (range) {
                in 0..1000 -> SonicWhisper
                in 1001..5000 -> TheSoundExplorer
                in 5001..20000 -> TheDailyWanderer
                in 20001..50000 -> SoulNavigator
                in 50001..80000 -> TheSonicOracle
                in 80001..Int.MAX_VALUE -> TheLegend
                else -> SonicWhisper
            }
        }
    }



}




enum class MonthlyListenerLevel {
    SoundCheck,
    TheMonthlyExplorer,
    TheDJofYourDay,
    FrequencyDominator,
    VibeMaster,
    MonthlyIcon;

    val levelName: String
        get() = when (this) {
            SoundCheck -> "Sound Check"
            TheMonthlyExplorer -> "The Monthly Explorer"
            TheDJofYourDay -> "The DJ of Your Day"
            FrequencyDominator -> "Frequency Dominator"
            VibeMaster -> "Vibe Master"
            MonthlyIcon -> "Monthly Icon"
        }

    val levelDescription: String
        get() = when (this) {
            SoundCheck -> "You've had a look, a small taste of what this month has to offer."
            TheMonthlyExplorer -> "You're discovering new tracks and artists, building the soundtrack for right now."
            TheDJofYourDay -> "Music has become the backbone of your days. Every moment has its own song."
            FrequencyDominator -> "Your headphones are almost an extension of you. You're always tuned into the right frequency."
            VibeMaster -> "You don't just listen to music; you control it. You are the master of this month's atmosphere."
            MonthlyIcon -> "Your listening level is legendary. You're a sonic reference point for everyone around you."
        }

    val levelApproximateHours: String
        get() = when (this) {
            SoundCheck -> "Up to 1 hour and 40 mins"
            TheMonthlyExplorer -> "From 1 hour and 40 mins to 8 hours"
            TheDJofYourDay -> "From 8 to 25 hours"
            FrequencyDominator -> "From 25 to 66 hours"
            VibeMaster -> "From 66 to 108 hours"
            MonthlyIcon -> "Over 108 hours"
        }

    val badge
        @Composable
        get() = when (this) {
            SoundCheck -> MonthlyIconBadge(
                icon = R.drawable.play,
                colors = listOf(
                    Color(0xFFABADAB),
                    Color(0xFF606460)
                )
            )
            TheMonthlyExplorer -> MonthlyIconBadge(
                icon = R.drawable.headset,
                colors = listOf(
                    Color(0xFF92EE92),
                    Color(0xFF2D6C2D)
                )
            )
            TheDJofYourDay -> MonthlyIconBadge(
                icon = R.drawable.disc,
                colors = listOf(
                    Color(0xFFEC5C61),
                    Color(0xFFCB1126)
                )
            )
            FrequencyDominator -> MonthlyIconBadge(
                icon = R.drawable.equalizer,
                colors = listOf(
                    Color(0xFF7EA6EA),
                    Color(0xFF0B67A2)
                )
            )
            VibeMaster -> MonthlyIconBadge(
                icon = R.drawable.volume_up,
                colors = listOf(
                    Color(0xFFDA92E7),
                    Color(0xFFC841E0)
                )
            )
            MonthlyIcon -> MonthlyIconBadge(
                icon = R.drawable.star,
                colors = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFA500)  // Orange
                )
            )
        }

    companion object {

        fun getLevelByMinutes(range: Int): MonthlyListenerLevel {
            return when (range) {
                in 0..100 -> SoundCheck
                in 101..500 -> TheMonthlyExplorer
                in 501..1500 -> TheDJofYourDay
                in 1501..4000 -> FrequencyDominator
                in 4001..6500 -> VibeMaster
                in 6501 until Int.MAX_VALUE -> MonthlyIcon
                else -> SoundCheck
            }
        }
    }

}
