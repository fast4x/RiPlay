package it.fast4x.chaquopy

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import it.fast4x.riplay.PythonEngine
import it.fast4x.riplay.PythonResponse

class PythonEngineImpl : PythonEngine {
    override fun executeScript(url: String, privateDir: String): PythonResponse {
        val py = Python.getInstance()
        val result =  py.getModule("MusicVault")
            .callAttr("download_audio", url, privateDir)

        val path              = result.callAttr("get", "path").toString()
        val fileName          = result.callAttr("get", "filename").toString()
        val thumbnailFileName = result.callAttr("get", "thumbnail_filename").toString()
        val title             = result.callAttr("get", "title").toString()
        val duration          = result.callAttr("get", "duration").toInt()
        val artist            = result.callAttr("get", "artist").toString()

        return PythonResponse(
            path = path,
            fileName = fileName,
            thumbnailFileName = thumbnailFileName,
            title = title,
            duration = duration,
            artist = artist
        )
    }

    override fun testAndStartChaquopy(context: Context): Triple<String, String, Boolean> {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()

        // Test 1: Python funziona?
        val sys = py.getModule("sys")
        val pyVersion = sys["version"].toString()

        // Test 2: yt-dlp è installato?
        val ytdlp = py.getModule("yt_dlp")
        val ytdlpVersion = ytdlp["version"]?.get("__version__").toString()

        val ytdlpIsReady = (pyVersion.isNotEmpty() && ytdlpVersion.isNotEmpty())

        return Triple(pyVersion, ytdlpVersion, ytdlpIsReady)
    }
}