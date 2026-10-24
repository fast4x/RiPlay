package it.fast4x.riplay.player.kcef

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Superficie video YouTube per desktop: incorpora il browser CEF (KCEF)
 * nel layout Compose tramite SwingPanel e mostra lo stato di installazione
 * del bundle (il download ~120-200 MB avviene solo al primo avvio).
 */
@Composable
fun KcefVideoSurface(
    modifier: Modifier = Modifier,
    controller: KcefYouTubePlayer
) {
    val kcefState by KcefEngine.state.collectAsState()

    // Sicurezza: se main() non l'ha gia' fatto, avvia il motore (idempotente)
    LaunchedEffect(Unit) { KcefEngine.start() }

    when (val s = kcefState) {
        is KcefState.Ready -> Box(modifier) {
            SwingPanel(
                factory = { controller.attachBrowser() },
                modifier = Modifier.fillMaxSize()
            )
            // STEP 2i: il player YT segnala onError (l'app mostra il CODICE
            // con il significato: la "An error occurred" di YouTube non
            // dice la causa, il codice si')
            val ytErr by controller.ytError.collectAsState()
            if (ytErr != 0) {
                Text(
                    "Errore YouTube (codice $ytErr: ${KcefEngine.ytErrorMeaning(ytErr)}). " +
                            "Dettagli nel log: righe [RiPlay-KCEF] / [RiPlay-YT].",
                    color = Color(0xFFFFC107),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color(0xCC000000))
                        .padding(6.dp)
                )
            }
        }

        is KcefState.Failed -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Motore di riproduzione KCEF non disponibile", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                s.message,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Dettagli completi nel log/console: righe che iniziano con [RiPlay-KCEF].",
                color = Color(0xFF8A8A8A),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { KcefEngine.retry() }) {
                Text("Riprova")
            }
        }

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stateLabel(s), color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            if (s is KcefState.Downloading) {
                LinearProgressIndicator(
                    progress = s.percent / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("${s.percent.toInt()}%", color = Color(0xFFB0B0B0))
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (KcefEngine.needsRestartAfterFirstDownload) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Il JDK in uso contiene un motore browser integrato di versione " +
                            "incompatibile con quello di RiPlay. Al termine del download " +
                            "l'applicazione si chiudera': riaprila per completare l'installazione.",
                    color = Color(0xFFFFC107),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun stateLabel(s: KcefState): String = when (s) {
    KcefState.NotStarted, KcefState.Locating ->
        "Preparazione del motore di riproduzione (KCEF)…"
    is KcefState.Downloading ->
        "Download del browser integrato (solo primo avvio)…"
    KcefState.Extracting ->
        "Estrazione dei componenti…"
    KcefState.Installing ->
        "Installazione…"
    KcefState.Initializing ->
        "Avvio del browser integrato…"
    else ->
        "Preparazione…"
}
