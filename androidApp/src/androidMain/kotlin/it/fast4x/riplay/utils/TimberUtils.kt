package it.fast4x.riplay.utils

import android.util.Log
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val logFile: File) : Timber.Tree() {

    private val maxLogSize = 5 * 1024 * 1024 // 5 MB
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault())

    private fun getPriorityString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> ""
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.DEBUG) {
            // Try-catch per evitare crash silenziosi di Timber
            try {
                val log = generateLog(priority, tag, message, t)

                // Sincronizzzione per evitare che thread multipli scrivano contemporaneamente
                synchronized(this) {
                    if (!logFile.exists()) {
                        logFile.createNewFile() // Può lanciare IOException se la cartella non esiste
                    }
                    writeLog(logFile, log)
                    ensureLogSize(logFile)
                }
            } catch (e: Exception) {
                // Se la scrittura fallisce, lo stampiamo in Logcat per saperlo perchè timber non sarebbe inizializzato
                Log.e("FileLoggingTree", "Errore scrittura log su file", e)
            }
        }
    }

    private fun generateLog(priority: Int, tag: String?, message: String, t: Throwable?): String {
        val logTimeStamp = dateFormat.format(Date())
        val sb = StringBuilder().append(logTimeStamp).append(" ")
            .append(getPriorityString(priority)).append(": ")
            .append(tag).append(" - ")
            .append(message).append('\n')

        // Se c'è un'eccezione, aggiungiamo lo stack trace al file!
        if (t != null) {
            sb.append(android.util.Log.getStackTraceString(t)).append('\n')
        }

        return sb.toString()
    }

    private fun writeLog(logFile: File, log: String) {
        val writer = FileWriter(logFile, true)
        writer.append(log)
        writer.flush()
        writer.close()
    }

    @Throws(IOException::class)
    private fun ensureLogSize(logFile: File) {
        if (logFile.length() < maxLogSize) return

        val startIndex = logFile.length() / 4

        val randomAccessFile = RandomAccessFile(logFile, "r")
        randomAccessFile.seek(startIndex)

        val into = ByteArrayOutputStream()

        val buf = ByteArray(4096)
        var n: Int
        while (true) {
            n = randomAccessFile.read(buf)
            if (n < 0) break
            into.write(buf, 0, n)
        }

        randomAccessFile.close()

        val outputStream = FileOutputStream(logFile)
        into.writeTo(outputStream)

        outputStream.close()
        into.close()
    }
}