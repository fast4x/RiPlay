package it.fast4x.riplay.extensions.rewind.slides

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.Heat
import it.fast4x.riplay.LocalPlayerServiceBinder
import it.fast4x.riplay.commonutils.thumbnail
import it.fast4x.riplay.extensions.rewind.data.AnimatedContent
import it.fast4x.riplay.extensions.rewind.data.AnimationType
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.extensions.rewind.data.slideTitleFontSize
import it.fast4x.riplay.extensions.rewind.utils.rewindPauseMedia
import it.fast4x.riplay.extensions.rewind.utils.rewindPlayMedia
import it.fast4x.riplay.extensions.visualbitmap.VisualBitmapCreator
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.resize
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun SongAchievementSlide(slide: RewindSlide.SongAchievement, isPageActive: Boolean = false) {

        var isContentVisible by remember { mutableStateOf(false) }

        val binder = LocalPlayerServiceBinder.current

        LaunchedEffect(isPageActive) {
            if (isPageActive) {

                rewindPlayMedia(slide.song, binder)

                delay(100)
                isContentVisible = true
            } else {
                rewindPauseMedia(binder)

                isContentVisible = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shaderBackground(Heat()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    isVisible = isContentVisible,
                    delay = 500,
                    animationType = AnimationType.SPRING_SCALE_IN
                ) {
                    Text(
                        text = slide.title,
                        color = Color.White,
                        fontSize = slideTitleFontSize,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    isVisible = isContentVisible,
                    delay = 500,
                    animationType = AnimationType.SPRING_SCALE_IN
                ) {
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
                                text = slide.level.title,
                                color = colorPalette().accent,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = slide.level.goal.replace(
                                    "%s",
                                    slide.minutesListened.toString(),
                                    true
                                ),
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

                Spacer(modifier = Modifier.height(16.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 1000) {

                    Box {
                        AsyncImage(
                            model = slide.albumArtUri.toString().resize(1200, 1200),
                            contentDescription = "loading...",
                            modifier = Modifier
                                .fillMaxWidth(.7f)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                        )

                    }


                }

                Spacer(modifier = Modifier.height(16.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 1500) {
                    Text(
                        text = slide.songTitle,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 2000) {
                    Text(
                        text = slide.artistName,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }

            }
        }

}