package it.fast4x.riplay.extensions.ritune.improved

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import it.fast4x.riplay.R
import it.fast4x.riplay.extensions.ritune.RiTuneDevice
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.colorPalette
import timber.log.Timber

@Composable
fun RiTuneSelector(
    onDismiss: () -> Unit,
    onSelect: (List<RiTuneDevice>) -> Unit
) {
    val deviceList = GlobalSharedData.riTuneDevices

//    LaunchedEffect(key1 = deviceList) {
//        riTuneDevices = deviceList
//    }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxWidth(.9f)
                .fillMaxHeight(.5f)
        ) {

            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(all = 10.dp),
                modifier = Modifier
                    .background(
                        colorPalette().background0
                    )
                    .height(400.dp)
            ) {
                item {
                    Text(
                        text = "Available RiTune Devices",
                        color = colorPalette().text,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )


                }
                items(
                    items = deviceList,
                    key = { "${it.host}:${it.port}" }
                ) { device ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable {
                                val index = deviceList.indexOf(device)

                                if (index >= 0) {
                                    val updatedDevice = device.copy(selected = !device.selected)

                                    GlobalSharedData.riTuneDevices[index] = updatedDevice

                                    onSelect(GlobalSharedData.riTuneDevices.toList())
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            icon = if (device.selected) R.drawable.cast_connected else R.drawable.cast_disconnected,
                            color = colorPalette().text,
                            enabled = true,
                            onClick = {},
                            modifier = Modifier
                                .size(32.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = device.name,
                            color = colorPalette().text,
                            modifier = Modifier.border(BorderStroke(1.dp, Color.Red))
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

}