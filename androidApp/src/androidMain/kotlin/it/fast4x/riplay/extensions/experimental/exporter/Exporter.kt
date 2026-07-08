package it.fast4x.riplay.extensions.experimental.exporter

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Song
import it.fast4x.riplay.enums.ExportType
import it.fast4x.riplay.utils.asMediaItem
import it.fast4x.riplay.utils.asSong
import it.fast4x.riplay.utils.isLocal
import it.fast4x.riplay.utils.parseDurationToSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

object Exporter {

    suspend fun exportTo(
        exportType: ExportType? = ExportType.Csv,
        context: Context,
        uri: Uri,
        songs: List<Song>,
        browseId: String?,
        plistName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {

        when(exportType) {
            ExportType.Csv -> exportToCsv(
                context,uri,songs,browseId,plistName
            )
            ExportType.M38u -> exportToM3U8(
                context,uri,songs
            )

            else -> Result.success(Unit)
        }

    }

    private suspend fun exportToCsv(
        context: Context,
        uri: Uri,
        songs: List<Song>,
        browseId: String?,
        plistName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.applicationContext.contentResolver.openOutputStream(uri)
                ?.use { outputStream ->
                    csvWriter().open(outputStream) {
                        writeRow(
                            "PlaylistBrowseId",
                            "PlaylistName",
                            "MediaId",
                            "Title",
                            "Artists",
                            "Duration",
                            "ThumbnailUrl",
                            "AlbumId",
                            "AlbumTitle",
                            "ArtistIds"
                        )

                        songs.forEach {
                            val artistInfos = Database.songArtistInfo(it.id)
                            val albumInfo = Database.songAlbumInfo(it.id)
                            writeRow(
                                browseId,
                                plistName,
                                it.id,
                                it.title,
                                artistInfos.joinToString(",") { it.name ?: "" },
                                it.durationText,
                                it.thumbnailUrl,
                                albumInfo?.id,
                                albumInfo?.name,
                                artistInfos.joinToString(",") { it.id }
                            )
                        }

                    }
                }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }

    }


    private suspend fun exportToM3U8(
        context: Context,
        uri: Uri,
        songs: List<Song>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {

            context.applicationContext.contentResolver.openOutputStream(uri)
                ?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->

                    writer.write("#EXTM3U\n")
                    writer.write("#EXTENC: UTF-8\n") // Dichiarazione esplicita encoding
                    writer.write("#PLAYLIST:Exported from RiPlay\n\n")

                    for (song in songs) {
                        // Scriviamo i metadati standard
                        writer.write("#EXTINF:${song.durationText.parseDurationToSeconds()},${song.artistsText} - ${song.title}\n")

                        // Le canzoni locali hanno id che inizia con suffisso LOCAL e che viene completato dall'id del mediastore
                        when (song.isLocal) {
                            true -> {
                                writer.write("${song.id}\n")
                            }
                            false -> {
                                // La canzone o video online non ha un url diretto quindi l'url sarà quello ufficiale
                                // Potrà riprodurlo un player esterno compatibile con l'url ufficiale, come fa RiPlay
                                writer.write("#RIPLAY_YOUTUBE:${song.id}\n")
                                writer.write("# https://www.youtube.com/watch?v=${song.id}\n")
                            }
                        }
                        writer.write("\n")
                    }
                }
            }
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}