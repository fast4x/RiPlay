package it.fast4x.riplay.server

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import it.fast4x.riplay.utils.getFileNameFromUri
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * RiPlay Relay — wiring ANDROID del modulo server condiviso (relayserver).
 *
 * Il modulo `modules/relayserver` contiene il server completo (engine Ktor,
 * endpoint, whitelist, Range, M3U, token): qui restano SOLE le parti che
 * dipendono dalla piattaforma Android, iniettate nel modulo all'avvio:
 *
 * - [AndroidRelayStreamSource]: byte dei brani da file:// (RandomAccessFile,
 *   seek esatto per il Range) e content:// (ContentResolver, skip per il seek)
 * - [AndroidRelayTokenStore]: token in SharedPreferences ("relay_prefs"),
 *   STESSO storage della versione pre-refactor: i link M3U degli utenti
 *   restano stabili dopo l'aggiornamento
 * - [installAndroidRelayServer]: registro del provider + resolver MIME
 *   (MimeTypeMap, comportamento identico a prima) + sink log verso Timber
 *
 * Chiamare [installAndroidRelayServer] una sola volta, in MainApplication.onCreate,
 * PRIMA di qualunque uso di [RelayManager].
 */

/**
 * Installa il wiring Android del server di streaming nel modulo condiviso.
 * Idempotente (chiamate successive sovrascrivono il provider: nessun effetto).
 */
fun installAndroidRelayServer(context: Context) {
    // Log del relay verso Timber: stessa integrazione del logcat e del file
    // di log di prima (tag "Relay", "RelayServer", "RelayManager").
    RelayLog.sink = RelayLog.Sink { level, tag, message, error ->
        when (level) {
            RelayLog.Level.DEBUG -> Timber.tag(tag).d(message)
            RelayLog.Level.INFO -> Timber.tag(tag).i(message)
            RelayLog.Level.WARN -> error?.let { Timber.tag(tag).w(it, message) }
                ?: Timber.tag(tag).w(message)
            RelayLog.Level.ERROR -> error?.let { Timber.tag(tag).e(it, message) }
                ?: Timber.tag(tag).e(message)
        }
    }

    // MIME: prima MimeTypeMap (come nella versione pre-refactor), poi la tabella
    // interna del modulo per le estensioni che MimeTypeMap non mappa.
    RelayMime.extensionResolver = { extension ->
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    val appContext = context.applicationContext
    RelayManager.install(RelayServerProvider {
        RelayServer(
            registry = RelayManager.registry,
            source = AndroidRelayStreamSource(appContext),
            tokenStore = AndroidRelayTokenStore(appContext),
        )
    })
    Timber.tag("RelayManager").i("wiring Android del server di streaming installato")
}

/** Persistenza del token su SharedPreferences: storage storico del relay Android. */
private class AndroidRelayTokenStore(context: Context) : RelayTokenStore {

    private val prefs = context.getSharedPreferences("relay_prefs", Context.MODE_PRIVATE)

    override fun load(): String? = prefs.getString("token", null)

    override fun save(token: String) {
        prefs.edit().putString("token", token).apply()
    }
}

/**
 * Sorgente dei byte per i brani registrati (ex LocalSource interna di RelayServer):
 * file:// via RandomAccessFile (Range esatto), content:// via ContentResolver
 * (skip per il seek). URI di altri schemi: null (404).
 */
private class AndroidRelayStreamSource(private val context: Context) : RelayStreamSource {

    override fun probe(track: RelayTrack): RelayMeta? {
        val uri = runCatching { Uri.parse(track.uri) }.getOrNull() ?: return null
        val size = localContentLength(uri) ?: return null
        if (size <= 0) return null
        return RelayMeta(totalSize = size, mime = track.mime ?: RelayMime.fromFileName(fileNameOf(uri)))
    }

    override fun open(track: RelayTrack, range: HttpRange?): InputStream? {
        val uri = runCatching { Uri.parse(track.uri) }.getOrNull() ?: return null
        return openLocalStream(uri, range)
    }

    private fun fileNameOf(uri: Uri): String? = context.getFileNameFromUri(uri)

    private fun localContentLength(uri: Uri): Long? {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: return null).takeIf { it.isFile }?.length()
                "content" -> {
                    val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
                    val length = afd?.use { it.length } ?: -1L
                    if (length > 0) length
                    else
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            var total = 0L
                            val skip = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(skip)
                                if (read <= 0) break
                                total += read
                            }
                            total
                        }
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.w("RelayServer probe $uri failed: ${e.message}")
            null
        }
    }

    private fun openLocalStream(uri: Uri, range: HttpRange?): InputStream? {
        return try {
            when (uri.scheme) {
                "file" -> {
                    val file = File(uri.path ?: return null)
                    if (!file.isFile) return null
                    val raf = RandomAccessFile(file, "r")
                    if (range != null) raf.seek(range.start)
                    // RandomAccessFile close: delegato al chiamante via InputStream.close
                    object : InputStream() {
                        override fun read(): Int = raf.read()
                        override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
                        override fun close() { raf.close() }
                    }
                }

                "content" -> {
                    val input = context.contentResolver.openInputStream(uri) ?: return null
                    if (range != null && range.start > 0) skipFully(input, range.start)
                    input
                }

                else -> null
            }
        } catch (e: Exception) {
            Timber.w("RelayServer open $uri range=$range failed: ${e.message}")
            null
        }
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                // skip() puo' restare a 0: consuma un byte alla volta
                if (input.read() < 0) return
                remaining -= 1
            }
        }
    }
}
