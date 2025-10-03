package it.fast4x.riplay.ui.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.colorPalette
import it.fast4x.riplay.typography
import it.fast4x.riplay.ui.styling.bold

@Composable
fun WelcomePage(){
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Welcome to RiPlay",
                style = TextStyle(
                    fontSize = typography().xl.bold.fontSize,
                    fontWeight = typography().xl.bold.fontWeight,
                    color = colorPalette().text,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 30.dp)
                )

        }
    }
}