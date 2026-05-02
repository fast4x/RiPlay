package database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getAndroidDatabaseBuilder(ctx: Context): RoomDatabase.Builder<MusicDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("riplay.db")
    return Room.databaseBuilder<MusicDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
