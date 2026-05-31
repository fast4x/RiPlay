package it.fast4x.riplay.musicvault

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MusicVaultEvents {
    private val _events = MutableSharedFlow<MusicVaultEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<MusicVaultEvent> = _events.asSharedFlow()

    fun emit(event: MusicVaultEvent) {
        _events.tryEmit(event)
    }
}

sealed class MusicVaultEvent {
    data class DownloadCompleted(
        val songId: String,
        val fileName: String,
        val thumbnailFileName: String
    ) : MusicVaultEvent()

    data class DownloadRemoved(val songId: String) : MusicVaultEvent()
}