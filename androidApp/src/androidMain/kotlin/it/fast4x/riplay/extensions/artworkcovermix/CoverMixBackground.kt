package it.fast4x.riplay.extensions.artworkcovermix

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.compose.AsyncImage
import it.fast4x.riplay.appRunningInBackground
import it.fast4x.riplay.utils.BlurTransformation
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun CoverMixBackground(
    artworks: List<String>,
    fallbackColors: List<Color>,
    modifier: Modifier = Modifier,
    rotateInterval: Duration = 5.seconds,
) {
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(0) }

    // Rotazione del mix, ferma se l'app è in background
    LaunchedEffect(artworks) {
        index = 0
        while (artworks.size > 1) {
            delay(rotateInterval)
            if (!appRunningInBackground) index = (index + 1) % artworks.size
        }
    }

    // Colori dell'immagine corrente
    var topColor by remember { mutableStateOf(fallbackColors.getOrElse(0) { Color.Black }) }
    var bottomColor by remember { mutableStateOf(fallbackColors.getOrElse(1) { Color.Black }) }
    LaunchedEffect(artworks, index) {
        val url = artworks.getOrNull(index) ?: return@LaunchedEffect
        val info = ArtworkRepository.infoFor(context, url) ?: return@LaunchedEffect
        topColor = Color(info.darkMuted)   // in alto: scuro/muto → leggibilità barra
        bottomColor = Color(info.vibrant)  // in basso: saturo → carattere
    }

    val gradTop by animateColorAsState(topColor, tween(1800, easing = LinearEasing), label = "mixTop")
    val gradBottom by animateColorAsState(bottomColor, tween(1800, easing = LinearEasing), label = "mixBottom")

    // Ken Burns lento
    val kbScale by rememberInfiniteTransition(label = "kenBurns").animateFloat(
        initialValue = 1f, targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "kbScale"
    )

    val saturateFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.5f) })
    }

    Box(
        modifier
            .drawBehind { drawRect(Brush.verticalGradient(listOf(gradTop, gradBottom))) }
    ) {
        Crossfade(
            targetState = index,
            animationSpec = tween(1600, easing = LinearEasing),
            label = "artworkMix"
        ) { i ->
            val url = artworks.getOrNull(i) ?: return@Crossfade
            val backdropBlur = remember { BlurTransformation(scale = .9f, radius = 2) }

            val model = remember(url) {
                ImageRequest.Builder(context)
                    .data(url)
                    //.size(480) // non è necessario
                    .transformations(backdropBlur)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = saturateFilter,
                alpha = 0.6f,   // miscelazione col gradiente
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer { scaleX = kbScale; scaleY = kbScale }
            )
        }
        // leggibilità testi e controlli
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(0.25f),
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(0.45f)
                    )
                )
        )
    }
}