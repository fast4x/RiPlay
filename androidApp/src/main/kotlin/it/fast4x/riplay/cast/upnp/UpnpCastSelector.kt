package it.fast4x.riplay.cast.upnp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import it.fast4x.riplay.R
import it.fast4x.riplay.cast.upnp.models.UpnpDevice
import it.fast4x.riplay.ui.components.themed.IconButton
import it.fast4x.riplay.ui.components.themed.TitleSection
import it.fast4x.riplay.ui.styling.bold
import it.fast4x.riplay.utils.GlobalSharedData
import it.fast4x.riplay.utils.colorPalette
import it.fast4x.riplay.utils.typography
import kotlinx.coroutines.launch

/**
 * Selettore dei dispositivi UPnP/DLNA (Sonos, smart TV, speaker DLNA...).
 * Controparte di RiTuneCastSelector: stessa UI, la sorgente dispositivi
 * e' la discovery SSDP invece di NSD, con fallback di inserimento IP manuale
 * per reti che filtrano il multicast.
 */
@Composable
fun UpnpCastSelector(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorPalette = colorPalette()

    var devices by remember { mutableStateOf(GlobalSharedData.upnpDevices.value) }
    var searching by remember { mutableStateOf(true) }
    var manualIp by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf(false) }

    // Identita' stabile del dispositivo (UDN se presente, altrimenti endpoint)
    fun deviceId(device: UpnpDevice) = device.udn ?: device.avTransportControlUrl

    // Discovery alla prima apertura del selettore: preserva la selezione corrente
    LaunchedEffect(Unit) {
        searching = true
        val found = UpnpDiscovery(context).discover()
        val previous = GlobalSharedData.upnpDevices.value
        devices = found.map { device ->
            device.copy(selected = previous.any { deviceId(it) == deviceId(device) && it.selected })
        }
        GlobalSharedData.upnpDevices.value = devices
        searching = false
    }

    fun select(device: UpnpDevice) {
        // il dispositivo puo' arrivare da discovery o da IP manuale: lo garantiamo in lista
        val known = devices.any { deviceId(it) == deviceId(device) }
        val base = if (known) devices else devices + device
        val updated = base.map {
            if (deviceId(it) == deviceId(device)) it.copy(selected = !it.selected)
            else it.copy(selected = false)
        }
        devices = updated
        GlobalSharedData.upnpDevices.value = updated
        onDismiss()
    }

    Box(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxHeight(.5f)
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(all = 10.dp),
            modifier = Modifier
                .background(colorPalette.background0)
        ) {
            item {
                TitleSection(stringResource(R.string.upnp_cast))
                Text(
                    text = if (searching)
                        stringResource(R.string.upnp_searching_devices)
                    else
                        stringResource(R.string.upnp_available_devices),
                    color = colorPalette.text,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            if (devices.isEmpty() && !searching)
                item {
                    Text(
                        text = stringResource(R.string.upnp_no_devices_found),
                        color = colorPalette.text,
                        modifier = Modifier
                            .padding(horizontal = 30.dp)
                            .padding(bottom = 10.dp)
                            .padding(top = 30.dp)
                    )
                }
            else
                items(
                    count = devices.size,
                    key = { index -> deviceId(devices[index]) }
                ) { index ->
                    val device = devices[index]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable { select(device) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            icon = if (device.selected) R.drawable.cast_connected else R.drawable.cast_disconnected,
                            color = colorPalette.text,
                            enabled = true,
                            onClick = {},
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                color = colorPalette.text,
                            )
                            Text(
                                text = "${device.host}:${device.port}",
                                color = colorPalette.text,
                                style = typography().xs.bold,
                            )
                        }
                    }
                }

            // Fallback: inserimento IP manuale (router con filtro multicast / AP isolation)
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.upnp_manual_connection),
                    color = colorPalette.text,
                    style = typography().xs.bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = manualIp,
                        onValueChange = {
                            manualIp = it
                            manualError = false
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        textStyle = androidx.compose.ui.text.TextStyle(color = colorPalette.text),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.background1)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (manualIp.isEmpty())
                                    Text(
                                        text = stringResource(R.string.upnp_manual_ip_hint),
                                        color = colorPalette.text,
                                        style = typography().xs.bold,
                                    )
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.background1)
                            .clickable {
                                val host = manualIp.trim().substringBefore(":")
                                val port = manualIp.trim().substringAfter(":", missingDelimiterValue = "1400")
                                    .toIntOrNull() ?: 1400
                                if (host.isBlank()) {
                                    manualError = true
                                } else
                                    scope.launch {
                                        val device = UpnpDiscovery(context).buildManualDevice(host, port)
                                        if (device != null) {
                                            select(device)
                                        } else
                                            manualError = true
                                    }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.upnp_connect),
                            color = colorPalette.text,
                            style = typography().xs.bold,
                        )
                    }
                }
                if (manualError)
                    Text(
                        text = stringResource(R.string.upnp_device_unreachable),
                        color = colorPalette.text,
                        style = typography().xs.bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
            }

        }



        if (searching)
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomCenter),
            )
    }
}

/** Riga indirizzo del relay: tocca per copiare negli appunti. */
@Composable
fun RelayLinkRow(label: String, url: String, onClick: () -> Unit) {
    val colorPalette = colorPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = colorPalette.text,
            style = typography().xs.bold,
        )
        Text(
            text = url,
            color = colorPalette.accent,
            style = typography().xs.bold,
        )
    }
}
