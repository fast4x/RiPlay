package it.fast4x.riplay.extensions.listenerlevel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class AnnualListenerLevel {
    SonicWhisper,
    TheSoundExplorer,
    TheDailyWanderer,
    SoulNavigator,
    TheSonicOracle,
    TheLegend;

    val levelName: String
        @Composable
        get() = when (this) {
            SonicWhisper -> stringResource(R.string.all_levelname_sonic_whisper)
            TheSoundExplorer -> stringResource(R.string.all_levelname_the_sound_explorer)
            TheDailyWanderer -> stringResource(R.string.all_levelname_the_daily_wanderer)
            SoulNavigator -> stringResource(R.string.all_levelname_soul_navigator)
            TheSonicOracle -> stringResource(R.string.all_levelname_the_sonic_oracle)
            TheLegend -> stringResource(R.string.all_levelname_the_legend)
        }

    val levelDescription: String
        @Composable
        get() = when (this) {
            SonicWhisper -> stringResource(R.string.all_leveldesc_music_barely_brushes_against_you_a_small_taste_of_a_much_bigger_world_you_re_just_starting_your_journey)
            TheSoundExplorer -> stringResource(R.string.all_leveldesc_you_re_starting_to_explore_discovering_new_sounds_and_crafting_your_first_soundtracks_curiosity_is_your_guide)
            TheDailyWanderer -> stringResource(R.string.all_leveldesc_music_is_your_daily_companion_every_day_has_its_playlist_and_every_song_a_destination)
            SoulNavigator -> stringResource(R.string.all_leveldesc_you_don_t_just_listen_to_music_you_live_it_the_notes_guide_your_deepest_emotions_and_memories)
            TheSonicOracle -> stringResource(R.string.all_leveldesc_you_are_a_beacon_in_the_world_of_music_your_listening_is_a_ritual_a_deep_and_constant_connection_with_the_art_of_sound)
            TheLegend -> stringResource(R.string.all_leveldesc_you_are_not_just_a_listener_you_are_an_integral_part_of_the_sonic_universe_your_name_is_whispered_between_the_notes)
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

        fun getRangeLevel(level: AnnualListenerLevel): Pair<Int, Int> {
            return when (level) {
                SonicWhisper -> Pair(0, 1000)
                TheSoundExplorer -> Pair(1001, 5000)
                TheDailyWanderer -> Pair(5001, 20000)
                SoulNavigator -> Pair(20001, 50000)
                TheSonicOracle -> Pair(50001, 80000)
                TheLegend -> Pair(80001, Int.MAX_VALUE)
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
        @Composable
        get() = when (this) {
            SoundCheck -> stringResource(R.string.mll_levelname_sound_check)
            TheMonthlyExplorer -> stringResource(R.string.mll_levelname_the_monthly_explorer)
            TheDJofYourDay -> stringResource(R.string.mll_levelname_the_dj_of_your_day)
            FrequencyDominator -> stringResource(R.string.mll_levelname_frequency_dominator)
            VibeMaster -> stringResource(R.string.mll_levelname_vibe_master)
            MonthlyIcon -> stringResource(R.string.mll_levelname_monthly_icon)
        }

    val levelDescription: String
        @Composable
        get() = when (this) {
            SoundCheck -> stringResource(R.string.mll_leveldesc_you_ve_had_a_look_a_small_taste_of_what_this_month_has_to_offer)
            TheMonthlyExplorer -> stringResource(R.string.mll_leveldesc_you_re_discovering_new_tracks_and_artists_building_the_soundtrack_for_right_now)
            TheDJofYourDay -> stringResource(R.string.mll_leveldesc_music_has_become_the_backbone_of_your_days_every_moment_has_its_own_song)
            FrequencyDominator -> stringResource(R.string.mll_leveldesc_your_headphones_are_almost_an_extension_of_you_you_re_always_tuned_into_the_right_frequency)
            VibeMaster -> stringResource(R.string.mll_leveldesc_you_don_t_just_listen_to_music_you_control_it_you_are_the_master_of_this_month_s_atmosphere)
            MonthlyIcon -> stringResource(R.string.mll_leveldesc_your_listening_level_is_legendary_you_re_a_sonic_reference_point_for_everyone_around_you)
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
