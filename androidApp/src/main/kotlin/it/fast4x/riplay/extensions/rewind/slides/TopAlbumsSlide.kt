package it.fast4x.riplay.extensions.rewind.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.MeshGradient
import it.fast4x.riplay.extensions.rewind.data.AnimatedContent
import it.fast4x.riplay.extensions.rewind.data.AnimationType
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.extensions.rewind.data.slideTitleFontSize
import it.fast4x.riplay.ui.items.AlbumItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import it.fast4x.riplay.utils.getRoundnessShape
import kotlinx.coroutines.delay


@Composable
fun TopAlbumsSlide(slide: RewindSlide.TopAlbums, isPageActive: Boolean = false) {

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
                .shaderBackground(
                    MeshGradient(
                        colors = listOf(
                            Color.Red.copy(alpha = 0.5f),
                            Color.Green,
                            Color.Magenta.copy(alpha = 0.5f)
                        ).toTypedArray()
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
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
                    delay = 2000,
                    animationType = AnimationType.SPRING_SCALE_IN
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(getRoundnessShape()),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {

                            slide.albums.forEachIndexed { index, it ->
                                if (it?.album == null) return@forEachIndexed

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)

                                        )
                                        Text(
                                            text = "${it.minutes} m",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Column {
                                        AlbumItem(
                                            album = it.album,
                                            thumbnailSizeDp = Dimensions.thumbnails.song,
                                            thumbnailSizePx = Dimensions.thumbnails.song.px,
                                            disableScrollingText = false,
                                            modifier = Modifier
                                        )

                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                }


                            }

                        }
                    }
                }
            }
        }

}