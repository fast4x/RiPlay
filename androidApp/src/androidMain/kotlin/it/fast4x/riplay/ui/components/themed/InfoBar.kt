package it.fast4x.riplay.ui.components.themed

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.styling.semiBold
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.toFlagEmoji
import it.fast4x.riplay.utils.typography

@Composable
fun InfoBar(year: String?, countryCode: String?) {
    if (year == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.calendar),
            contentDescription = "Year",
            tint = colorPalette().text,
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = year,
            style = typography().xs.semiBold,
            color = colorPalette().text
        )

        if (countryCode != null) {
            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = countryCode.toFlagEmoji(),
                style = typography().xxs.semiBold,
                color = colorPalette().text
            )
        }

    }
}