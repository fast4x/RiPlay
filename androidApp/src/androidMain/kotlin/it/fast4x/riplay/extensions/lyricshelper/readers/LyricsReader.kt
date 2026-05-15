package it.fast4x.riplay.extensions.lyricshelper.readers

import android.content.Context
import android.net.Uri
import it.fast4x.riplay.extensions.lyricshelper.parsers.LyricsParser
import it.fast4x.riplay.extensions.lyricshelper.parsers.LyricsRaw
import it.fast4x.riplay.extensions.lyricshelper.parsers.OggCodec
import it.fast4x.riplay.extensions.lyricshelper.parsers.detectOggCodec
import it.fast4x.riplay.extensions.lyricshelper.parsers.readOpusVorbisComments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

suspend fun readLyricsFromAudio(context: Context, audioUri: Uri): LyricsRaw? =
    withContext(Dispatchers.IO) {
        val tempFile = copyUriToTempFile(context, audioUri)
        try {
            // Tentativo 1: JAudioTagger
            try {
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tag
                val rawLyrics = tag?.getFirst(FieldKey.LYRICS)?.takeIf { it.isNotBlank() }
                val lyrics = rawLyrics?.let { LyricsParser.parse(it) }
                Timber.d("LyricsReader lyrics $lyrics")
                return@withContext lyrics
            } catch (e: Exception) {
                Timber.e("LyricsReader error ${e.message} try with ogg reader")
            }

            // Tentativo 2: OGG reader
            return@withContext try {
                val rawLyrics = readOggLyrics(tempFile)
                val lyrics = rawLyrics?.let { LyricsParser.parse(it) }
                Timber.d("LyricsReader lyrics from ogg $lyrics")
                lyrics
            } catch (e: Exception) {
                null
            }

        } finally {
            tempFile.delete() // pulisce
        }
    }

private fun readOggLyrics(
    file: File
): String? {
    return when (detectOggCodec(file)) {
        OggCodec.VORBIS  -> null
        OggCodec.OPUS, OggCodec.UNKNOWN -> {
            val comments = readOpusVorbisComments(file)
            (comments["LYRICS"] ?: comments["UNSYNCEDLYRICS"])
                ?.takeIf { it.isNotBlank() }


        }
    }
}

private fun copyUriToTempFile(context: Context, uri: Uri): File {
    val extension = resolveExtension(context, uri)
    val tempFile = File.createTempFile("audio_", ".$extension", context.cacheDir)

    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }

    return tempFile
}

private fun resolveExtension(context: Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri)
    val fromMime = mime?.let {
        android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(it)
    }
    if (!fromMime.isNullOrBlank()) return fromMime

    return uri.lastPathSegment
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?: "tmp"
}