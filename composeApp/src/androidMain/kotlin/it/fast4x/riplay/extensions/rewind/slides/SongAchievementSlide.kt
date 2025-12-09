package it.fast4x.riplay.extensions.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun SongAchievementSlide(slide: RewindSlide.SongAchievement, isPageActive: Boolean = false) {

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
                    text = "You have reached a new level!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 500) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {

                    Icon(
                        painter = painterResource(id = slide.albumArtRes),
                        contentDescription = slide.songTitle,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Gray.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 1000) {
                Text(
                    text = slide.songTitle,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 1500) {
                Text(
                    text = slide.artistName,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))


            AnimatedItem(isVisible = isContentVisible, delay = 2000) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = slide.level.goal,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = slide.level.description,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}