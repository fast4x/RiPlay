package it.fast4x.riplay.enums

enum class PauseBetweenSongs {
    `0`,
    `5`,
    `10`,
    `15`,
    `20`,
    `30`,
    `40`,
    `50`,
    `60`;

    val number: Long
        get() = when (this) {
            `0` -> 0
            `5` -> 5
            `10` -> 10
            `15` -> 15
            `20` -> 20
            `30` -> 30
            `40` -> 40
            `50` -> 50
            `60` -> 60

        } * 1000L
}
