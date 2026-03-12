package it.fast4x.riplay.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.fast4x.kugou.KuGou
import it.fast4x.lrclib.LrcLib
import it.fast4x.riplay.extensions.lyricshelper.models.LyricLine
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.commonutils.durationTextToMillis
import it.fast4x.riplay.data.Database
import it.fast4x.riplay.data.models.Lyrics
import it.fast4x.riplay.data.models.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class SynchronizedLyricsLines(val sentences: List<LyricLine>, private val positionProvider: () -> Long) {
    var index by mutableStateOf(currentIndex)
        private set

    private val currentIndex: Int
        get() {
            //return sentences.indexOfLast { it.timeMs <= positionProvider() }
            var index = -1
            for (item in sentences) {
                if (item.timeMs >= positionProvider()) break
                index++
            }
            return if (index == -1) 0 else index
        }

    fun update(): Boolean {
        val newIndex = currentIndex
        return if (newIndex != index) {
            index = newIndex
            true
        } else {
            false
        }
    }
}

fun List<Pair<Long, String>>.toLyricLine(): List<LyricLine> {
    return this.map { pair ->
        LyricLine(
            timeMs = pair.first,
            text = pair.second
        )
    }

}

class SynchronizedLyrics(val sentences: List<Pair<Long, String>>, private val positionProvider: () -> Long) {
    var index by mutableStateOf(currentIndex)
        private set

    private val currentIndex: Int
        get() {
            var index = -1
            for (item in sentences) {
                if (item.first >= positionProvider()) break
                index++
            }
            return if (index == -1) 0 else index
        }

    fun update(): Boolean {
        val newIndex = currentIndex
        return if (newIndex != index) {
            index = newIndex
            true
        } else {
            false
        }
    }
}

fun downloadSyncedLyrics(it : SongEntity, coroutineScope : CoroutineScope) {
    var lyrics by mutableStateOf<Lyrics?>(null)
    coroutineScope.launch {
        withContext(Dispatchers.IO) {
            Database.lyrics(it.asMediaItem.mediaId)
                .collect { currentLyrics ->
                    if (currentLyrics?.synced == null) {
                        lyrics = null
                        runCatching {
                            LrcLib.lyrics(
                                artist = it.song.artistsText
                                    ?: "",
                                title = cleanPrefix(it.song.title),
                                duration = durationTextToMillis(
                                    it.song.durationText
                                        ?: ""
                                ).milliseconds,
                                album = it.albumTitle
                            )?.onSuccess { lyrics ->
                                Database.upsert(
                                    Lyrics(
                                        songId = it.asMediaItem.mediaId,
                                        fixed = currentLyrics?.fixed,
                                        synced = lyrics?.text.orEmpty()
                                    )
                                )
                            }?.onFailure { lyrics ->
                                runCatching {
                                    KuGou.lyrics(
                                        artist = it.song.artistsText
                                            ?: "",
                                        title = cleanPrefix(
                                            it.song.title
                                        ),
                                        duration = durationTextToMillis(
                                            it.song.durationText
                                                ?: ""
                                        ) / 1000
                                    )?.onSuccess { lyrics ->
                                        Database.upsert(
                                            Lyrics(
                                                songId = it.asMediaItem.mediaId,
                                                fixed = currentLyrics?.fixed,
                                                synced = lyrics?.value.orEmpty()
                                            )
                                        )
                                    }?.onFailure {}
                                }.onFailure {}
                            }
                        }.onFailure {}
                    }
                }
        }
    }
}
