package it.fast4x.riplay.extensions.rewind.slides

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.GradientFlow
import com.mikepenz.hypnoticcanvas.shaders.MeshGradient
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.rewind.data.AnimatedContent
import it.fast4x.riplay.extensions.rewind.data.AnimationType
import it.fast4x.riplay.extensions.rewind.data.RewindSlide
import it.fast4x.riplay.extensions.rewind.data.slideTitleFontSize
import it.fast4x.riplay.extensions.rewind.utils.colorsList
import it.fast4x.riplay.extensions.visualbitmap.VisualBitmapCreator
import it.fast4x.riplay.ui.items.SongItem
import it.fast4x.riplay.ui.styling.Dimensions
import it.fast4x.riplay.ui.styling.px
import kotlinx.coroutines.delay
import kotlin.random.Random


@Composable
fun IntermediateSlide(slide: RewindSlide.Intermediate, isPageActive: Boolean = false) {

        var isContentVisible by remember { mutableStateOf(false) }

        LaunchedEffect(isPageActive) {
            if (isPageActive) {
                delay(100)
                isContentVisible = true
            } else {
                isContentVisible = false
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ), label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shaderBackground(MeshGradient(
                    (colorsList()[Random.nextInt(0, colorsList().size -1 )]).toTypedArray()
                )),
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
                    delay = 500,
                    animationType = AnimationType.SLIDE_FROM_UP
                ) {
                    Text(
                        text = slide.message,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                AnimatedContent(
                    isVisible = isContentVisible,
                    delay = 500,
                    animationType = AnimationType.SLIDE_FROM_UP
                ) {
                    Text(
                        text = slide.subMessage,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                AnimatedContent(
                    isVisible = isContentVisible,
                    delay = 500,
                    animationType = AnimationType.SLIDE_FROM_UP
                ) {
                    Text(
                        text = slide.message1,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                AnimatedContent(
                    isVisible = isContentVisible,
                    delay = 500,
                    animationType = AnimationType.SLIDE_FROM_UP
                ) {
                    Text(
                        text = slide.subMessage1,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(46.dp))

                AnimatedContent(isVisible = isContentVisible, delay = 1500) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Swipe to continue",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.chevron_forward),
                            contentDescription = "Swipe",
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .scale(scale)
                        )
                    }
                }

            }
        }

}