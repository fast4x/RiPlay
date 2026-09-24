package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class ArtistDiscography(
    @PrimaryKey val id: String,
    val albums: List<Album>,
    val singles: List<Album>
)