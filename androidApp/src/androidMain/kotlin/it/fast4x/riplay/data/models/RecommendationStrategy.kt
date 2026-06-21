package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "user_artist_affinity",
    primaryKeys = ["userId", "artistId"],
    indices = [
        Index(value = ["userId", "score"]),
        Index(value = ["artistId"])
    ]
)
data class UserArtistAffinity(
    val userId: String,
    val artistId: String,
    val score: Float,
    val playCount: Int,
    val totalPlayTimeMs: Long,
    val lastPlayedAt: Long,
    val likedSongs: Int,
    val dislikedSongs: Int,
    val bookmarked: Boolean,
    val updatedAt: Long
)

@Immutable
@Entity(
    tableName = "user_keyword_affinity",
    primaryKeys = ["userId", "keyword"],
    indices = [Index(value = ["userId", "weight"])]
)
data class UserKeywordAffinity(
    val userId: String,
    val keyword: String,            // lowercase, fuso genres+tags
    val weight: Float,              // TF-IDF normalizzato
    val playCount: Int,
    val updatedAt: Long
)

@Immutable
@Entity(
    tableName = "user_era_affinity",
    primaryKeys = ["userId", "decade"],
    indices = [Index(value = ["userId", "weight"])]
)
data class UserEraAffinity(
    val userId: String,
    val decade: Int,                // 1960, 1970, ...
    val weight: Float,
    val playCount: Int,
    val updatedAt: Long
)

@Immutable
@Entity(
    tableName = "recommendation",
    primaryKeys = ["userId", "songId", "strategyId"],
    indices = [
        Index(value = ["userId", "strategyId", "score"]),
        Index(value = ["userId", "consumed"]),
        Index(value = ["userId", "rejectedAt"])
    ]
)
data class Recommendation(
    val userId: String,
    val songId: String,
    val strategyId: String,
    val score: Float,
    val reasonsJson: String,        // JSON di List<String>
    val generatedAt: Long,
    val consumed: Boolean = false,
    val consumedAt: Long? = null,
    val rejectedAt: Long? = null    // se utente ha detto "non interessato"
)

@Immutable
@Entity(
    tableName = "artist_relation",
    primaryKeys = ["fromArtistId", "toArtistId", "relationType"],
    indices = [
        Index(value = ["fromArtistId"]),
        Index(value = ["toArtistId"])
    ]
)
data class ArtistRelation(
    val fromArtistId: String,
    val toArtistId: String,
    val relationType: String,       // "member_of", "collaborator", "influenced_by", ...
    val direction: String = "bidirectional",
    val fetchedAt: Long
)

data class KeywordWeight(val keyword: String, val weight: Float)


@Immutable
@Entity(
    tableName = "song_artist_cross_ref",
    primaryKeys = ["songId", "artistId"],
    indices = [
        Index(value = ["songId"]),
        Index(value = ["artistId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongArtistCrossRef(
    val songId: String,
    val artistId: String,
    @ColumnInfo(defaultValue = "main")
    val role: String = "main",     // "main", "feature", "remixer"
    @ColumnInfo(defaultValue = "0")
    val order: Int = 0,            // per ordinare nella visualizzazione
    val createdAt: Long = System.currentTimeMillis()
)