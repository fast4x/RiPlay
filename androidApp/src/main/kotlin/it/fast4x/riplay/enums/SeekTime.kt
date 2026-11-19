package it.fast4x.riplay.enums

enum class SeekTime {
    `5s`,
    `10s`,
    `15s`,
    `20s`,
    `25s`,
    `30s`;

    val milliseconds: Long
        get() = when (this) {
            SeekTime.`5s` -> 5000L
            SeekTime.`10s` -> 10000L
            SeekTime.`15s` -> 15000L
            SeekTime.`20s` -> 20000L
            SeekTime.`25s` -> 25000L
            SeekTime.`30s` -> 30000L
        }
}