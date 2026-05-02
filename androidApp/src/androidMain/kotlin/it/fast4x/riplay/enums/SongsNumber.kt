package it.fast4x.riplay.enums

enum class SongsNumber {
    `1`,
    `2`,
    `3`,
    `4`,
    `5`,
    `6`,
    `7`,
    `8`,
    `9`;

    val number: Int
        get() = when (this) {
            `1` -> 1
            `2` -> 2
            `3` -> 3
            `4` -> 4
            `5` -> 5
            `6` -> 6
            `7` -> 7
            `8` -> 8
            `9` -> 9
        }

}