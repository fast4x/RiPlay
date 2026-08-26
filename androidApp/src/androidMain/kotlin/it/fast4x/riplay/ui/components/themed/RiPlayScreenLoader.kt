package it.fast4x.riplay.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import it.fast4x.riplay.R

@Composable
fun RiPlayScreenLoader(
    modifier: Modifier = Modifier,
    onLoadingComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val loadingMessages = remember {
        listOf(
            context.resources.getString(R.string.loading_msg_1),
            context.resources.getString(R.string.loading_msg_2),
            context.resources.getString(R.string.loading_msg_3),
            context.resources.getString(R.string.loading_msg_4),
            context.resources.getString(R.string.loading_msg_5),
            context.resources.getString(R.string.loading_msg_6),
        )
    }

    var textIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (textIndex < loadingMessages.lastIndex) {
            delay(700.milliseconds)
            textIndex++
        }
        delay(1000.milliseconds)
        onLoadingComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.wrapContentSize()
        ) {

            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "Logo RiPlay",
                modifier = Modifier.size(70.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Play",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
        }

        Spacer(modifier = Modifier.height(56.dp))

        // Messaggi di caricamento in dissolvenza
        AnimatedContent(
            targetState = textIndex,
            label = "text_change",
            transitionSpec = {
                fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing))
                    .togetherWith(fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)))
            }
        ) { index ->
            Text(
                text = loadingMessages[index],
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .widthIn(max = 260.dp)
            )
        }
    }
}
