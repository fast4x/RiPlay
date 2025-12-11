package it.fast4x.riplay.extensions.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients
import com.mikepenz.hypnoticcanvas.shaders.GradientFlow
import com.mikepenz.hypnoticcanvas.shaders.MeshGradient
import com.mikepenz.hypnoticcanvas.shaders.MesmerizingLens
import com.mikepenz.hypnoticcanvas.shaders.Stripy
import it.fast4x.riplay.R
import it.fast4x.riplay.commonutils.cleanPrefix
import it.fast4x.riplay.extensions.rewind.data.AnimatedContent
import it.fast4x.riplay.extensions.rewind.data.AnimationType
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.ui.components.themed.Playlist
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.colorPalette
import kotlinx.coroutines.delay


@Composable
fun PlaylistAchievementSlide(slide: RewindSlide.PlaylistAchievement, isPageActive: Boolean = false) {
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
            .shaderBackground(GradientFlow),
//            .shaderBackground(
//                MeshGradient(
//                    arrayOf(Color(0xFFFF15E5), Color(0xFFFAAEF7), Color(0xFF6903F9)),
//                    scale = 1f
//                )
//            ),
            //.shaderBackground(MesmerizingLens),
            //.shaderBackground(GlossyGradients),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            AnimatedContent(isVisible = isContentVisible, delay = 500, animationType = AnimationType.SPRING_SCALE_IN) {
                Text(
                    text = slide.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            AnimatedContent(isVisible = isContentVisible, delay = 500) {

                if (slide.playlist != null)
                    Playlist(
                        playlist = slide.playlist.toPlaylistPreview(slide.songCount),
                        thumbnailSizeDp = 300.dp,
                        thumbnailSizePx = 300.dp.px,
                        alternative = true,
                        showName = false,
                        modifier = Modifier
                            .padding(top = 14.dp),
                        disableScrollingText = false,
                        thumbnailUrl = null
                    )
                else
                    Icon(
                        painter = painterResource(id = R.drawable.playlist),
                        contentDescription = "Playlist Icon",
                        modifier = Modifier.size(108.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )

            }

            Spacer(modifier = Modifier.height(16.dp))


            AnimatedContent(isVisible = isContentVisible, delay = 1000) {
                Text(
                    text = cleanPrefix(slide.playlistName),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }


            AnimatedContent(isVisible = isContentVisible, delay = 1500) {
                Text(
                    text = "${slide.songCount} songs",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))


            AnimatedContent(isVisible = isContentVisible, delay = 2000, animationType = AnimationType.SPRING_SCALE_IN) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = slide.level.title,
                            color = colorPalette().accent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = slide.level.goal.replace("%s", slide.totalMinutes.toString(), true),
                            color = Color.White,
                            fontSize = 26.sp,
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