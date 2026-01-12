package it.fast4x.riplay.extensions.ritune

import android.content.Context
import android.net.nsd.NsdServiceInfo
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.File

@Serializable
data class RiTuneDevices(
    var devices: List<RiTuneDevice> = emptyList<RiTuneDevice>(),
)

@Serializable
data class RiTuneDevice (
    val name: String,
    val host: String,
    val port: Int,
    var selected: Boolean = false,
)

fun NsdServiceInfo.toRiTuneDevice() = RiTuneDevice(
    name = this.serviceName,
    host = this.host.toString(),
    port = this.port,
)

fun String.toRiTuneDevice() = RiTuneDevice(
    name = this.split(",")[0],
    host = this.split(",")[1],
    port = this.split(",")[2].toInt(),
)

class LinkDevicesSelected(context: Context) {
    private val fileName: String = "LinkDevicesSelected.txt"
    private val file = File(context.filesDir, fileName)

    private val devicesLoaded = if (file.exists()) {
        try {
            file.readLines().map { line ->
                line.toRiTuneDevice()
            }
        } catch (e: Exception) {
            emptyList()
        }
    } else { emptyList() }

    fun saveDevices (devices: List<RiTuneDevice>) {
        val lines = devices.map { device ->
            "${device.name},${device.host},${device.port}"
        }
        try {
            file.writeText(lines.joinToString("\n"))
        } catch (e: Exception) {
            Timber.e("Error saving devices: ${e.message}")
        }

    }

    fun devices() = devicesLoaded


}