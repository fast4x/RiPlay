package it.fast4x.riplay.extensions.experimental.recommendationstrategy.ui

import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.ScoredRecommendation

data class RecommendationSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val items: List<ScoredRecommendation>,
    val updatedAt: Long
) {
    val isEmpty: Boolean get() = items.isEmpty()
}