package it.fast4x.riplay.extensions.experimental.recommendationstrategy.strategies

import java.time.LocalDate

object RecommendationCopy {

    /**
     * Copy alternativi per ogni strategia.
     * Ruotano giornalmente in base al giorno dell'anno.
     */
    private val copyByStrategy: Map<String, List<Pair<String, String>>> = mapOf(
        "forgotten_gems" to listOf(
            "Dimenticati nel tempo" to "Brani che amavi ma non ascolti da mesi",
            "Riscopri i tuoi preferiti" to "Dalla tua libreria, con affetto",
            "Dalla tua libreria" to "Brani che meritano una seconda chance",
            "Un viaggio nel passato" to "Quello che ascoltavi e hai dimenticato",
            "Riprendi da dove avevi lasciato" to "Brani amati, tempo di risentirli"
        ),
        "quality_curator" to listOf(
            "Capolavori del genere" to "Album acclamati dalla community",
            "Album da scoprire" to "Capolavori secondo MusicBrainz",
            "I migliori secondo MB" to "Album votati dalla community",
            "Qualità garantita" to "Album con rating alto su MusicBrainz",
            "Curati per te" to "Album acclamati che potresti apprezzare"
        ),
        "deep_cuts" to listOf(
            "Tracce da scoprire" to "Brani meno noti di artisti che ami",
            "Oltre i singoli" to "Tracce d'album dei tuoi artisti",
            "Deep Cuts" to "I brani nascosti dei tuoi artisti preferiti",
            "Esplora il catalogo" to "Brani non ascoltati di artisti che ami",
            "Sfuggiti alla radio" to "Tracce d'album meritevoli"
        ),
        "era_explorer" to listOf(
            "Nello stesso periodo, altro genere" to "Cosa succedeva in altri generi",
            "Viaggi nel tempo" to "Cross-genre della tua epoca preferita",
            "Anni d'oro" to "Altri generi della tua decade",
            "Ponti musicali" to "Generi diversi, stessa epoca",
            "Era Explorer" to "Scoperte cross-genre per decade"
        ),
        "mb_graph_walk" to listOf(
            "Dallo stesso universo" to "Artisti correlati a quelli che ami",
            "Connessioni MB" to "Artisti collegati via MusicBrainz",
            "Dalla stessa scena" to "Collaborazioni e membri in comune",
            "Universi paralleli" to "Artisti correlati ai tuoi preferiti",
            "Grafo musicale" to "Connessioni scoperte da MusicBrainz"
        ),
        "new_releases" to listOf(
            "Novità" to "Le ultime uscite che potresti apprezzare",
            "Appena usciti" to "Novità musicali fresh",
            "Fresh & New" to "Uscite recenti per i tuoi gusti",
            "Sullo stereo questa settimana" to "Album usciti di recente",
            "Nuovo in città" to "Artisti emergenti e uscite recenti"
        ),
    )

    /**
     * Header della sezione principale (ruota giornalmente).
     */
    private val mainHeaders = listOf(
        "Per te dal tuo ascolto" to "Suggerimenti basati sulla tua libreria",
        "Scoperte per te" to "Basato sui tuoi gusti musicali",
        "Ispirati a te" to "Suggerimenti personalizzati",
        "La tua musica, riscoperta" to "Curato dal tuo ascolto",
        "Esplora da qui" to "Partendo da ciò che ami"
    )

    /**
     * Restituisce il copy del giorno per una strategia.
     * Usa giorno dell'anno + ID strategia per rotazione deterministica.
     */
    fun getCopyForStrategy(strategyId: String): Pair<String, String> {
        val copies = copyByStrategy[strategyId] ?: return "Suggerimenti" to ""
        val dayOfYear = LocalDate.now().dayOfYear
        // Rotazione deterministica: giorno + hash dell'ID per variabilità tra strategie
        val seed = dayOfYear + strategyId.hashCode().absoluteValue
        val index = seed % copies.size
        return copies[index]
    }

    /**
     * Restituisce l'header principale del giorno.
     */
    fun getMainHeader(): Pair<String, String> {
        val dayOfYear = LocalDate.now().dayOfYear
        val index = dayOfYear % mainHeaders.size
        return mainHeaders[index]
    }

    private val Int.absoluteValue: Int get() = if (this < 0) -this else this
}