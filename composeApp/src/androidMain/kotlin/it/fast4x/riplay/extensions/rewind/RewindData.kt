package it.fast4x.riplay.extensions.rewind

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dati che rappresentano una singola slide del nostro "Wrapped"
data class RewindPage(
    val id: Int,
    val title: String,
    val subtitle: String,
    val mainStat: String? = null, // Statistica principale (es. "1234 minuti")
    val imageRes: Int? = null,    // Risorsa dell'immagine (es. R.drawable.my_artist)
    val backgroundBrush: Brush,
    val textColor: Color = Color.White
)

// Funzione per generare dati fittizi
fun getRewindPages(): List<RewindPage> {
    // NOTA: Aggiungi delle immagini nella cartella res/drawable del tuo progetto
    // Ad esempio: artist_1.png, song_1.png, etc.
    return listOf(
        RewindPage(
            id = 0,
            title = "È arrivato il momento",
            subtitle = "Scopri il tuo anno in musica.",
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFF191414))
            )
        ),
        RewindPage(
            id = 1,
            title = "Il tuo artista top dell'anno",
            subtitle = "Hai ascoltato",
            mainStat = "The Weeknd",
            imageRes = android.R.drawable.ic_menu_gallery, // Sostituisci con la tua immagine
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
            )
        ),
        RewindPage(
            id = 2,
            title = "La tua canzone preferita",
            subtitle = "E hai ascoltato 'Blinding Lights' per ben",
            mainStat = "432 volte",
            imageRes = android.R.drawable.ic_menu_gallery, // Sostituisci con la tua immagine
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2196F3), Color(0xFF3F51B5))
            )
        ),
        RewindPage(
            id = 3,
            title = "Sei un ascoltatore seriale",
            subtitle = "Hai ascoltato musica per",
            mainStat = "45,678 minuti",
            imageRes = android.R.drawable.ic_menu_gallery, // Sostituisci con la tua immagine
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
            )
        ),
        RewindPage(
            id = 4,
            title = "Grazie per essere stato con noi",
            subtitle = "Non vediamo l'ora di ascoltare con te anche nel 2024.",
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1DB954), Color(0xFF191414))
            )
        )
    )
}