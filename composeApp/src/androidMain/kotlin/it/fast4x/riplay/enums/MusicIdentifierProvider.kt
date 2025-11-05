package it.fast4x.riplay.enums

enum class MusicIdentifierProvider {
    AudioTagInfo;

    val title: String
        get() = when(this) {
            AudioTagInfo -> "AudioTag"
        }

    val website: String
        get() = when(this) {
            AudioTagInfo -> "https://audiotag.info"
        }

    val info: String
        get() = when(this) {
            AudioTagInfo -> "AudioTag recognizes music using its own proprietary patented acoustic fingerprinting technology."
        }

}