package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.environment.Environment
import it.fast4x.riplay.enums.ThumbnailRoundness
import it.fast4x.riplay.extensions.preferences.rememberPreference
import it.fast4x.riplay.extensions.preferences.thumbnailRoundnessKey
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.ui.styling.shimmer
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun MoodItemColored(
    mood: Environment.Mood.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val moodColor by remember { derivedStateOf { Color(mood.stripeColor) } }

    Column (
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(thumbnailRoundness.shape())
            .clickable { onClick() }

    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(color = moodColor)
                .padding(start = 10.dp)
                .fillMaxHeight(0.9f)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(150.dp)
                    .background(color = colorPalette().background4)
                    .fillMaxSize()
            ) {

                BasicText(
                    text = mood.title,
                    style = TextStyle(
                        color = colorPalette().text,
                        fontStyle = typography().xs.semiBold.fontStyle,
                        fontWeight = typography().xs.semiBold.fontWeight
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp).align(Alignment.CenterStart),
                    maxLines = 2,

                    )
            }
        }
    }
}

@Composable
fun MoodGridItemColored(
    mood: Environment.Mood.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailSizeDp: Dp
) {
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val moodColor by remember { derivedStateOf { Color(mood.stripeColor) } }

    Column (
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .size(thumbnailSizeDp,thumbnailSizeDp)
            .padding(5.dp)
            .clickable { onClick() }

    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(thumbnailRoundness.shape())

        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(color = moodColor)
                    .padding(start = 10.dp)
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .background(color = colorPalette().background4)
                        .fillMaxSize()
                ) {

                    BasicText(
                        text = mood.title,
                        style = TextStyle(
                            color = colorPalette().text,
                            fontStyle = typography().xs.semiBold.fontStyle,
                            fontWeight = typography().xs.semiBold.fontWeight
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp)
                            .align(Alignment.CenterStart),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
fun MoodItem(
    mood: Environment.Mood.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )


    Column (
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(thumbnailRoundness.shape())
            .clickable { onClick() }

    ) {
        Box(
            modifier = Modifier
                .requiredWidth(150.dp)
                .background(color = colorPalette().background4, shape = thumbnailRoundness.shape())
                .fillMaxWidth(0.9f)
                .padding(all = 10.dp)
        ){

            BasicText(
                text = mood.title,
                style =  TextStyle(
                    color = colorPalette().text,
                    fontStyle = typography().xs.semiBold.fontStyle,
                    fontWeight = typography().xs.semiBold.fontWeight
                ), //typography().xs.semiBold,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 1,

                )
        }
    }
}

@Composable
fun MoodGridItem(
    mood: Environment.Mood.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailSizeDp: Dp
) {
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )


    Column (
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .size(thumbnailSizeDp,thumbnailSizeDp)
            //.background(colorPalette().background1)
            .clip(thumbnailRoundness.shape())
            .clickable { onClick() }

    ) {
        Box(
            modifier = Modifier
                .background(color = colorPalette().background4, shape = thumbnailRoundness.shape())
                .fillMaxSize(0.9f)
                .padding(horizontal = 10.dp)
                .padding(vertical = 50.dp)
        ) {
            BasicText(
                text = mood.title,
                style = TextStyle(
                    color = colorPalette().text,
                    fontStyle = typography().xxl.semiBold.fontStyle,
                    fontWeight = typography().xxl.semiBold.fontWeight,
                    fontFamily = typography().xxl.fontFamily,
                    textAlign = TextAlign.Start
                ),
                modifier = modifier.padding(start = 4.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun MoodItemPlaceholder(
    width: Dp,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier
            .background(color = colorPalette().shimmer)
            .size(width, 64.dp)
    )
}