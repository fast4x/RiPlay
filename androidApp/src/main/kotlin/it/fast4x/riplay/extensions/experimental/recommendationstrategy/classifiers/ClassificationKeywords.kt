package it.fast4x.riplay.extensions.experimental.recommendationstrategy.classifiers

object ClassificationKeywords {
    val AI = setOf(
        "ai", "ai generated", "ai generated music", "ai pop", "ai music",
        "suno", "udio", "ai art", "generative ai", "ai vocal", "synthetic voice"
    )

    val VIRTUAL = setOf(
        "vocaloid", "virtual singer", "virtual band", "vtuber", "utau",
        "synthesizer v", "cevio", "synth v"
    )

    val COVER = setOf(
        "tribute", "cover band", "cover act", "tribute act", "tribute band",
        "cover version", "covers"
    )

    val COMPILATION_ALBUM = setOf(
        "compilation", "various artists", "anthology", "collection", "best of"
    )

    val SOUNDTRACK = setOf(
        "soundtrack", "score", "film score", "ost", "original motion picture"
    )

    val COMPILATION_ARTIST_NAMES = listOf(
        "various artists", "varios artistas", "v/a", "v.a.",
        "time life", "greatest hits"
    )
}