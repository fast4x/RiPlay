package it.fast4x.riplay.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import it.fast4x.riplay.data.dao.AlbumDao
import it.fast4x.riplay.data.dao.AppSettingsDao
import it.fast4x.riplay.data.dao.AppearancePresetDao
import it.fast4x.riplay.data.dao.ArtistDao
import it.fast4x.riplay.data.dao.EventDao
import it.fast4x.riplay.data.dao.MBAlbumDao
import it.fast4x.riplay.data.dao.RecommendationDao
import it.fast4x.riplay.data.dao.RelationDao
import it.fast4x.riplay.data.dao.SongArtistCrossRefDao
import it.fast4x.riplay.data.dao.SongDao
import it.fast4x.riplay.data.dao.UnplayableTracksDao
import it.fast4x.riplay.data.dao.WebDavAccountDao
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.data.models.AppSettingsEntity
import it.fast4x.riplay.data.models.AppearancePresetEntity
import it.fast4x.riplay.data.models.Artist
import it.fast4x.riplay.data.models.ArtistDiscography
import it.fast4x.riplay.data.models.ArtistRelation
import it.fast4x.riplay.data.models.Blacklist
import it.fast4x.riplay.data.models.Event
import it.fast4x.riplay.data.models.ExternalApp
import it.fast4x.riplay.data.models.Format
import it.fast4x.riplay.data.models.Lyrics
import it.fast4x.riplay.data.models.MBAlbum
import it.fast4x.riplay.data.models.Playlist
import it.fast4x.riplay.data.models.QueuedMediaItem
import it.fast4x.riplay.data.models.Queues
import it.fast4x.riplay.data.models.Recommendation
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.data.models.SongAlbumMap
import it.fast4x.riplay.data.models.SearchQuery
import it.fast4x.riplay.data.models.SongArtistCrossRef
import it.fast4x.riplay.data.models.SongArtistMap
import it.fast4x.riplay.data.models.SongPlaylistMap
import it.fast4x.riplay.data.models.SortedSongPlaylistMap
import it.fast4x.riplay.data.models.UnplayableTrack
import it.fast4x.riplay.data.models.WebDavAccount
import it.fast4x.riplay.data.models.UserArtistAffinity
import it.fast4x.riplay.data.models.UserKeywordAffinity
import it.fast4x.riplay.data.models.UserEraAffinity

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object DatabaseInitializerConstructor : RoomDatabaseConstructor<DatabaseInitializer>

@TypeConverters(Converters::class)
@androidx.room.Database(
    entities = [
        Song::class,
        SongPlaylistMap::class,
        Playlist::class,
        Artist::class,
        SongArtistMap::class,
        Album::class,
        SongAlbumMap::class,
        SearchQuery::class,
        QueuedMediaItem::class,
        Format::class,
        Event::class,
        Lyrics::class,
        Queues::class,
        ExternalApp::class,
        Blacklist::class,
        ArtistDiscography::class,
        UserArtistAffinity::class,
        UserKeywordAffinity::class,
        UserEraAffinity::class,
        Recommendation::class,
        ArtistRelation::class,
        MBAlbum::class,
        SongArtistCrossRef::class,
        AppearancePresetEntity::class,
        AppSettingsEntity::class,
        WebDavAccount::class,
        UnplayableTrack::class
    ],
    views = [
        SortedSongPlaylistMap::class
    ],
    version = 68,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = DatabaseInitializer.From3To4Migration::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8, spec = DatabaseInitializer.From7To8Migration::class),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 11, to = 12, spec = DatabaseInitializer.From11To12Migration::class),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21, spec = DatabaseInitializer.From20To21Migration::class),
        AutoMigration(from = 21, to = 22, spec = DatabaseInitializer.From21To22Migration::class),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 30, to = 31),
        AutoMigration(from = 32, to = 33),
        AutoMigration(from = 33, to = 34),
        AutoMigration(from = 34, to = 35),
        AutoMigration(from = 35, to = 36),
        AutoMigration(from = 36, to = 37),
        AutoMigration(from = 37, to = 38),
        AutoMigration(from = 38, to = 39),
        AutoMigration(from = 39, to = 40),
        AutoMigration(from = 40, to = 41),
        AutoMigration(from = 41, to = 42),
        AutoMigration(from = 42, to = 43),
        AutoMigration(from = 44, to = 45),
        AutoMigration(from = 45, to = 46),
        AutoMigration(from = 46, to = 47),
        AutoMigration(from = 47, to = 48),
        AutoMigration(from = 48, to = 49),
        AutoMigration(from = 49, to = 50),
        AutoMigration(from = 50, to = 51),
        AutoMigration(from = 51, to = 52),
        AutoMigration(from = 52, to = 53),
        AutoMigration(from = 53, to = 54),
    ],
)
@ConstructedBy(DatabaseInitializerConstructor::class)
abstract class DatabaseInitializer : RoomDatabase() {
    abstract fun databaseDao(): Database

// Lista Dao dichiarati
    abstract fun songArtistCrossRefDao(): SongArtistCrossRefDao
    abstract fun songDao(): SongDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun mbAlbumDao(): MBAlbumDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun relationDao(): RelationDao
    abstract fun eventDao(): EventDao
    abstract fun appearancePresetDao(): AppearancePresetDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun webDavAccountDao(): WebDavAccountDao
    abstract fun unplayableTracksDao(): UnplayableTracksDao

    companion object {
        const val DB_NAME = "data.db"

        // Callback KMP condiviso da Android e Desktop
        val DbCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SQLiteConnection) {
                super.onCreate(db)
                db.execSQL("INSERT OR IGNORE INTO `app_settings` (`id`, `activePresetId`, `settingsJson`) VALUES (1, 'aura', '{}')")
            }

            override fun onOpen(db: SQLiteConnection) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        // Migrazioni manuali (applicate da entrambi i builder)
        val MANUAL_MIGRATIONS: List<Migration> = listOf(
            From8To9Migration(),
            From10To11Migration(),
            From14To15Migration(),
            From22To23Migration(),
            From23To24Migration(),
            From24To25Migration(),
            From25To26Migration(),
            From26To27Migration(),
            From31To32Migration(),
            From38To39Migration(),
            From40To41Migration(),
            From43To44Migration(),
            From45To46Migration(),
            From54To55Migration(),
            From55To56Migration(),
            From56To57Migration(),
            From57To58Migration(),
            From58To59Migration(),
            From59To60Migration(),
            From60To61Migration(),
            From61To62Migration(),
            From62To63Migration(),
            From63To64Migration(),
            From64To65Migration(),
            From65To66Migration(),
            From66To67Migration(),
            From67To68Migration(),
        )
    }

    // Crud da migrare in dao
    @DeleteTable.Entries(DeleteTable(tableName = "QueuedMediaItem"))
    class From3To4Migration : AutoMigrationSpec

    @RenameColumn.Entries(RenameColumn("Song", "albumInfoId", "albumId"))
    class From7To8Migration : AutoMigrationSpec

    class From8To9Migration : Migration(8, 9) {
        override fun migrate(db: SQLiteConnection) {
            // (Porting KMP: cursor/ContentValues riscritti in SQL puro)
            db.execSQL(
                "INSERT OR IGNORE INTO Album (id, title) " +
                "SELECT DISTINCT Info.browseId, Info.text FROM Info JOIN Song ON Info.id = Song.albumId " +
                "WHERE Info.browseId IS NOT NULL"
            )

            db.execSQL(
                "UPDATE Song SET albumId = (SELECT Info.browseId FROM Info WHERE Info.id = Song.albumId) " +
                "WHERE albumId IN (SELECT id FROM Info WHERE browseId IS NOT NULL)"
            )

            db.execSQL(
                "UPDATE Song SET artistsText = (" +
                "SELECT GROUP_CONCAT(Info.text, '') FROM Info " +
                "JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId " +
                "WHERE SongWithAuthors.songId = Song.id GROUP BY SongWithAuthors.songId) " +
                "WHERE id IN (SELECT songId FROM SongWithAuthors)"
            )

            db.execSQL(
                "INSERT OR IGNORE INTO Artist (id, name) " +
                "SELECT DISTINCT Info.browseId, Info.text FROM Info " +
                "JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId " +
                "WHERE Info.browseId IS NOT NULL"
            )

            db.execSQL(
                "UPDATE SongWithAuthors SET authorInfoId = (SELECT Info.browseId FROM Info WHERE Info.id = SongWithAuthors.authorInfoId) " +
                "WHERE authorInfoId IN (SELECT id FROM Info WHERE browseId IS NOT NULL)"
            )

            db.execSQL("INSERT INTO SongArtistMap(songId, artistId) SELECT songId, authorInfoId FROM SongWithAuthors")

            db.execSQL("DROP TABLE Info;")
            db.execSQL("DROP TABLE SongWithAuthors;")
        }
    }

    class From10To11Migration : Migration(10, 11) {
        override fun migrate(db: SQLiteConnection) {
            db.execSQL(
                "INSERT OR IGNORE INTO SongAlbumMap (songId, albumId) " +
                "SELECT id, albumId FROM Song WHERE albumId IS NOT NULL"
            )

            db.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, `loudnessDb` REAL, `contentLength` INTEGER, PRIMARY KEY(`id`))")

            db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength FROM Song;")
            db.execSQL("DROP TABLE Song;")
            db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }

    @RenameTable("SongInPlaylist", "SongPlaylistMap")
    @RenameTable("SortedSongInPlaylist", "SortedSongPlaylistMap")
    class From11To12Migration : AutoMigrationSpec

    class From14To15Migration : Migration(14, 15) {
        override fun migrate(db: SQLiteConnection) {
            db.execSQL(
                "INSERT OR IGNORE INTO Format (songId, loudnessDb, contentLength) " +
                "SELECT id, loudnessDb, contentLength FROM Song"
            )

            db.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs FROM Song;")
            db.execSQL("DROP TABLE Song;")
            db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }

    @DeleteColumn.Entries(
        DeleteColumn("Artist", "shuffleVideoId"),
        DeleteColumn("Artist", "shufflePlaylistId"),
        DeleteColumn("Artist", "radioVideoId"),
        DeleteColumn("Artist", "radioPlaylistId"),
    )
    class From20To21Migration : AutoMigrationSpec

    @DeleteColumn.Entries(DeleteColumn("Artist", "info"))
    class From21To22Migration : AutoMigrationSpec

    class From22To23Migration : Migration(22, 23) {
        override fun migrate(db: SQLiteConnection) {
            db.execSQL("CREATE TABLE IF NOT EXISTS Lyrics (`songId` TEXT NOT NULL, `fixed` TEXT, `synced` TEXT, PRIMARY KEY(`songId`), FOREIGN KEY(`songId`) REFERENCES `Song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")

            db.execSQL(
                "INSERT OR IGNORE INTO Lyrics (songId, fixed, synced) " +
                "SELECT id, lyrics, synchronizedLyrics FROM Song"
            )

            db.execSQL("CREATE TABLE IF NOT EXISTS Song_new (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT, `thumbnailUrl` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs FROM Song;")
            db.execSQL("DROP TABLE Song;")
            db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }

    class From23To24Migration : Migration(23, 24) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE SongPlaylistMap ADD COLUMN setVideoId TEXT;")
            } catch (e: Exception) {
                println("Database From23To24Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From24To25Migration : Migration(24, 25) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Playlist ADD COLUMN isEditable INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From24To25Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From25To26Migration : Migration(25, 26) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Playlist ADD COLUMN isYoutubePlaylist INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From25To26Migration error ${e.stackTraceToString()}")
            }

        }
    }
    class From26To27Migration : Migration(26, 27) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Album ADD COLUMN isYoutubeAlbum INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From26To27Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Artist ADD COLUMN isYoutubeArtist INTEGER NOT NULL DEFAULT 0;")
            } catch (e: Exception) {
                println("Database From26To27Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE SongPlaylistMap ADD COLUMN dateAdded INTEGER NULL;")
            } catch (e: Exception) {
                println("Database From26To27Migration error ${e.stackTraceToString()}")
            }

        }
    }
    class From31To32Migration : Migration(31, 32) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS QueuedMediaItem_copy (`mediaId` TEXT NOT NULL, `mediaItem` BLOB NOT NULL, `position` INTEGER, `idQueue` INTEGER, PRIMARY KEY(`mediaId`));")
                db.execSQL("DROP TABLE IF EXISTS QueuedMediaItem;")
                db.execSQL("ALTER TABLE QueuedMediaItem_copy RENAME TO QueuedMediaItem;")
            } catch (e: Exception) {
                println("Database From31To32Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From38To39Migration : Migration(38, 39) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN folder TEXT;")
            } catch (e: Exception) {
                println("Database From38To39Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From40To41Migration : Migration(40, 41) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Blacklist ADD COLUMN name TEXT;")
            } catch (e: Exception) {
                println("Database From40To41Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From43To44Migration : Migration(43, 44) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN musicVaultState TEXT NOT NULL DEFAULT 'NONE'")
            } catch (e: Exception) {
                println("Database From43To44Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN musicVaultFileName TEXT")
            } catch (e: Exception) {
                println("Database From43To44Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN musicVaultThumbnailFileName TEXT")
            } catch (e: Exception) {
                println("Database From43To44Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From45To46Migration : Migration(45, 46) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Album ADD COLUMN genres TEXT;")
            } catch (e: Exception) {
                println("Database From45To46Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Artist ADD COLUMN genres TEXT;")
            } catch (e: Exception) {
                println("Database From45To46Migration error ${e.stackTraceToString()}")
            }
            try {
                db.execSQL("ALTER TABLE Song ADD COLUMN genres TEXT;")
            } catch (e: Exception) {
                println("Database From45To46Migration error ${e.stackTraceToString()}")
            }

        }
    }

    class From54To55Migration : Migration(54, 55) {
        override fun migrate(db: SQLiteConnection) {
            try {
                // 1. Crea la nuova tabella con il DEFAULT 'NONE'
                db.execSQL("""
                    CREATE TABLE Song_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        mediaId TEXT,
                        title TEXT NOT NULL,
                        artistsText TEXT,
                        durationText TEXT,
                        thumbnailUrl TEXT,
                        likedAt INTEGER,
                        totalPlayTimeMs INTEGER NOT NULL DEFAULT 0,
                        isAudioOnly INTEGER NOT NULL DEFAULT 1,
                        isPodcast INTEGER NOT NULL DEFAULT 0,
                        folder TEXT,
                        musicVaultState TEXT NOT NULL DEFAULT 'NONE',
                        musicVaultFileName TEXT,
                        musicVaultThumbnailFileName TEXT,
                        genres TEXT
                    )
                """)

                // 2. Copia i dati
                db.execSQL("""
                    INSERT INTO Song_new (id, mediaId, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs, isAudioOnly, isPodcast, folder, musicVaultState, musicVaultFileName, musicVaultThumbnailFileName, genres)
                    SELECT id, mediaId, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs, isAudioOnly, isPodcast, folder, musicVaultState, musicVaultFileName, musicVaultThumbnailFileName, genres
                    FROM Song
                """)

                // 3. Rimuovi la vecchia tabella
                db.execSQL("DROP TABLE Song")

                // 4. Rinomina la nuova tabella
                db.execSQL("ALTER TABLE Song_new RENAME TO Song")
            } catch (e: Exception) {
                println("Database From54To55Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From55To56Migration : Migration(55, 56) {
        override fun migrate(db: SQLiteConnection) {
            try {
                // Profilo utente
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_artist_affinity (
                userId TEXT NOT NULL,
                artistId TEXT NOT NULL,
                score REAL NOT NULL,
                playCount INTEGER NOT NULL,
                totalPlayTimeMs INTEGER NOT NULL,
                lastPlayedAt INTEGER NOT NULL,
                likedSongs INTEGER NOT NULL,
                dislikedSongs INTEGER NOT NULL,
                bookmarked INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(userId, artistId)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_artist_affinity_userId_score ON user_artist_affinity(userId, score)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_artist_affinity_artistId ON user_artist_affinity(artistId)")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_keyword_affinity (
                userId TEXT NOT NULL,
                keyword TEXT NOT NULL,
                weight REAL NOT NULL,
                playCount INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(userId, keyword)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_keyword_affinity_userId_weight ON user_keyword_affinity(userId, weight)")

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_era_affinity (
                userId TEXT NOT NULL,
                decade INTEGER NOT NULL,
                weight REAL NOT NULL,
                playCount INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(userId, decade)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_era_affinity_userId_weight ON user_era_affinity(userId, weight)")

                // Recommendation
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS recommendation (
                userId TEXT NOT NULL,
                songId TEXT NOT NULL,
                strategyId TEXT NOT NULL,
                score REAL NOT NULL,
                reasonsJson TEXT NOT NULL,
                generatedAt INTEGER NOT NULL,
                consumed INTEGER NOT NULL DEFAULT 0,
                consumedAt INTEGER,
                rejectedAt INTEGER,
                PRIMARY KEY(userId, songId, strategyId)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_userId_strategyId_score ON recommendation(userId, strategyId, score)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_userId_consumed ON recommendation(userId, consumed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_userId_rejectedAt ON recommendation(userId, rejectedAt)")

                // Artist relation (anticipata)
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS artist_relation (
                fromArtistId TEXT NOT NULL,
                toArtistId TEXT NOT NULL,
                relationType TEXT NOT NULL,
                direction TEXT NOT NULL DEFAULT 'bidirectional',
                fetchedAt INTEGER NOT NULL,
                PRIMARY KEY(fromArtistId, toArtistId, relationType)
            )
        """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_relation_fromArtistId ON artist_relation(fromArtistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_relation_toArtistId ON artist_relation(toArtistId)")
            } catch (e: Exception) {
                println("Database From55To56Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From56To57Migration : Migration(56, 57) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mb_album (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        primaryType TEXT,
                        secondaryTypes TEXT,
                        firstReleaseDate TEXT,
                        originalYear INTEGER,
                        genres TEXT,
                        tags TEXT,
                        rating REAL,
                        ratingVotes INTEGER,
                        wikipediaUrl TEXT,
                        links TEXT,
                        artistCredit TEXT,
                        artistMbIds TEXT,
                        matchedAlbumId TEXT,
                        matchScore REAL,
                        matchedAt INTEGER,
                        fetchedAt INTEGER NOT NULL,
                        popularityScore REAL NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_rating_ratingVotes ON mb_album(rating, ratingVotes)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_originalYear ON mb_album(originalYear)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_primaryType ON mb_album(primaryType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_fetchedAt ON mb_album(fetchedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_matchedAlbumId ON mb_album(matchedAlbumId)")

            } catch (e: Exception) {
                println("Database From55To56Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From57To58Migration : Migration(57, 58) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE song ADD COLUMN albumId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_albumId ON song(albumId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS song_artist_cross_ref (
                        songId TEXT NOT NULL,
                        artistId TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'main',
                        "order" INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(songId, artistId),
                        FOREIGN KEY(songId) REFERENCES Song(id) ON DELETE CASCADE,
                        FOREIGN KEY(artistId) REFERENCES Artist(id) ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_artist_cross_ref_songId ON song_artist_cross_ref(songId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_artist_cross_ref_artistId ON song_artist_cross_ref(artistId)")
            } catch (e: Exception) {
                println("Database From57To58Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From58To59Migration : Migration(58, 59) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE Artist ADD COLUMN mbId TEXT")
            } catch (e: Exception) {
                println("Database From58To59Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From59To60Migration : Migration(59, 60) {
        override fun migrate(db: SQLiteConnection) {
            try {
                // === Artist ===
                // IMPORTANTE: DEFAULT NULL esplicito per matchare @ColumnInfo(defaultValue = "NULL")

                db.execSQL("ALTER TABLE Artist ADD COLUMN youtubeChannelId TEXT")
                // IMPORTANTE: NOT NULL per matchare il campo Kotlin non-nullable
                db.execSQL("ALTER TABLE Artist ADD COLUMN nature TEXT NOT NULL DEFAULT 'UNKNOWN'")

                // Popola youtubeChannelId per artisti YT esistenti
                db.execSQL("UPDATE Artist SET youtubeChannelId = id")

                // Indici non-unique
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_mbId ON Artist(mbId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_youtubeChannelId ON Artist(youtubeChannelId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_name ON Artist(name)")

                // === Album ===
                db.execSQL("ALTER TABLE Album ADD COLUMN mbId TEXT")
                db.execSQL("ALTER TABLE Album ADD COLUMN youtubeAlbumId TEXT")
                db.execSQL("ALTER TABLE Album ADD COLUMN nature TEXT NOT NULL DEFAULT 'UNKNOWN'")

                db.execSQL("UPDATE Album SET youtubeAlbumId = id")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_mbId ON Album(mbId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_youtubeAlbumId ON Album(youtubeAlbumId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_album_title_authorsText ON Album(title, authorsText)")

                // === MBAlbum ===
                db.execSQL("ALTER TABLE mb_album ADD COLUMN nature TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mb_album_nature ON mb_album(nature)")
            } catch (e: Exception) {
                println("Database From59To60Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From60To61Migration : Migration(60, 61) {
        override fun migrate(db: SQLiteConnection) {
            println("Database From60To61Migration chiamato")
            try {
                // 1. Creazione della tabella dei Preset
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `appearance_presets` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `author` TEXT,
                        `imageUrl` TEXT,
                        `localImageRes` INTEGER,
                        `isBuiltIn` INTEGER NOT NULL DEFAULT 0,
                        `settingsJson` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // 2. Creazione della tabella delle Impostazioni Globali (Theme attivo)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_settings` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `activePresetId` TEXT NOT NULL DEFAULT 'aura'
                    )
                    """.trimIndent()
                )

                db.execSQL("INSERT OR REPLACE INTO app_settings (id, activePresetId) VALUES (1, 'aura')")

            } catch (e: Exception) {
                println("Database From60To61Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From61To62Migration : Migration(61, 62) {
        override fun migrate(db: SQLiteConnection) {
            println("Database From61To62Migration chiamato")
            try {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN settingsJson TEXT NOT NULL DEFAULT '{}'")
            } catch (e: Exception) {
                println("Database From61To62Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From62To63Migration : Migration(62, 63) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN activeAppearanceJson TEXT NOT NULL DEFAULT '{}'")
            } catch (e: Exception) {
                println("Database From62To63Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From63To64Migration : Migration(63, 64) {
        override fun migrate(db: SQLiteConnection) {
            try {
                // 1. Creazione della tabella con i tipi esatti richiesti da Room
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `webdav_account` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `baseUrl` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `encryptedPassword` TEXT NOT NULL,
                        `isMusicSource` INTEGER NOT NULL DEFAULT 1,
                        `musicFolder` TEXT NOT NULL DEFAULT '/',
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )

                // 2. Creazione dell'indice univoco (fondamentale per combaciare con l'Entity)
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_webdav_account_baseUrl_username` 
                    ON `webdav_account` (`baseUrl`, `username`)
                    """.trimIndent()
                )
            } catch (e: Exception) {
                println("Database From63To64Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From64To65Migration : Migration(64, 65) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL("ALTER TABLE `webdav_account` ADD COLUMN `scanSubFolders` INTEGER NOT NULL DEFAULT 0")
            } catch (e: Exception) {
                println("Database From64To65Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From65To66Migration : Migration(65, 66) {
        override fun migrate(db: SQLiteConnection) {
            try {
                // Elimina il VECCHIO indice se esiste
                db.execSQL("DROP INDEX IF EXISTS `index_webdav_account_baseUrl_username`")

                // Crea il NUOVO indice univoco con 3 colonne
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_webdav_account_baseUrl_username_musicFolder` 
                    ON `webdav_account` (`baseUrl`, `username`, `musicFolder`)
                """.trimIndent())

            } catch (e: Exception) {
                println("Database From65To66Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From66To67Migration : Migration(66, 67) {
        override fun migrate(db: SQLiteConnection) {
            try {
                // Butta via la vecchia tabella perchè la funzionalità non è stata rilasciata ancora
                db.execSQL("DROP TABLE IF EXISTS `webdav_account`")

                // Creazione della tabella nuova
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `webdav_account` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `baseUrl` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `encryptedPassword` TEXT NOT NULL,
                        `isMusicSource` INTEGER NOT NULL DEFAULT 1,
                        `remoteFolder` TEXT NOT NULL DEFAULT '/',
                        `scanSubFolders` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )

                // Crea l'indice univoco aggiornato
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_webdav_account_baseUrl_username_remoteFolder` 
                    ON `webdav_account` (`baseUrl`, `username`, `remoteFolder`)
                """.trimIndent())

            } catch (e: Exception) {
                println("Database From66To67Migration error ${e.stackTraceToString()}")
            }
        }
    }

    class From67To68Migration : Migration(67, 68) {
        override fun migrate(db: SQLiteConnection) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `unplayable_tracks` (
                        `videoId` TEXT NOT NULL,
                        `reason` INTEGER NOT NULL,
                        `markedAt` INTEGER NOT NULL,
                        `markedWhileLoggedIn` INTEGER NOT NULL,
                        PRIMARY KEY(`videoId`)
                    )
                """
                )
            } catch (e: Exception) {
                println("Database From67To68Migration error ${e.stackTraceToString()}")
            }
        }
    }
}
