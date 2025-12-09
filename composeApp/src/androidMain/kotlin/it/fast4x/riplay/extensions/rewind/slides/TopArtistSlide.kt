package it.fast4x.riplay.extensions.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.extensions.rewind.data.AnimatedItem
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import kotlinx.coroutines.delay

@Composable
fun TopArtistSlide(slide: RewindSlide.TopArtist, isPageActive: Boolean = false) {

    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isPageActive) {
        if (isPageActive) {
            delay(100)
            isContentVisible = true
        } else {
            isContentVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(slide.backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {

            AnimatedItem(isVisible = isContentVisible, delay = 0) {
                Text(
                    text = "Your top artist of the year",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 500) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = slide.artistImageRes),
                        contentDescription = slide.artistName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedItem(isVisible = isContentVisible, delay = 1000) {
                Text(
                    text = slide.artistName,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }


            AnimatedItem(isVisible = isContentVisible, delay = 1500) {
                Text(
                    text = "${slide.minutesListened / 60} listening hours",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}