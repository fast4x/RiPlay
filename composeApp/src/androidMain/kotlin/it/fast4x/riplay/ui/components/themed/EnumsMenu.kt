package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.ui.styling.medium
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.GenericMenuItem
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.drawCircle
import it.fast4x.riplay.utils.typography
import org.jetbrains.compose.resources.painterResource

@Composable
fun EnumsMenu(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    title: String,
    titleSecondary: String? = null,
    selectedValue: GenericMenuItem,
    values: List<GenericMenuItem>,
    onValueSelected: (GenericMenuItem) -> Unit,
    valueText: @Composable (GenericMenuItem) -> String = { it.toString() }
) {
    val colorPalette = colorPalette()

    Column(
        modifier = modifier
            .padding(all = 10.dp)
            .background(color = colorPalette.background1, shape = RoundedCornerShape(8.dp))
            .padding(vertical = 16.dp)
    ) {
        BasicText(
            text = title,
            style = typography().s.semiBold,
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 24.dp)
        )
        if (titleSecondary != null) {
            BasicText(
                text = titleSecondary,
                style = typography().xxs.semiBold,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 24.dp)
            )
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            values.forEach { value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                onDismiss()
                                onValueSelected(value)
                            }
                        )
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    if (selectedValue.titleId == value.titleId) {
                        Canvas(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = colorPalette.accent,
                                    shape = CircleShape
                                )
                        ) {
                            drawCircle(
                                color = colorPalette.onAccent,
                                radius = 4.dp.toPx(),
                                center = size.center,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    blurRadius = 4.dp.toPx(),
                                    offset = Offset(x = 0f, y = 1.dp.toPx())
                                )
                            )
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .size(18.dp)
                                .border(
                                    width = 1.dp,
                                    color = colorPalette.textDisabled,
                                    shape = CircleShape
                                )
                        )
                    }

                    Image(
                        painter = painterResource(value.iconId),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().text),
                        modifier = Modifier
                            .size(15.dp)
                    )

                    BasicText(
                        text = valueText(value),
                        style = typography().xs.medium
                    )
                }
            }
        }

    }

}