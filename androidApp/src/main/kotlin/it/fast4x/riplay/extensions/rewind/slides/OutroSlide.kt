package it.fast4x.riplay.extensions.rewind.slides

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BubbleRings
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.rewind.data.AnimatedContent
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.extensions.visualbitmap.VisualBitmapCreator

@Composable
fun OutroSlideComposable(slide: RewindSlide.OutroSlide, isPageActive: Boolean) {

        var isContentVisible by remember { mutableStateOf(false) }

        LaunchedEffect(isPageActive) {
            if (isPageActive) {
                isContentVisible = true
            } else {
                isContentVisible = false
            }
        }


        val infiniteTransition = rememberInfiniteTransition(label = "heartBeat")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ), label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shaderBackground(BubbleRings),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {

                AnimatedContent(isVisible = isContentVisible, delay = 0) {
                    Icon(
                        painter = painterResource(id = R.drawable.heart),
                        contentDescription = "Heart",
                        tint = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 500) {
                    Text(
                        text = stringResource(R.string.rw_thank_you),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 1000) {
                    Text(
                        text = stringResource(R.string.rw_the_music_you_love_is_a_piece_of_you_sharing_it_is_the_best_way_to_connect_with_others),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))


                AnimatedContent(isVisible = isContentVisible, delay = 1500) {
                    Text(
                        text = stringResource(R.string.rw_keep_listening_keep_sharing_keep_loving_never_hate_anyone),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

//            Spacer(modifier = Modifier.height(48.dp))
//
//
//            AnimatedContent(isVisible = isContentVisible, delay = 2000) {
//                Text(
//                    text = "RiPlay Rewind",
//                    color = Color.White.copy(alpha = 0.7f),
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Medium
//                )
//            }
            }
        }

}