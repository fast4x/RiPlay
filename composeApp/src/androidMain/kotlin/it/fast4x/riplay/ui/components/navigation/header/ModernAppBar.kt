package it.fast4x.riplay.ui.components.navigation.header

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavController
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.isEnabledFullscreen

@Composable
fun ModernTopAppBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    backButton: @Composable () -> Unit = {},
    context: Context,
) {

    var isVisible by remember { mutableStateOf(false) }

    val offsetY by animateIntAsState(
        targetValue = if (isVisible) 0 else -300,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val insetsPadding = if (isEnabledFullscreen()) TopAppBarDefaults.windowInsets
        .only(WindowInsetsSides.Horizontal).asPaddingValues()
    else TopAppBarDefaults.windowInsets.asPaddingValues()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .offset { IntOffset(0, offsetY) }
            .alpha(alpha)
            //.padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorPalette().background1.copy(alpha = 0.85f),
                            colorPalette().background1.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(insetsPadding)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                backButton()

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AppTitle(navController, context)
                }

                Spacer(modifier = Modifier.width(8.dp))

                ActionBar(navController)
            }
        }
    }
}