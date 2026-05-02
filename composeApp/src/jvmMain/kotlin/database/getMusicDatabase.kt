package database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDesktopDatabaseBuilder(): RoomDatabase.Builder<MusicDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "rimusic.db")
    return Room.databaseBuilder<MusicDatabase>(
        name = dbFile.absolutePath,
    )
}

val MusicDatabaseDesktop: MusicDatabaseDao
    get() = getRoomDatabase(getDesktopDatabaseBuilder()).getDao()

val DB = MusicDatabaseDesktop