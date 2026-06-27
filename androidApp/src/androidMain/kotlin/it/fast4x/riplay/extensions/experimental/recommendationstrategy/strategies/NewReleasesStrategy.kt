package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import android.util.Log
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.RecommendationStrategy
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.ScoredRecommendation
import it.fast4x.riplay.extensions.experimental.recommendationstrategy.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NewReleasesStrategy() : RecommendationStrategy {


    private val mbAlbumDao = Database.mbAlbumDao()
    private val artistDao = Database.artistDao()
    private val albumDao = Database.albumDao()
    private val songDao = Database.songDao()


    override val id: String = "new_releases"
    override val displayName: String = "Novità"
    override val displaySubtitle: String = "Le ultime uscite che potrebbero piacerti"

    override suspend fun generate(
        profile: UserProfile,
        limit: Int,
        excludedIds: Set<String>
    ): List<ScoredRecommendation> = withContext(Dispatchers.IO) {

        if (profile.keywordVector.isEmpty()) return@withContext emptyList()

        val now = LocalDate.now()
        val last30Days = now.minusDays(30).format(DateTimeFormatter.ISO_DATE)
        val last90Days = now.minusDays(90).format(DateTimeFormatter.ISO_DATE)

        val results = mutableListOf<ScoredRecommendation>()

        // === SEZIONE 1: Appena usciti (ultimi 30 giorni, tuoi artisti) ===
        val recentByArtist = getRecentFromFavoriteArtists(
            sinceDate = last30Days,
            profile = profile,
            excludedIds = excludedIds,
            limit = limit / 3
        )
        results.addAll(recentByArtist)
        Timber.tag("REC_DEBUG").d("NewReleases: ${recentByArtist.size} from favorite artists")

        // === SEZIONE 2: Novità per generi (ultimi 90 giorni) ===
        if (results.size < limit) {
            val byGenre = getRecentByGenreMatch(
                sinceDate = last90Days,
                profile = profile,
                excludedIds = excludedIds + results.map { it.album?.id ?: "" },
                limit = limit - results.size
            )
            results.addAll(byGenre)
            Timber.tag("REC_DEBUG").d("NewReleases: ${byGenre.size} by genre match")
        }

        // === SEZIONE 3: Artisti emergenti (beginYear recente) ===
        if (results.size < limit) {
            val emerging = getEmergingArtists(
                profile = profile,
                excludedIds = excludedIds + results.map { it.artist?.id ?: "" },
                limit = (limit - results.size).coerceAtMost(3)
            )
            results.addAll(emerging)
            Timber.tag("REC_DEBUG").d("NewReleases: ${emerging.size} emerging artists")
        }

        results
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * Sezione 1: album usciti negli ultimi 30 giorni da artisti che l'utente ama.
     */
    private suspend fun getRecentFromFavoriteArtists(
        sinceDate: String,
        profile: UserProfile,
        excludedIds: Set<String>,
        limit: Int
    ): List<ScoredRecommendation> {
        if (limit <= 0) return emptyList()

        val topArtistNames = profile.topArtists
            .filter { !it.artistId.startsWith("virtual::") }
            .take(10)
            .mapNotNull { artistDao.getById(it.artistId)?.name?.lowercase() }
            .toSet()

        if (topArtistNames.isEmpty()) return emptyList()

        val candidates = mbAlbumDao.getRecentAlbumsWithArtist(sinceDate, limit * 5)

        return candidates
            .filter { "mb-${it.id}" !in excludedIds }
            .filter { mb ->
                // Match case-insensitive: artistCredit contiene il nome dell'artista
                val creditLower = mb.artistCredit?.lowercase() ?: ""
                topArtistNames.any { creditLower.contains(it) }
            }
            .take(limit)
            .map { mb ->
                val daysAgo = ChronoUnit.DAYS.between(
                    LocalDate.parse(mb.firstReleaseDate!!.take(10)),
                    LocalDate.now()
                )
                ScoredRecommendation(
                    song = null,
                    album = mapMbAlbumToAlbum(mb),
                    artist = null,
                    score = 0.9f,  // alta rilevanza: tuo artista + release recente
                    reasons = buildList {
                        add("Uscito $daysAgo giorni fa")
                        add("Dal tuo artista preferito")
                    },
                    strategyId = id,
                    strategyDisplayName = displayName
                )
            }
    }

    /**
     * Sezione 2: album recenti che matchano i generi dell'utente.
     */
    private suspend fun getRecentByGenreMatch(
        sinceDate: String,
        profile: UserProfile,
        excludedIds: Set<String>,
        limit: Int
    ): List<ScoredRecommendation> {
        if (limit <= 0) return emptyList()

        val userKeywords = profile.keywordVector.keys
        val candidates = mbAlbumDao.getRecentAlbumsWithArtist(sinceDate, limit * 5)

        return candidates
            .filter { "mb-${it.id}" !in excludedIds }
            .map { mb ->
                val albumKeywords = (mb.genres.orEmpty() + mb.tags.orEmpty())
                    .map { it.lowercase().trim() }
                    .toSet()
                val matched = albumKeywords.intersect(userKeywords)
                Triple(mb, matched, albumKeywords)
            }
            .filter { it.second.isNotEmpty() }
            .sortedByDescending { (mb, matched, _) ->
                matched.sumOf { (profile.keywordVector[it] ?: 0f).toDouble() }
            }
            .take(limit)
            .map { (mb, matched, _) ->
                val daysAgo = ChronoUnit.DAYS.between(
                    LocalDate.parse(mb.firstReleaseDate!!.take(10)),
                    LocalDate.now()
                )
                val genreScore = matched.sumOf { (profile.keywordVector[it] ?: 0f).toDouble() }.toFloat()
                val recencyScore = (1f - (daysAgo.toFloat() / 90f)).coerceIn(0f, 1f)
                val score = (0.6f * genreScore + 0.4f * recencyScore).coerceIn(0f, 1f)

                ScoredRecommendation(
                    song = null,
                    album = mapMbAlbumToAlbum(mb),
                    artist = null,
                    score = score,
                    reasons = buildList {
                        add("Uscito $daysAgo giorni fa")
                        add("Generi: ${matched.take(3).joinToString(", ")}")
                    },
                    strategyId = id,
                    strategyDisplayName = displayName
                )
            }
    }

    /**
     * Sezione 3: artisti emergenti (beginYear recente) nel genere utente.
     */
    private suspend fun getEmergingArtists(
        profile: UserProfile,
        excludedIds: Set<String>,
        limit: Int
    ): List<ScoredRecommendation> {
        if (limit <= 0) return emptyList()

        val currentYear = LocalDate.now().year
        val userKeywords = profile.keywordVector.keys

        val candidates = artistDao.getRecentArtists(
            minYear = currentYear - 2,  // artisti nati negli ultimi 2 anni
            limit = limit * 5
        )

        return candidates
            .filter { it.id !in excludedIds }
            .map { artist ->
                val artistKeywords = artist.keywords.map { it.lowercase() }.toSet()
                val matched = artistKeywords.intersect(userKeywords)
                Triple(artist, matched, artistKeywords)
            }
            .filter { it.second.isNotEmpty() }
            .sortedByDescending { (_, matched, _) -> matched.size }
            .take(limit)
            .map { (artist, matched, _) ->
                val score = (matched.size.toFloat() / userKeywords.size).coerceIn(0.3f, 0.7f)
                ScoredRecommendation(
                    song = null,
                    album = null,
                    artist = artist,
                    score = score,
                    reasons = buildList {
                        add("Artista emergente (${artist.beginYear})")
                        add("Generi: ${matched.take(3).joinToString(", ")}")
                    },
                    strategyId = id,
                    strategyDisplayName = displayName
                )
            }
    }

    private fun mapMbAlbumToAlbum(mb: MBAlbum): Album = Album(
        id = "mb-${mb.id}",
        title = mb.title,
        authorsText = mb.artistCredit,
        originalYear = mb.originalYear,
        albumType = mb.primaryType,
        genres = mb.genres,
        tags = mb.tags,
        rating = mb.rating,
        ratingVotes = mb.ratingVotes,
        isYoutubeAlbum = false,
        timestamp = mb.fetchedAt,
        nature = mb.nature
    )
}