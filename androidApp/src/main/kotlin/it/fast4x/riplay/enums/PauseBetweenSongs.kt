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

    val displayName: String
        get() = when (this) {
            PauseBetweenSongs.`0` -> "0s"
            PauseBetweenSongs.`5` -> "5s"
            PauseBetweenSongs.`10` -> "10s"
            PauseBetweenSongs.`15` -> "15s"
            PauseBetweenSongs.`20` -> "20s"
            PauseBetweenSongs.`30` -> "30s"
            PauseBetweenSongs.`40` -> "40s"
            PauseBetweenSongs.`50` -> "50s"
            PauseBetweenSongs.`60` -> "60s"
        }


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
