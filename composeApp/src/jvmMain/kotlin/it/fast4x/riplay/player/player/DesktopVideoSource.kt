package player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import it.fast4x.riplay.player.kcef.KcefVideoSurface
import it.fast4x.riplay.player.kcef.KcefYouTubePlayer
import it.fast4x.riplay.ui.bars.DefaultBottomBar

/**
 * STEP 2: sorgente video desktop ibrido.
 *
 *  - Tab "VLCJ": percorso attuale (FrameContainer pixel-buffer, test stream)
 *  - Tab "YouTube (KCEF)": riproduzione online con la stessa IFrame Player
 *    API usata su Android, via browser integrato KCEF.
 *
 * Il selettore e' temporaneo: STEP 3 rimuoverra' i tab di debug e
 * sceglie automaticamente il motore in base alla traccia (YouTube -> KCEF,
 * locale/altro -> VLCJ).
 *
 * STEP 2k: [videoId] e' la canzone selezionata nell'app (Song.id, che su
 * YouTube e' l'id del video). Senza questo collegamento il player KCEF non
 * riceve MAI un videoId (aveva solo l'input manuale in fondo al pannello) e
 * YouTube risponde code=2 "parametro non valido" perche' l'embed parte
 * senza ID. Quando [videoId] cambia, il player KCEF carica e avvia il video.
 */
@Composable
fun DesktopVideoSource(
    modifier: Modifier = Modifier,
    videoId: String? = null,
    vlcjContent: @Composable () -> Unit
) {
    val (currentTab, setCurrentTab) = remember { mutableStateOf(1) }

    Column(modifier) {
        TabRow(currentTab, Modifier.fillMaxWidth()) {
            Tab(currentTab == 0, onClick = { setCurrentTab(0) }, text = {
                Text("VLCJ")
            })
            Tab(currentTab == 1, onClick = { setCurrentTab(1) }, text = {
                Text("YouTube (KCEF)")
            })
        }
        when (currentTab) {
            0 -> Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                vlcjContent()
            }

            else -> YouTubeKcefPane(videoId, Modifier.weight(1f))
        }
    }
}

@Composable
private fun YouTubeKcefPane(
    videoId: String?,
    modifier: Modifier = Modifier
) {
    val controller = remember { KcefYouTubePlayer() }
    val (url, setUrl) = remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { controller.dispose() }
    }

    // STEP 2k: canzone selezionata nell'app -> player KCEF. Se il browser
    // non e' ancora pronto (altro tab attivo, KCEF in avvio) KcefYouTube
    // Player mette in pending e applica al momento dell'attach.
    LaunchedEffect(videoId) {
        val id = videoId?.trim().orEmpty()
        if (id.isNotEmpty()) controller.loadVideoById(id, 0f)
    }

    // PlayerInput gia' debuncia di 500 ms prima di chiamare onUrl
    LaunchedEffect(url) {
        url.takeIf { it.isNotBlank() }?.let { controller.load(it) }
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        KcefVideoSurface(
            Modifier.weight(1f).fillMaxWidth(),
            controller
        )
        DefaultBottomBar(Modifier.fillMaxWidth(), controller)
        PlayerInput { setUrl(it) }
    }
}
