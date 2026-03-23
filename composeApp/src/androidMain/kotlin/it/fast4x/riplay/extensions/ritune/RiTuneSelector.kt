package it.fast4x.riplay.extensions.ritune

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.TitleSection
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.colorPalette

@Composable
fun RiTuneSelector(
    onDismiss: () -> Unit,
) {
    val riTuneDevices = GlobalSharedData.riTuneDevices.value.distinctBy { it.host }.distinctBy { it.port }
    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight(.5f)
    ) {

        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(all = 10.dp),
            modifier = Modifier
                .background(colorPalette().background0)
        ) {
            item {
                TitleSection("RiTune Cast")
                Text(
                    text = "Available devices:",
                    color = colorPalette().text,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            items(
                count = riTuneDevices.size,
                key = { index -> riTuneDevices[index].name }
            ) { index ->

                val device = riTuneDevices[index]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clickable {
                            val devices = riTuneDevices.toMutableList()

                            val updatedDevice = device.copy(selected = !device.selected)

                            devices[index] = updatedDevice

                            GlobalSharedData.riTuneDevices.value = devices.toMutableStateList()

                            onDismiss()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        icon = if (device.selected) R.drawable.cast_connected else R.drawable.cast_disconnected,
                        color = colorPalette().text,
                        enabled = true,
                        onClick = {},
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = device.name,
                        color = colorPalette().text,
                        //modifier = Modifier.border(BorderStroke(1.dp, Color.Red))
                    )
                }
            }
        }

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter),
        )

    }

}