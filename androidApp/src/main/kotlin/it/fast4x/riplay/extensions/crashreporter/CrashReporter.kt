package it.fast4x.riplay.extensions.crashreporter

import android.content.Context
import it.fast4x.riplay.BuildConfig
import it.fast4x.riplay.utils.appContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashReporter (private val LOG_PATH: String) : Thread.UncaughtExceptionHandler {

    /**
     * Memoria persistente dei crash, letta dalla MainActivity al prossimo
     * avvio ([it.fast4x.riplay.MainActivity.showLastCrashIfNeeded]):
     * se il processo precedente è morto con un crash poco prima, il report
     * con lo stack trace viene mostrato UNA volta. Questo elimina il
     * classico "lampeggio" a ciclo infinito su Android TV: se l'apertura
     * automatica di CrashReporterActivity viene bloccata dal sistema
     * (background activity start), l'utente/developer vede comunque il
     * report al lancio successivo.
     */
    companion object {
        const val PREF_STARTUP_GUARD = "riplay_startup_guard"
        const val KEY_CRASH_COUNT = "consecutive_crashes"
        const val KEY_LAST_STACK = "last_stack"
        const val KEY_LAST_CRASH_TS = "last_crash_ts"
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Questo handler NON DEVE MAI lanciare a sua volta: se lo facesse il
        // processo morirebbe senza lasciare traccia e — tipico scenario
        // Android TV — l'app "lampeggia" dal launcher senza spiegazioni.
        try {
            // Save crash log to a file
            saveCrashLog(throwable)

            // Build crash log to sent to crash activity
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            printWriter.append("------------------------------------------------- \n")
            printWriter.append("--- RiPlay ${BuildConfig.VERSION_NAME} \n")
            printWriter.append("--- Crash Event ${LocalDateTime.now()} \n")
            printWriter.append("------------------------------------------------- \n")
            throwable.printStackTrace(printWriter)
            val stackTrace = stringWriter.toString()

            // Ricorda il crash per il prossimo avvio (vedi companion object)
            rememberCrashForNextLaunch(stackTrace)

            CrashReporterActivity.start(appContext(), stackTrace)
        } catch (_: Throwable) {
            // mai rilanciare dall'handler: il kill qui sotto deve avvenire comunque
        }

        // Terminate the app or perform any other necessary action
        android.os.Process.killProcess(android.os.Process.myPid());
        exitProcess(1)
    }

    private fun rememberCrashForNextLaunch(stackTrace: String) {
        try {
            val prefs = appContext()
                .getSharedPreferences(PREF_STARTUP_GUARD, Context.MODE_PRIVATE)
            // commit() e NON apply(): il processo sta per essere ucciso e con
            // apply() la scrittura asincrona potrebbe andare perduta.
            prefs.edit()
                .putInt(KEY_CRASH_COUNT, prefs.getInt(KEY_CRASH_COUNT, 0) + 1)
                .putString(KEY_LAST_STACK, stackTrace.take(20_000))
                .putLong(KEY_LAST_CRASH_TS, System.currentTimeMillis())
                .commit()
        } catch (_: Throwable) {
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        try {

            val logFile = File(
                LOG_PATH,
                "RiPlay_crash_log.txt"
            )
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            FileWriter(logFile, true).use { writer ->
                writer.append("------------------------------------------------- \n")
                writer.append("--- Crash Event ${LocalDateTime.now()} \n")
                printFullStackTrace(throwable,PrintWriter(writer))
            }

            // Copia anche nello storage esterno dell'app:
            // /storage/emulated/0/Android/data/<pkg>/files/logs/
            // molto più facile da recuperare via adb (o da un file manager)
            // su un Android TV, senza bisogno di permessi di root.
            try {
                val extDir = appContext().getExternalFilesDir(null)
                    ?.resolve("logs")
                    ?.apply { mkdirs() }
                if (extDir != null) {
                    FileWriter(File(extDir, "RiPlay_crash_log.txt"), true).use { writer ->
                        writer.append("------------------------------------------------- \n")
                        writer.append("--- Crash Event ${LocalDateTime.now()} \n")
                        printFullStackTrace(throwable, PrintWriter(writer))
                    }
                }
            } catch (_: Exception) {
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun printFullStackTrace(throwable: Throwable, printWriter: PrintWriter) {
        //printWriter.println()
        printWriter.print("FullStackTrace: \n")
        printWriter.print(throwable.toString()+"\n")
        throwable.stackTrace.forEach { element ->
            printWriter.print("\t $element \n")
        }
        val cause = throwable.cause
        if (cause != null) {
            printWriter.print("Caused by:\t")
            printFullStackTrace(cause, printWriter)
        }
        printWriter.print("\n")
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
