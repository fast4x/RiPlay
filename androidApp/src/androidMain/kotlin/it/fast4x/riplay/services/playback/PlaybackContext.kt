package it.fast4x.riplay.services.playback

import it.fast4x.riplay.enums.PlaybackOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mantiene il contesto del playback corrente.
 * Aggiornato da ogni punto che avvia un brano (suggerimenti, search, library, related).
 */
object PlaybackContext {

    // todo gestire i vari contesti di avvio di una canzone

    private val _currentOrigin = MutableStateFlow(PlaybackOrigin.UNKNOWN)
    val currentOrigin: StateFlow<PlaybackOrigin> = _currentOrigin.asStateFlow()

    private val _currentSuggestionInfo = MutableStateFlow<SuggestionInfo?>(null)
    val currentSuggestionInfo: StateFlow<SuggestionInfo?> = _currentSuggestionInfo.asStateFlow()

    /**
     * Chiamato quando l'utente clicca su un suggerimento.
     */
    fun setFromSuggestion(strategyId: String, strategyName: String, reasons: List<String>, itemId: String) {
        _currentOrigin.value = PlaybackOrigin.SUGGESTION
        _currentSuggestionInfo.value = SuggestionInfo(strategyId, strategyName, reasons, itemId)
    }

    /**
     * Chiamato per tutte le altre origini (search, library, related, external).
     */
    fun setOrigin(origin: PlaybackOrigin) {
        _currentOrigin.value = origin
        _currentSuggestionInfo.value = null
    }

    /**
     * Reset completo (es. su stop).
     */
    fun clear() {
        _currentOrigin.value = PlaybackOrigin.UNKNOWN
        _currentSuggestionInfo.value = null
    }

    data class SuggestionInfo(
        val strategyId: String,
        val strategyName: String,
        val reasons: List<String>,
        val itemId: String
    )
}