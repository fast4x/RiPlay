package it.fast4x.riplay.extensions.listenerlevel

import androidx.compose.runtime.Composable

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
        get() = IconBadge(this)


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

        fun getDistanceToNextLevel(range: Int): Int {
            return when (range) {
                in 0..1000 -> 1000 - range
                in 1001..5000 -> 5000 - range
                in 5001..20000 -> 20000 - range
                in 20001..50000 -> 50000 - range
                in 50001..80000 -> 80000 - range
                in 80001..Int.MAX_VALUE -> 0
                else -> 1000 - range
            }
        }

        fun getNextLevel(level: AnnualListenerLevel): AnnualListenerLevel {
            return when (level) {
                SonicWhisper -> TheSoundExplorer
                TheSoundExplorer -> TheDailyWanderer
                TheDailyWanderer -> SoulNavigator
                SoulNavigator -> TheSonicOracle
                TheSonicOracle -> TheLegend
                TheLegend -> TheLegend
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
        get() = IconBadge(this)


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

        fun getRangeLevel(level: MonthlyListenerLevel): Pair<Int, Int> {
            return when (level) {
                SoundCheck -> Pair(0,100)
                TheMonthlyExplorer -> Pair(101,500)
                TheDJofYourDay -> Pair(501,1500)
                FrequencyDominator -> Pair(1501,4000)
                VibeMaster -> Pair(4001,6500)
                MonthlyIcon -> Pair(6501, Int.MAX_VALUE)
            }
        }

        fun getNextLevel(level: MonthlyListenerLevel): MonthlyListenerLevel {
            return when (level) {
                SoundCheck -> TheMonthlyExplorer
                TheMonthlyExplorer -> TheDJofYourDay
                TheDJofYourDay -> FrequencyDominator
                FrequencyDominator -> VibeMaster
                VibeMaster -> MonthlyIcon
                MonthlyIcon -> MonthlyIcon
            }
        }
    }

}
