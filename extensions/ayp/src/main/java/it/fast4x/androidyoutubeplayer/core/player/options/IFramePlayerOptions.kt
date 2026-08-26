package it.fast4x.androidyoutubeplayer.core.player.options

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

/**
 * Options used to configure the IFrame Player. All critical parameters for volume
 * management (origin, autoplay, mute) are hard-locked internally to prevent audio fading.
 */
class IFramePlayerOptions private constructor(private val playerOptions: JSONObject) {

  companion object {
    private val fallbackUrl = "https://youtube.com"

    // Imposto controls = 1 per scongiurare  problemi di rendering dovuti alle possibili restrizioni di sicurezza
    fun getDefault(context: Context) = Builder(context).controls(1).build()
  }

  override fun toString(): String {
    return playerOptions.toString()
  }

  // Fallback sicuro e centralizzato per l'Origin
  internal fun getOrigin(): String {
    return try {
      val origin = playerOptions.optString(Builder.ORIGIN, "")
      if (origin.isBlank() || origin == "null")
        fallbackUrl
      else origin

    } catch (e: Exception) {
      fallbackUrl
    }
  }

  class Builder(context: Context) {
    companion object {
      internal const val AUTO_PLAY = "autoplay"
      internal const val MUTE = "mute"
      internal const val CONTROLS = "controls"
      internal const val ENABLE_JS_API = "enablejsapi"
      internal const val FS = "fs"
      internal const val ORIGIN = "origin"
      internal const val REL = "rel"
      internal const val IV_LOAD_POLICY = "iv_load_policy"
      internal const val CC_LOAD_POLICY = "cc_load_policy"
      internal const val CC_LANG_PREF = "cc_lang_pref"
      internal const val LIST = "list"
      internal const val LIST_TYPE = "listType"
      internal const val START = "start"
      internal const val END = "end"
    }

    private val builderOptions = JSONObject()

    init {
      // Impostazioni di default
      addInt(AUTO_PLAY, 1) // Forza l'avvio immediato per stabilizzare i buffer
      addInt(MUTE, 0)      // Se abilitato avvia con volume zero a livello hardware per saltare il soft-start di Android
      addString(ORIGIN, fallbackUrl) // Dominio fiduciario per permessi codec massimi

      // Impostazioni standard modificabili
      addInt(CONTROLS, 0)
      addInt(ENABLE_JS_API, 1)
      addInt(FS, 0)
      addInt(REL, 0)
      addInt(IV_LOAD_POLICY, 3)
      addInt(CC_LOAD_POLICY, 0)
    }

    fun build(): IFramePlayerOptions {
      return IFramePlayerOptions(builderOptions)
    }

    fun controls(controls: Int): Builder {
      addInt(CONTROLS, controls)
      return this
    }

    // Deporchiamo o rendiamo inattivi i metodi pubblici per evitare sovrascritture esterne dannose
    @Deprecated("Forced internally to 1 to prevent system audio fading", ReplaceWith(""))
    fun autoplay(controls: Int): Builder {
      // Ignora l'input dell'utente e mantiene il valore di sicurezza 1
      addInt(AUTO_PLAY, 1)
      return this
    }

    @Deprecated("Forced internally to 1 to prevent system audio fading", ReplaceWith(""))
    fun mute(controls: Int): Builder {
      // Ignora l'input dell'utente e mantiene il valore di sicurezza 1
      addInt(MUTE, 1)
      return this
    }

    @Deprecated("Forced internally to 'https://youtube.com'", ReplaceWith(""))
    fun origin(origin: String): Builder {
      addString(ORIGIN, fallbackUrl)
      return this
    }

    // Tutti gli altri parametri non critici rimangono configurabili dall'utente...
    fun rel(rel: Int): Builder {
      addInt(REL, rel)
      return this
    }

    fun ivLoadPolicy(ivLoadPolicy: Int): Builder {
      addInt(IV_LOAD_POLICY, ivLoadPolicy)
      return this
    }

    fun langPref(languageCode: String): Builder {
      addString(CC_LANG_PREF, languageCode)
      return this
    }

    fun ccLoadPolicy(ccLoadPolicy: Int): Builder {
      addInt(CC_LOAD_POLICY, ccLoadPolicy)
      return this
    }

    fun list(list: String): Builder {
      addString(LIST, list)
      return this
    }

    fun listType(listType: String): Builder {
      addString(LIST_TYPE, listType)
      return this
    }

    fun fullscreen(fs: Int): Builder {
      addInt(FS, fs)
      return this
    }

    fun start(startSeconds: Int): Builder {
      addInt(START, startSeconds)
      return this
    }

    fun end(endSeconds: Int): Builder {
      addInt(END, endSeconds)
      return this
    }

    @Deprecated("Deprecated by YouTube and will have no effect")
    fun modestBranding(modestBranding: Int): Builder {
      return this
    }

    private fun addString(key: String, value: String) {
      try {
        builderOptions.put(key, value)
      } catch (e: JSONException) {
        throw RuntimeException("Illegal JSON value $key: $value")
      }
    }

    private fun addInt(key: String, value: Int) {
      try {
        builderOptions.put(key, value)
      } catch (e: JSONException) {
        throw RuntimeException("Illegal JSON value $key: $value")
      }
    }
  }
}
