package it.fast4x.riplay.extensions.link

import android.content.Context
import android.net.nsd.NsdServiceInfo
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.File

@Serializable
data class LinkDevices(
    var devices: List<LinkDevice> = emptyList<LinkDevice>(),
)

@Serializable
data class LinkDevice (
    val name: String,
    val host: String,
    val port: Int,
    val selected: Boolean = false,
)

fun NsdServiceInfo.toLinkDevice() = LinkDevice(
    name = this.serviceName,
    host = this.host.toString(),
    port = this.port,
)

fun String.toLinkDevice() = LinkDevice(
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
                line.toLinkDevice()
            }
        } catch (e: Exception) {
            emptyList()
        }
    } else { emptyList() }

    fun saveDevices (devices: List<LinkDevice>) {
        val lines = devices.map { device ->
            "${device.name},${device.host},${device.port}"
        }
        try {
            file.writeText(lines.joinToString("\n"))
        } catch (e: Exception) {
            println("Error saving devices: ${e.message}")
            Timber.e("Error saving devices: ${e.message}")
        }

    }

    fun devices() = devicesLoaded


}