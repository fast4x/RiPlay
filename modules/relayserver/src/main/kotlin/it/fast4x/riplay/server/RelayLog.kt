package it.fast4x.riplay.server

/**
 * RiPlay Relay — facciata di logging del modulo server condiviso.
 *
 * Il modulo relayserver e' Kotlin/JVM puro (consumato sia da androidApp sia
 * dal desktop): non puo' dipendere da Timber (Android only) ne' da println
 * diretto legato a una piattaforma. Ogni log del modulo passa da qui, verso
 * un [Sink] sostituibile a runtime:
 *
 * - DEFAULT (desktop, test JVM): println su stdout "[RiPlay-Relay] TAG/LEVEL msg"
 * - androidApp: sink che delega a Timber (installato in MainApplication.onCreate),
 *   cosi' i log del server finiscono in logcat e nel file di log con gli stessi
 *   tag di prima ("Relay", "RelayServer", "RelayManager")
 *
 * Il formatting dei messaggi replica Timber (String.format con varargs): i
 * call site spostati dal modulo Android hanno conservato le stringhe di
 * formato originali (%s, %d) senza nessuna modifica.
 */
object RelayLog {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    /** Destinazione dei log: una sola istanza attiva per processo. */
    fun interface Sink {
        fun log(level: Level, tag: String, message: String, error: Throwable?)
    }

    @Volatile
    var sink: Sink = defaultSink()

    // ─────────────────── API stile Timber (format + varargs) ───────────────────

    fun d(tag: String, message: String, vararg args: Any?) =
        dispatch(Level.DEBUG, tag, message, args, null)

    fun i(tag: String, message: String, vararg args: Any?) =
        dispatch(Level.INFO, tag, message, args, null)

    fun w(tag: String, message: String, vararg args: Any?) =
        dispatch(Level.WARN, tag, message, args, null)

    fun w(error: Throwable, tag: String, message: String, vararg args: Any?) =
        dispatch(Level.WARN, tag, message, args, error)

    fun e(tag: String, message: String, vararg args: Any?) =
        dispatch(Level.ERROR, tag, message, args, null)

    fun e(error: Throwable, tag: String, message: String, vararg args: Any?) =
        dispatch(Level.ERROR, tag, message, args, error)

    // ─────────────────── interno ───────────────────

    private fun dispatch(
        level: Level,
        tag: String,
        format: String,
        args: Array<out Any?>,
        error: Throwable?,
    ) {
        val message = if (args.isEmpty()) format
        else runCatching { format.format(*args) }.getOrDefault(format)
        runCatching { sink.log(level, tag, message, error) }
    }

    /**
     * Sink predefinito: stdout (desktop e test JVM). Formato compatto con
     * tag esplicito per il grep console: "[RiPlay-Relay] RelayServer/I msg".
     * Gli errori appendono message + stacktrace come fa Timber in logcat.
     */
    private fun defaultSink(): Sink = Sink { level, tag, message, error ->
        println("[RiPlay-Relay] $tag/${level.name}: $message")
        if (error != null) {
            println("[RiPlay-Relay] $tag/${level.name}:   caused by: $error")
        }
    }
}
