package it.fast4x.riplay

import android.content.Context

interface PythonEngine {
    fun executeScript(url: String, privateDir: String): PythonResponse

    fun testAndStartChaquopy(context: Context): Triple<String, String, Boolean>
}

data class PythonResponse(
    val path              : String,
    val fileName          : String,
    val thumbnailFileName : String,
    val title             : String,
    val duration          : Int,
    val artist            : String
)