package it.fast4x.riplay.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.riplay.R

enum class RewindThresholdDuration(val rawSeconds: Int) {
    Disabled(0),
    `3`(3),
    `4`(4),
    `5`(5),
    `6`(6),
    `7`(7),
    `8`(8),
    `9`(9),
    `10`(10),
    `11`(11),
    `12`(12);

    val milliSeconds: Long
        get() = rawSeconds * 1000L

    val textName: String
        @Composable
        get() = when (this) {
            RewindThresholdDuration.Disabled -> stringResource(R.string.vt_disabled)
            RewindThresholdDuration.`3` -> "3s"
            RewindThresholdDuration.`4` -> "4s"
            RewindThresholdDuration.`5` -> "5s"
            RewindThresholdDuration.`6` -> "6s"
            RewindThresholdDuration.`7` -> "7s"
            RewindThresholdDuration.`8` -> "8s"
            RewindThresholdDuration.`9` -> "9s"
            RewindThresholdDuration.`10` -> "10s"
            RewindThresholdDuration.`11` -> "11s"
            RewindThresholdDuration.`12` -> "12s"



        }
}


enum class DurationInMinutes(val minutes: Int) {
    Disabled(-1), // Valore di fallback
    `0`(0),
    `1`(1),
    `3`(3),
    `5`(5),
    `10`(10),
    `15`(15),
    `20`(20),
    `25`(25),
    `30`(30),
    `60`(60),
    `90`(90),
    `120`(120),
    `150`(150),
    `180`(180);

    val displayName: String
        @Composable
        get() = when (this) {
            DurationInMinutes.Disabled -> stringResource(R.string.vt_disabled)
            DurationInMinutes.`0` -> "0m"
            DurationInMinutes.`1` -> "1m"
            DurationInMinutes.`3` -> "3m"
            DurationInMinutes.`5` -> "5m"
            DurationInMinutes.`10` -> "10m"
            DurationInMinutes.`15` -> "15m"
            DurationInMinutes.`20` -> "20m"
            DurationInMinutes.`25` -> "25m"
            DurationInMinutes.`30` -> "30m"
            DurationInMinutes.`60` -> "60m"
            DurationInMinutes.`90` -> "90m"
            DurationInMinutes.`120` -> "120m"
            DurationInMinutes.`150` -> "150m"
            DurationInMinutes.`180` -> "180m"

        }

    val milliSeconds: Long
        get() = if (minutes == -1) -1L else minutes * 60000L
}
