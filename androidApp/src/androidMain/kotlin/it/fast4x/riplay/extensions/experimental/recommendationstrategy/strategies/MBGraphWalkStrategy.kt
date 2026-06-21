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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber


class MBGraphWalkStrategy() : RecommendationStrategy {

    override val id: String = "mb_graph_walk"
    override val displayName: String = "Dallo stesso universo"
    override val displaySubtitle: String = "Artisti correlati a quelli che ami"

    override suspend fun generate(
        profile: UserProfile,
        limit: Int
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.topArtists.isEmpty()) return@withContext emptyList()

        val topArtistsWithMbId = profile.topArtists
            .filter { !it.artistId.startsWith("virtual::") }
            .take(10)
            .mapNotNull { affinity ->
                val artist = Database.artist(affinity.artistId).first() ?: return@mapNotNull null
                if (artist.mbId.isNullOrBlank()) return@mapNotNull null
                Triple(artist, artist.mbId, affinity)
            }

        if (BuildConfig.DEBUG)
            Timber.tag("REC_DEBUG").d("MBGraphWalk: ${topArtistsWithMbId.size} artists with mbId")

        val alreadyListenedArtistIds = profile.topArtists.map { it.artistId }.toSet()
        val alreadyListenedMbIds = profile.topArtists
            .mapNotNull { affinity ->
                Database.artist(affinity.artistId).first()?.mbId
            }.toSet()

        val results = mutableListOf<ScoredRecommendation>()
        val seenArtistMbIds = mutableSetOf<String>()
        var onDemandFetchCount = 0
        val maxOnDemandFetch = 30

        for ((artist, mbId, affinity) in topArtistsWithMbId) {
            val relations = Database.getBidirectional(mbId)

            if (BuildConfig.DEBUG)
                Timber.tag("REC_DEBUG")
                    .d("  ${artist.name} ($mbId): ${relations.size} relations, affinity=${affinity.score}")

            var usefulRelations = 0
            var skippedUnknownType = 0
            var skippedAlreadyListened = 0
            var skippedFetchFailed = 0
            var skippedLowScore = 0

            for (rel in relations) {
                val otherMbId = if (rel.fromArtistId == mbId) rel.toArtistId else rel.fromArtistId

                if (BuildConfig.DEBUG)
                    Timber.tag("REC_DEBUG")
                        .d("    [RAW] type='${rel.relationType}', target_mbid=$otherMbId")

                if (otherMbId in seenArtistMbIds) continue
                seenArtistMbIds.add(otherMbId)

                // SALTA se utente già ascolta questo artista (via mbId)
                if (otherMbId in alreadyListenedMbIds) {
                    skippedAlreadyListened++
                    continue
                }

                // CALCOLA peso relazione — skip se tipo inutile
                val relationWeight = getRelationWeight(rel.relationType)
                if (relationWeight <= 0f) {
                    skippedUnknownType++
                    continue
                }

                // Ottieni info artista target
                val existingArtist = Database.artistDao().getByMbId(otherMbId)
                val targetArtist = existingArtist ?: run {
                    if (onDemandFetchCount >= maxOnDemandFetch) {
                        skippedFetchFailed++
                        return@run null
                    }
                    onDemandFetchCount++
                    fetchArtistStubFromMB(otherMbId)
                } ?: run {
                    skippedFetchFailed++
                    null
                } ?: continue

                // CALCOLA score
                val score = scoreCandidate(
                    sourceArtist = artist,
                    otherArtist = targetArtist,
                    sourceAffinity = affinity,
                    relationWeight = relationWeight,
                    relationType = rel.relationType
                )

                if (score.score > 0.15f) {  // ← abbassato da 0.2 a 0.15
                    results.add(score)
                    usefulRelations++

                    if (BuildConfig.DEBUG)
                        Timber.tag("REC_DEBUG")
                            .d("    ✓ ${targetArtist.name} (type=${rel.relationType}, weight=$relationWeight, score=${score.score})")
                } else {
                    skippedLowScore++
                }

                if (results.size >= limit * 2) break
            }

            if (BuildConfig.DEBUG)
                Timber.tag("REC_DEBUG")
                    .d("    Summary: useful=$usefulRelations, unknownType=$skippedUnknownType, alreadyListened=$skippedAlreadyListened, fetchFailed=$skippedFetchFailed, lowScore=$skippedLowScore")

            if (results.size >= limit * 2) break
        }

        if (BuildConfig.DEBUG)
            Timber.tag("REC_DEBUG").d("MBGraphWalk: ${results.size} candidates")

        results
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * Pesi per tipi di relazione MB.
     * IMPORTANTE: ritorna 0 per tipi "rumore" (session musicians, engineers, etc.)
     */
    private fun getRelationWeight(type: String): Float {
        val normalized = type.lowercase().trim()
        return when {
            // ★ "member of band" — il tipo standard MB per membri di gruppi
            normalized == "member of band" ||
                    normalized == "member of" ||
                    normalized == "subgroup of" ||
                    normalized == "founder of" ||
                    normalized == "founder" ||
                    normalized == "is person" ||
                    normalized == "group" ||
                    normalized == "founder of group" ||
                    normalized == "parent" ||
                    normalized == "subgroup" -> 1.0f

            // Collaborazioni
            normalized == "collaborator" ||
                    normalized == "collaborated with" ||
                    normalized == "collaborations" ||
                    normalized == "performing duo with" ||
                    normalized == "member of duo with" ||
                    normalized == "collaborated_on" -> 0.7f

            // Influences
            normalized == "influenced by" ||
                    normalized == "influencer" ||
                    normalized == "influence" ||
                    normalized == "derivative of" -> 0.4f

            // Cover / tribute
            normalized == "cover of" ||
                    normalized == "covered by" ||
                    normalized == "tribute to" ||
                    normalized == "tributed by" ||
                    normalized == "tribute act" ||
                    normalized == "cover band" -> 0.3f

            // Skip esplicito per rumore
            normalized.contains("supporting musician") ||
                    normalized.contains("instrumental support") ||
                    normalized.contains("vocal support") ||
                    normalized.contains("supporting vocal") ||
                    normalized == "performer" ||
                    normalized == "producer" ||
                    normalized == "engineer" ||
                    normalized == "mastering" ||
                    normalized == "mix" ||
                    normalized.contains("orchestra member") ||
                    normalized.contains("live band member") -> 0.0f

            else -> 0.0f
        }
    }

    private suspend fun fetchArtistStubFromMB(mbId: String): Artist? {
        return try {
            val mbClient = MusicBrainz()
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
            Database.artistDao().upsert(artist)

            if (BuildConfig.DEBUG)
                Timber.tag("REC_DEBUG").d("    Fetched stub: ${artist.name} ($mbId)")

            artist
        } catch (e: Exception) {
            Timber.tag("REC_DEBUG").w("    Failed to fetch artist $mbId: ${e.message}")
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
            strategyId = id
        )
    }
}