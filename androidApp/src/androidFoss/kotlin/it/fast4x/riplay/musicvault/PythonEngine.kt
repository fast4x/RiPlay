package it.fast4x.riplay.musicvault

import android.content.Context
import it.fast4x.riplay.PythonEngine
import it.fast4x.riplay.PythonResponse

val engine: PythonEngine = object : PythonEngine {
    override fun executeScript(url: String, privateDir: String): PythonResponse =
        PythonResponse("", "", "", "", 0, "")
    override fun testAndStartChaquopy(context: Context): Triple<String, String, Boolean> =
        Triple("", "", false)
}