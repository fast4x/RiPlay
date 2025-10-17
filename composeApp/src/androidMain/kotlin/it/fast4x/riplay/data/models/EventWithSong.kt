package it.fast4x.riplay.data.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import java.time.LocalDate

@Immutable
data class EventWithSong(
    val timestampDay: Long?,
    @Embedded
    val event: Event,
    @Relation(
        entity = Song::class,
        parentColumn = "songId",
        entityColumn = "id"
    )
    val song: Song
)


sealed class DateAgo {
    object Today : DateAgo()
    object Yesterday : DateAgo()
    object ThisWeek : DateAgo()
    object LastWeek : DateAgo()
    class Other(val date: LocalDate) : DateAgo() {
        override fun equals(other: Any?): Boolean {
            if (other is Other) return date == other.date
            return super.equals(other)
        }

        override fun hashCode(): Int = date.hashCode()
    }
}