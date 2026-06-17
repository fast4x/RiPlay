package database.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/*

@Immutable
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = _Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class _Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val songId: String,
    var timestamp: Long,
    val playTime: Long
)


 */