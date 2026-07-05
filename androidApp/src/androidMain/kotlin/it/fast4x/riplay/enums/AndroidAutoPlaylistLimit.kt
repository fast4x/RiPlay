package it.fast4x.riplay.enums

enum class AndroidAutoPlaylistLimit {
    Unlimited,
    `100`,
    `200`,
    `300`,
    `400`,
    `500`;

    val number: Int?
        get() = when (this) {
            Unlimited -> null
            `100` -> 100
            `200` -> 200
            `300` -> 300
            `400` -> 400
            `500` -> 500
        }
}
