package it.fast4x.riplay.enums

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

    val milliSeconds: Long
        get() = if (minutes == -1) -1L else minutes * 60000L
}
