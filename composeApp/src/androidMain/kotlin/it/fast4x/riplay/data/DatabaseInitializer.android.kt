package it.fast4x.riplay.data

import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Registry dell'istanza singleton del database per Android.
 * androidApp registra il builder (che richiede il Context) in MainApplication.onCreate:
 *
 *     registerAndroidDatabase {
 *         Room.databaseBuilder(appContext(), DatabaseInitializer::class.java, DatabaseInitializer.DB_NAME)
 *     }
 *
 * Migrazioni e callback sono applicate qui in modo centrale (definite in commonMain).
 */
object AndroidDbRegistry {

    @Volatile
    var builderFactory: (() -> RoomDatabase.Builder<DatabaseInitializer>)? = null

    @Volatile
    private var raw: DatabaseInitializer? = null

    val instance: DatabaseInitializer
        get() = raw ?: synchronized(this) {
            raw ?: build().also { raw = it }
        }

    private fun build(): DatabaseInitializer {
        val factory = builderFactory
            ?: error(
                "AndroidDbRegistry.builderFactory non inizializzato: " +
                "chiama registerAndroidDatabase { ... } prima di usare il database " +
                "(es. in MainApplication.onCreate)"
            )
        return factory()
            .addMigrations(*DatabaseInitializer.MANUAL_MIGRATIONS.toTypedArray())
            .addCallback(DatabaseInitializer.DbCallback)
            .build()
    }
}

fun registerAndroidDatabase(builderFactory: () -> RoomDatabase.Builder<DatabaseInitializer>) {
    AndroidDbRegistry.builderFactory = builderFactory
}

actual fun getDatabaseInitializer(): DatabaseInitializer = AndroidDbRegistry.instance

actual fun getDatabaseDao(): Database = AndroidDbRegistry.instance.databaseDao()
