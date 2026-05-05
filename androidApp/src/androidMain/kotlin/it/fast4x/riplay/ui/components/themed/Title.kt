package it.fast4x.riplay.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography

@Composable
fun Title(
    title: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 8.dp,
    @DrawableRes icon: Int? = R.drawable.arrow_forward,
    enableClick: Boolean = true,
    onClick: (() -> Unit)? = null,
    mini: Boolean? = false,
) {
    BoldSectionTitle(
        title,
        modifier,
        {
            if (onClick != null && enableClick) {
                Icon(
                    painter = painterResource(icon ?: R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = colorPalette().text,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            if (enableClick)
                                onClick.invoke()
                        }
                )
            }
        },
        mini = mini
    )
    /*
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            //.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick != null) {
                if (enableClick)
                    onClick?.invoke()
            }
            .padding(horizontal = 12.dp)
            .padding(top = verticalPadding, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = typography().l.bold.fontSize,
                fontWeight = typography().l.bold.fontWeight,
                color = colorPalette().text,
                textAlign = TextAlign.Start
            ),
            modifier = Modifier.weight(1f)

        )

        if (onClick != null && enableClick) {
            Icon(
                painter = painterResource(icon ?: R.drawable.arrow_forward),
                contentDescription = null,
                tint = colorPalette().text,
                modifier = Modifier.size(22.dp)
            )
        }
    }

     */
}

@Composable
fun Title2Actions(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes icon1: Int? = R.drawable.arrow_forward,
    @DrawableRes icon2: Int? = R.drawable.arrow_forward,
    enableClick: Boolean = true,
    onClick1: (() -> Unit)? = null,
    onClick2: (() -> Unit)? = null,
) {
    BoldSectionTitle(
        title,
        modifier,
        {
            if (onClick2 != null && enableClick) {
                Icon(
                    painter = painterResource(icon2 ?: R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = colorPalette().text,
                    modifier = Modifier
                        .clickable {
                            onClick2.invoke()
                        }
                        .padding(end = 12.dp)
                        .size(20.dp)
                )
            }

            if (onClick1 != null && enableClick) {
                Icon(
                    painter = painterResource(icon1 ?: R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = colorPalette().text,
                    modifier = Modifier
                        .clickable {
                            onClick1.invoke()
                        }
                        .size(20.dp)
                )
            }
        }
    )
    /*
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick1 != null) {
                if (enableClick)
                    onClick1?.invoke()
            }
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = typography().l.bold.fontSize,
                fontWeight = typography().l.bold.fontWeight,
                color = colorPalette().text,
                textAlign = TextAlign.Start
            ),
            modifier = Modifier.weight(1f)

        )
        if (onClick2 != null && enableClick) {
            Icon(
                painter = painterResource(icon2 ?: R.drawable.arrow_forward),
                contentDescription = null,
                tint = colorPalette().text,
                modifier = Modifier
                    .clickable {
                        onClick2.invoke()
                    }
                    .padding(end = 12.dp)
                    .size(20.dp)
            )
        }

        if (onClick1 != null && enableClick) {
            Icon(
                painter = painterResource(icon1 ?: R.drawable.arrow_forward),
                contentDescription = null,
                tint = colorPalette().text,
                modifier = Modifier
                    .clickable {
                        onClick1.invoke()
                    }
                    .size(20.dp)
            )
        }

    }

     */
}

@Composable
fun TitleSection(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = TextStyle(
            fontSize = typography().l.bold.fontSize,
            fontWeight = typography().l.bold.fontWeight,
            color = colorPalette().text,
            textAlign = TextAlign.Start
        ),
        modifier = modifier.padding(end = 12.dp)

    )


}

@Composable
fun TitleMiniSection(
    title: String,
    modifier: Modifier = Modifier
) {
    BoldSectionTitle(title = title, mini = true)
//    Text(
//        text = title,
//        style = TextStyle(
//            fontSize = typography().xs.semiBold.fontSize,
//            fontWeight = typography().xs.semiBold.fontWeight,
//            color = colorPalette().text,
//            textAlign = TextAlign.Start
//        ),
//        modifier = modifier.padding(top = 5.dp)
//    )
}

@Composable
fun BoldSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    mini: Boolean? = false
) {
    if (title.isEmpty()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (mini == false) {
            // Accent bar verticale a sinistra
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colorPalette().accent)
            )
            Spacer(Modifier.width(10.dp))
        }
        BasicText(
            text = title.uppercase(),
            style = (if (mini == false) typography().xs else typography().xxs).semiBold.copy(
                color = colorPalette().text,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailingContent?.invoke()
    }
}

@Composable
fun AlbumSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colorPalette().accent)
        )
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = title.uppercase(),
            style = typography().xs.semiBold.copy(
                color = colorPalette().text,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailingContent?.invoke()
    }
}
