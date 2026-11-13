package it.fast4x.riplay.extensions.discord

import it.fast4x.riplay.utils.DesktopAppInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * PORTING DESKTOP di KizzyRPC (com.github.dead8309:KizzyRPC usato su
 * Android): client Discord Rich Presence via GATEWAY websocket.
 *
 * KizzyRPC e' distribuito come AAR (dipende da androidx.core) e NON puo'
 * girare su desktop JVM: questa classe replica il SUO PROTOCOLLO con le
 * sole librerie gia' presenti sul desktop (okhttp3 + kotlinx.serialization),
 * wire-format identico:
 *
 *  1. connessione a wss://gateway.discord.gg/?v=10&encoding=json;
 *  2. OP 10 (Hello) -> heartbeat ogni heartbeat_interval ms (OP 1, d = seq);
 *  3. IDENTIFY (OP 2) con il token dell'utente e intents = 0;
 *  4. PRESENCE UPDATE (OP 3) con l'attivita' da mostrare;
 *  5. clear = OP 3 con activities vuota; chiusura = close websocket.
 *
 * La struttura dell'attivita' e' la stessa della libreria Android
 * (Activity: name/details/state/type/application_id/timestamps/assets/
 * buttons/metadata): la presenza impostata da RiPlay desktop appare in
 * Discord esattamente come quella impostata da RiPlay Android.
 *
 * NB: stessa natura della libreria Android - connessione gateway
 * non ufficiale usata per la sola presence. Nessun dato viene letto
 * dal gateway oltre l'ack heartbeat (le classi OP 0 servono solo per
 * mantenere la sequenza).
 */
class DiscordRpcGateway(private val token: String) {

    companion object {
        private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
        private const val OP_DISPATCH = 0
        private const val OP_HEARTBEAT = 1
        private const val OP_IDENTIFY = 2
        private const val OP_PRESENCE = 3
        private const val OP_HELLO = 10
        private const val OP_HEARTBEAT_ACK = 11

        /** Pendenza LISTENING dell'attivita' (come TypeDiscordActivity Android). */
        const val ACTIVITY_TYPE_LISTENING = 2
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // websocket: nessun read timeout
        .pingInterval(0, TimeUnit.MILLISECONDS) // il heartbeat Discord fa da ping
        .build()

    @Volatile
    private var socket: WebSocket? = null

    private val lastSequence = AtomicReference<Long?>(null)
    private val heartbeatExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "riplay-discord-gw-heartbeat").apply { isDaemon = true }
    }

    @Volatile
    private var closedByUs = false

    /** true se il websocket e' aperto e identificato (nessuna garanzia extra). */
    val isConnected: Boolean
        get() = socket != null && !closedByUs

    /** Token in uso (lettura per la diagnostica della UI). */
    fun tokenPreview(): String = if (token.length > 8) token.take(4) + "…" + token.takeLast(4) else "…"

    init {
        connect()
    }

    private fun connect() {
        closedByUs = false
        val request = Request.Builder()
            .url(GATEWAY_URL)
            .header("User-Agent", "RiPlay/${DesktopAppInfo.VERSION}")
            .build()
        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    println("[RiPlay-Discord] gateway connesso (http ${response.code})")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (!closedByUs)
                        println("[RiPlay-Discord] gateway fallito: ${t.javaClass.simpleName}: ${t.message}")
                    heartbeatExecutor.shutdownNow()
                    socket = null
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (!closedByUs)
                        println("[RiPlay-Discord] gateway chiuso dal server: $code $reason")
                    heartbeatExecutor.shutdownNow()
                    socket = null
                }
            }
        )
    }

    private fun handleMessage(text: String) {
        runCatching {
            val obj = Json.parseToJsonElement(text).jsonObject
            val op = obj["op"]?.jsonPrimitive?.content?.toIntOrNull() ?: return
            when (op) {
                OP_HELLO -> {
                    val interval = obj["d"]?.jsonObject?.get("heartbeat_interval")
                        ?.jsonPrimitive?.content?.toLongOrNull() ?: 45_000L
                    startHeartbeat(interval)
                    sendIdentify()
                }
                OP_DISPATCH -> {
                    obj["s"]?.jsonPrimitive?.content?.toLongOrNull()?.let { lastSequence.set(it) }
                }
                OP_HEARTBEAT_ACK -> { /* ack ricevuto: connessione viva */ }
            }
        }.onFailure {
            println("[RiPlay-Discord] messaggio gateway non interpretato: ${it.message}")
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatExecutor.shutdownNow()
        heartbeatExecutor.scheduleAtFixedRate({
            try {
                val s = socket
                if (s != null) {
                    val payload = buildJsonObject {
                        put("op", OP_HEARTBEAT)
                        lastSequence.get()?.let { put("d", it) } ?: put("d", kotlinx.serialization.json.JsonNull)
                    }
                    s.send(payload.toString())
                }
            } catch (t: Throwable) {
                println("[RiPlay-Discord] heartbeat errore: ${t.message}")
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS)
    }

    private fun sendIdentify() {
        val payload = buildJsonObject {
            put("op", OP_IDENTIFY)
            putJsonObject("d") {
                put("token", token)
                put("intents", 0)
                putJsonObject("properties") {
                    put("os", System.getProperty("os.name") ?: "desktop")
                    put("browser", "RiPlay")
                    put("device", "RiPlay")
                }
            }
        }
        socket?.send(payload.toString())
        println("[RiPlay-Discord] IDENTIFY inviato (${tokenPreview()})")
    }

    /** Invia la presence (OP 3). Restituisce true se accettata dal websocket. */
    fun sendPresence(activity: JsonObject?, status: String = "online"): Boolean {
        val payload = buildJsonObject {
            put("op", OP_PRESENCE)
            putJsonObject("d") {
                put("since", kotlinx.serialization.json.JsonNull)
                putJsonArray("activities") {
                    if (activity != null) add(activity)
                }
                put("status", status)
                put("afk", false)
            }
        }
        val s = socket ?: return false
        return s.send(payload.toString())
    }

    /** Rimuove la presence (activities vuota) senza chiudere la connessione. */
    fun clearPresence(): Boolean = sendPresence(null)

    /** Chiude websocket ed heartbeat (come KizzyRPC.closeRPC). */
    fun closeConnection() {
        closedByUs = true
        runCatching { heartbeatExecutor.shutdownNow() }
        runCatching { socket?.close(1000, "RiPlay logout") }
        socket = null
    }
}
