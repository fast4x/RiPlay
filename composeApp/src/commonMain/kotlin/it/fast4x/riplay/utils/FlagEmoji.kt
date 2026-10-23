package it.fast4x.riplay.utils

// Estensione per convertire "IT" in i-flag emoji (versione Kotlin pura per commonMain)
fun String.toFlagEmoji(): String {
    if (this.length != 2) return ""
    val countryCode = this.uppercase()
    val firstLetter = countryCode[0].code - 0x41 + 0x1F1E6
    val secondLetter = countryCode[1].code - 0x41 + 0x1F1E6
    return String(charArrayOf(Char(firstLetter), Char(secondLetter)))
}
