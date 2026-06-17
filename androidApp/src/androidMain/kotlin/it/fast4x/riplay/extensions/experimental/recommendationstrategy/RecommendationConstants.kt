package it.fast4x.riplay.extensions.experimental.recommendationstrategy

object RecommendationConstants {
    const val USER_ID_SELF = "self"

    // Soglie gating UI
    const val MIN_SONGS_PLAYED = 20
    const val MIN_DISTINCT_ARTISTS = 5

    // Finestre temporali
    const val FORGOTTEN_GEMS_MIN_AGE_DAYS = 90L
    const val REJECTED_COOLDOWN_DAYS = 30L
    const val CONSUMED_COOLDOWN_DAYS = 14L

    // Soglie qualità MB
    const val QUALITY_RATING_MIN = 4.0f
    const val QUALITY_VOTES_MIN = 50
    val BAYESIAN_PRIOR_M = 10f
    val BAYESIAN_PRIOR_C = 3.5f
}