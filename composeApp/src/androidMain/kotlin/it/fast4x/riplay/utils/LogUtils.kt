package it.fast4x.riplay.utils

import android.util.Log
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CaptureCrash (private val LOG_PATH: String) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Save crash log to a file
        saveCrashLog(throwable)

        // Terminate the app or perform any other necessary action
        android.os.Process.killProcess(android.os.Process.myPid());
        exitProcess(1)
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
                writer.append("------------------------------------------------- \n")
                printFullStackTrace(throwable,PrintWriter(writer))
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

class FileLoggingTree(private val logFile: File) : Timber.DebugTree() {

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
            val log = generateLog(priority, tag, message)
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            writeLog(logFile, log)
            ensureLogSize(logFile)
        }
    }

    private fun generateLog(priority: Int, tag: String?, message: String): String {
        val logTimeStamp = dateFormat.format(Date())

        return StringBuilder().append(logTimeStamp).append(" ")
            .append(getPriorityString(priority)).append(": ")
            .append(tag).append(" - ")
            .append(message).append('\n').toString()
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

        // We remove first 25% part of logs
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