package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import android.util.Log
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ArtistAffinity
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.UserProfile
import it.fast4x.riplay.extensions.musicbrainz.MusicBrainz
import it.fast4x.riplay.extensions.musicbrainz.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber


class MBGraphWalkStrategy() : RecommendationStrategy {

    override val id: String = "mb_graph_walk"
    override val displayName: String = "Dallo stesso universo"
    override val displaySubtitle: String = "Artisti correlati a quelli che ami"


    val relationDao = Database.relationDao()
    val songArtistRefDao = Database.songArtistCrossRefDao()
    val artistDao = Database.artistDao()
    val mbClient = MusicBrainz()


    override suspend fun generate(
        profile: UserProfile,
        limit: Int,
        excludedIds: Set<String>
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.topArtists.isEmpty()) return@withContext emptyList()

        val topArtistsWithMbId = profile.topArtists
            .filter { !it.artistId.startsWith("virtual::") }
            .take(10)
            .mapNotNull { affinity ->
                val artist = artistDao.getById(affinity.artistId) ?: return@mapNotNull null
                if (artist.mbId.isNullOrBlank()) return@mapNotNull null
                Triple(artist, artist.mbId, affinity)
            }

        val alreadyListenedArtistIds = profile.topArtists.map { it.artistId }.toSet()
        val alreadyListenedMbIds = profile.topArtists
            .mapNotNull { affinity ->
                artistDao.getById(affinity.artistId)?.mbId
            }.toSet()

        val results = mutableListOf<ScoredRecommendation>()
        val seenArtistMbIds = mutableSetOf<String>()
        var onDemandFetchCount = 0
        val maxOnDemandFetch = 30

        for ((artist, mbId, affinity) in topArtistsWithMbId) {
            val relations = relationDao.getBidirectional(mbId)

            for (rel in relations) {
                val otherMbId = if (rel.fromArtistId == mbId) rel.toArtistId else rel.fromArtistId

                if (otherMbId in seenArtistMbIds) continue
                seenArtistMbIds.add(otherMbId)

                if (otherMbId in alreadyListenedMbIds) continue

                val relationWeight = getRelationWeight(rel.relationType)
                if (relationWeight <= 0f) continue

                val existingArtist = artistDao.getByMbId(otherMbId)
                val targetArtist = existingArtist ?: run {
                    if (onDemandFetchCount >= maxOnDemandFetch) return@run null
                    onDemandFetchCount++
                    fetchArtistStubFromMB(otherMbId)
                } ?: continue

                // ★ Filtro excluded (artista MB ha id "mb-...")
                val targetArtistId = targetArtist.id
                if (targetArtistId in excludedIds) continue

                val score = scoreCandidate(
                    sourceArtist = artist,
                    otherArtist = targetArtist,
                    sourceAffinity = affinity,
                    relationWeight = relationWeight,
                    relationType = rel.relationType
                )
                if (score.score > 0.15f) {
                    results.add(score)
                }

                if (results.size >= limit * 2) break
            }
            if (results.size >= limit * 2) break
        }

        results
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun getRelationWeight(type: String): Float {
        val normalized = type.lowercase().trim()
        return when {
            normalized == "member of band" || normalized == "member of" ||
                    normalized == "subgroup of" || normalized == "founder of" ||
                    normalized == "founder" || normalized == "is person" ||
                    normalized == "group" || normalized == "founder of group" ||
                    normalized == "parent" || normalized == "subgroup" -> 1.0f

            normalized == "collaborator" || normalized == "collaborated with" ||
                    normalized == "collaborations" || normalized == "performing duo with" ||
                    normalized == "member of duo with" || normalized == "collaborated_on" -> 0.7f

            normalized == "influenced by" || normalized == "influencer" ||
                    normalized == "influence" || normalized == "derivative of" -> 0.4f

            normalized == "cover of" || normalized == "covered by" ||
                    normalized == "tribute to" || normalized == "tributed by" ||
                    normalized == "tribute act" || normalized == "cover band" -> 0.3f

            normalized.contains("supporting musician") ||
                    normalized.contains("instrumental support") ||
                    normalized.contains("vocal support") ||
                    normalized.contains("supporting vocal") ||
                    normalized == "performer" || normalized == "producer" ||
                    normalized == "engineer" || normalized == "mastering" ||
                    normalized == "mix" || normalized.contains("orchestra member") ||
                    normalized.contains("live band member") -> 0.0f

            else -> 0.0f
        }
    }

    private suspend fun fetchArtistStubFromMB(mbId: String): Artist? {
        return try {
            delay(1100)
            val mbArtist = mbClient.fetchArtistDetail(mbId)
            val artist = Artist(
                id = "mb-$mbId",
                name = mbArtist.name,
                mbId = mbId,
                timestamp = System.currentTimeMillis(),
                isYoutubeArtist = false,
                genres = mbArtist.genres.map { it.name }.takeIf { it.isNotEmpty() },
                artistType = mbArtist.type,
                countryCode = mbArtist.country,
                beginYear = mbArtist.lifeSpan?.begin?.substring(0, 4)?.toIntOrNull(),
                tags = mbArtist.tags?.map { it.name }?.takeIf { it.isNotEmpty() }
            )
            artistDao.upsert(artist)
            artist
        } catch (e: Exception) {
            null
        }
    }

    private fun scoreCandidate(
        sourceArtist: Artist,
        otherArtist: Artist,
        sourceAffinity: ArtistAffinity,
        relationWeight: Float,
        relationType: String
    ): ScoredRecommendation {
        val score = (0.5f * sourceAffinity.score + 0.5f * relationWeight).coerceIn(0f, 1f)

        val reasons = buildList {
            add("Da ${sourceArtist.name} — $relationType")
            add("${otherArtist.name} — peso ${(relationWeight * 100).toInt()}%")
        }

        return ScoredRecommendation(
            song = null,
            album = null,
            artist = otherArtist,
            score = score,
            reasons = reasons,
            strategyId = id,
            strategyDisplayName = displayName
        )
    }
}