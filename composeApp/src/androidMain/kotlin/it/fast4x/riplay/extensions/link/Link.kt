package it.fast4x.riplay.extensions.link

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import timber.log.Timber
import java.io.InputStream

import java.io.OutputStream

fun buildLinkClient(
    address: String = "localhost",
    port: Int = 8000
): LinkClient {

    val client = LinkClient(address, port)
    client.run()

    return client
}

class LinkClient(var hostAddress: String, var port: Int) {

    lateinit var inputStream: InputStream
    lateinit var outputStream: OutputStream
    lateinit var socket: io.ktor.network.sockets.Socket
    lateinit var sendChannel: ByteWriteChannel
    lateinit var selectorManager: SelectorManager


    fun run() {

        CoroutineScope(Dispatchers.IO).launch {
            selectorManager = SelectorManager(Dispatchers.IO)
            socket = aSocket(selectorManager)
                .tcp().connect(hostAddress, port) {
                    keepAlive = true
                }

            //val receiveChannel = socket.openReadChannel()
            sendChannel = socket.openWriteChannel(autoFlush = true)


//            while (true) {
//                val greeting = receiveChannel.readUTF8Line()
//                if (greeting != null) {
//                    println(greeting)
//                } else {
//                    println("Server closed a connection")
//                    socket.close()
//                    selectorManager.close()
//
//                }
//            }
        }

    }

    suspend fun send(message: String){
        try {
            sendChannel.writeStringUtf8("$message\n")
            Timber.d("LinkClient Sent: $message")
        }catch (ex: IOException){
            println("LinkClient Error sending data: ${ex.message}")
        }
    }

    fun close() {
        socket.close()
        selectorManager.close()
    }


}

