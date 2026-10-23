package it.fast4x.riplay.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unplayable_tracks")
data class UnplayableTrack(
    @PrimaryKey val videoId: String,
    val reason: Int,                  // codice errore di riproduzione 100, 101, 153
    val markedAt: Long,               // quando è stato inserito nel DB
    val markedWhileLoggedIn: Boolean  // contesto al momento della marcatura con revoca al cambio login
)