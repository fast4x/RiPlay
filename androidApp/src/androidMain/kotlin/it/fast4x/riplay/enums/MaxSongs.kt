package it.fast4x.riplay.enums

enum class MaxSongs {
    `50`,
    `100`,
    `200`,
    `300`,
    `500`,
    `1000`,
    `2000`,
    `3000`,
    Unlimited;

    val number: Long
        get() = when (this) {
            `50` -> 50
            `100` -> 100
            `200` -> 200
            `300` -> 300
            `500` -> 500
            `1000` -> 1000
            `2000` -> 2000
            `3000` -> 3000
            Unlimited -> 1000000
        } * 1L
}