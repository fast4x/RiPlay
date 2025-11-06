package it.fast4x.riplay.enums

enum class MusicIdentifierProvider {
    AudioTagInfo;

    val title: String
        get() = when(this) {
            AudioTagInfo -> "AudioTag"
        }

    val subtitle: String
        get() = when(this) {
            AudioTagInfo -> "Get your api key"
        }


    val website: String
        get() = when(this) {
            AudioTagInfo -> "https://audiotag.info/apisection"
        }

    val info: String
        get() = when(this) {
            AudioTagInfo -> "AudioTag recognizes music using its own proprietary patented acoustic fingerprinting technology."
        }

}