package it.fast4x.riplay.extensions.link

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import okhttp3.OkHttpClient
import timber.log.Timber

import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

//fun buildLinkClientWeb(
//    address: String = "localhost",
//    port: Int = 8000,
//    ssl: Boolean = true
//): LinkServiceClientWeb {
//
//    val client = LinkServiceClientWeb(address, port, ssl)
//    client.run()
//
//    return client
//}

fun linkServiceClientSend(command: String, castToLinkDevice: Boolean){
    if (!castToLinkDevice) return

    //TODO Implement link client logic

    val client = LinkServiceClientWeb("192.168.68.123", 18443, true)
    try {
        client.send(command)
    } catch (e: Exception) {
        Timber.e("linkClientSend Error sending data: $e")
        println("linkClientSend Error sending data: ${e.message}")
    }

}


class LinkServiceClientWeb(var hostAddress: String, var port: Int, var ssl: Boolean = true) {

    val client = HttpClient(OkHttp) {
        expectSuccess = true
        engine {
            preconfigured = getUnsafeOkHttpClient()
        }
    }
    var response: HttpResponse? = null

    private fun buildRequest(command: String) = "${if (ssl) "https://" else "http://"}$hostAddress:$port/${command.split("&")[0].split("=")[1]}/?"

    fun send(command: String){
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val buildRequest = buildRequest(command)
                if (buildRequest.isEmpty()) return@launch
                response = try {
                    client.get(buildRequest){
                        url {
                            command.split("&").forEach {
                                val params = it.split("=")
                                if (params.size > 1) {
                                    params[0].let { param ->
                                        params[1].let { value ->
                                            parameters.append(param, value)
                                        }
                                    }
                                }

                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("LinkServiceClientWeb Error sending data: $e")
                    println("LinkServiceClientWeb Error sending data: ${e.message}")
                    null
                }
                Timber.d("LinkServiceClientWeb Sent: $buildRequest$command")
                response?.bodyAsText()?.let {
                    Timber.d("LinkServiceClientWeb Response: $it")
                }
            }

        }catch (ex: IOException){
            println("LinkServiceClientWeb Error sending data: ${ex.message}")
        }
    }


    private fun getUnsafeOkHttpClient(): OkHttpClient {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }.build()
    }


}