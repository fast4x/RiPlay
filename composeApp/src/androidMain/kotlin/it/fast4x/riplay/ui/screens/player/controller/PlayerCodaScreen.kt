package it.fast4x.riplay.ui.screens.player.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCodaScreen(
    playerViewModel: PlayerCodaViewModel
) {
    val inRiproduzione by playerViewModel.inRiproduzione.collectAsState()
    val indiceCorrente by playerViewModel.indiceCorrente.collectAsState()
    val statoPlayer by playerViewModel.statoPlayer.collectAsState()
    val coda = playerViewModel.codaRiproduzione

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player con Coda") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            val branoCorrente = coda[indiceCorrente]
            if (coda.isNotEmpty()) {

                Text(
                    text = "Sto ascoltando:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = branoCorrente.titolo ?: "Titolo Sconosciuto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = branoCorrente.artista ?: "Artista Sconosciuto",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.vaiAlPrecedente() }) {
                    Icon(painter = painterResource(id = R.drawable.play_skip_back), contentDescription = "Precedente")
                }
                FloatingActionButton(
                    onClick = {
                        if (inRiproduzione) {
                            playerViewModel.pause()
                        } else {
                            playerViewModel.play()
                        }
                    }
                ) {
                    Icon(
                        if (inRiproduzione) painterResource(id = R.drawable.pause) else painterResource(id = R.drawable.play),
                        contentDescription = if (inRiproduzione) "Pausa" else "Play"
                    )
                }
                IconButton(onClick = { playerViewModel.vaiAlProssimo() }) {
                    Icon(painter = painterResource(id = R.drawable.play_skip_forward), contentDescription = "Successivo")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Coda di Riproduzione",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(coda) { index, mediaItem ->
                    val isCorrente = index == indiceCorrente
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { playerViewModel.vaiA(index) }
                            .background(if (isCorrente) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaItem.titolo ?: "Titolo Sconosciuto",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mediaItem.artista ?: "Artista Sconosciuto",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (isCorrente) {
                            Icon(
                                painter = painterResource(id = R.drawable.volume_up),
                                contentDescription = "In riproduzione",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

//fun statoPlayerToString(stato: Int): String {
//    return when (stato) {
//        Player.STATE_IDLE -> "In Attesa"
//        Player.STATE_BUFFERING -> "Buffering"
//        Player.STATE_READY -> "Pronto"
//        Player.STATE_ENDED -> "Terminato"
//        else -> "Sconosciuto"
//    }
//}