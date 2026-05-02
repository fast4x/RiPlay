package it.fast4x.riplay.enums

enum class MaxStatisticsItems {
    `10`,
    `20`,
    `30`,
    `40`,
    `50`;

    val number: Long
        get() = when (this) {
            `10` -> 10
            `20` -> 20
            `30` -> 30
            `40` -> 40
            `50` -> 50
        }

}