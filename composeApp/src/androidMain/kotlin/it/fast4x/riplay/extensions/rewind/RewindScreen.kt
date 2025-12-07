package it.fast4x.riplay.extensions.rewind

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Composable principale che contiene il Pager
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewindScreen(pages: List<RewindPage>) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = Modifier.fillMaxSize()) {
        // Il Pager orizzontale che gestisce le slide
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            RewindPageComposable(page = pages[page])
        }

        // Indicatore di pagina in basso
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

// Composable per una singola slide
@Composable
fun RewindPageComposable(page: RewindPage) {
    // Animazione di ingresso per il contenuto
    val transition = rememberInfiniteTransition(label = "infinite")
    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(page.backgroundBrush)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Immagine principale, se presente
            page.imageRes?.let {
                // Usa AsyncImage se carichi da URL, altrimenti painterResource per le risorse locali
                // AsyncImage(model = "https://...", contentDescription = ...)
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .scale(scale)
                ) {
                    // Sostituisci con la tua immagine
                    // Per ora usa un'icona di sistema come placeholder
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = page.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = 0.2f), CircleShape)
                            .padding(32.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Statistica principale, se presente
            page.mainStat?.let {
                Text(
                    text = it,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = page.textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Titolo
            Text(
                text = page.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = page.textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Sottotitolo
            Text(
                text = page.subtitle,
                fontSize = 20.sp,
                color = page.textColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}


// Anteprima per il design
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    // Crea un tema Material3 per l'anteprima
    MaterialTheme {
        RewindScreen(pages = getRewindPages())
    }
}