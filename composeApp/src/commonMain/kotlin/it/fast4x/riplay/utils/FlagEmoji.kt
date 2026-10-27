package it.fast4x.riplay.utils

fun String.toFlagEmoji(): String {
    //println("toFlagEmoji: $this")
    if (this.length != 2) return ""

    val countryCode = this.uppercase()

    // 'A' ha codice 0x41 (65)
    val aCode = 'A'.code

    // Calcolo dei code point dei Regional Indicator Symbols (RIS)
    val firstCharValue = countryCode[0].code - aCode + 0x1F1E6
    val secondCharValue = countryCode[1].code - aCode + 0x1F1E6

    // Controllo di sicurezza: fondamentale per prevenire il crash
    if (firstCharValue < 0x1F1E6 || firstCharValue > 0x1F1FF ||
        secondCharValue < 0x1F1E6 || secondCharValue > 0x1F1FF) {
        return ""
    }

    // Usiamo StringBuilder e appendCodePoint() per gestire correttamente
    // i codici Unicode alti (oltre 65535) richiesti dalle emoji.
    val sb = StringBuilder()
    sb.appendCodePoint(firstCharValue)
    sb.appendCodePoint(secondCharValue)

    return sb.toString()
}

