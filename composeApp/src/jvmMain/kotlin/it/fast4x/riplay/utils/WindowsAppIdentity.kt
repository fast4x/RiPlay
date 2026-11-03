package it.fast4x.riplay.utils

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.WString
import java.io.File

/**
 * STEP 4a (richiesta utente n.1): su Windows la notifica multimediale di
 * sistema (System Media Transport Controls) mostrava "app sconosciuta"
 * invece di RiPlay, mentre pulsanti multimediali, artista e titolo brano
 * erano visibili.
 *
 * PERCHE': i metadati (titolo/artista) e i pulsanti arrivano dalla
 * MediaSession del player integrato (KCEF/Chromium, che espone i metadati
 * del brano YouTube in riproduzione), ma il NOME dell'app che Windows
 * mostra accanto viene risolto dall'IDENTITA' del processo che possiede la
 * sessione: un processo Win32 senza AppUserModelID (AUMID) esplicito e
 * senza voce nel registro finisce come "app sconosciuta".
 *
 * FIX (approccio standard per app desktop non-packaged, lo stesso usato da
 * Chromium/Electron con app.setAppUserModelId):
 *  1. SetCurrentProcessExplicitAppUserModelID(AUMID) sul processo
 *     principale: il browser KCEF vive IN-PROCESS dentro RiPlay.exe (le
 *     macchine a stati CEF/multimediale girano nel processo browser), quindi
 *     la sessione multimediale eredita questa identita';
 *  2. chiave di registro HKCU\Software\Classes\AppUserModelId\<AUMID> con
 *     DisplayName = RiPlay (+ IconUri verso l'eseguibile corrente): e'
 *     da qui che Windows legge nome e icona da mostrare nel pannello
 *     multimediale. HKCU non richiede privilegi di amministratore.
 *
 * Tutto e' BEST-EFFORT (runCatching): qualunque fallimento non impatta
 * l'avvio e il comportamento resta quello attuale ("app sconosciuta").
 * Nessuna nuova dipendenza: JNA (core) e' gia' nel classpath desktop con
 * vlcj; le scritture di registro usano il comando `reg` di Windows.
 *
 * La funzione NON fa nulla su Linux/macOS (notifica multimediale gestita
 * in modo diverso da quei sistemi: MPRIS / Now Playing, fuori da questo step).
 */
object WindowsAppIdentity {

    /**
     * AUMID stabile dell'app desktop (stesso stile del bundleID macOS
     * "it.fast4x.riplay.desktop" in composeApp/build.gradle.kts). Non cambiarlo
     * tra versioni: e' la chiave con cui Windows riconosce l'app.
     */
    const val AUMID = "it.fast4x.riplay.desktop"
    const val APP_NAME = "RiPlay"

    /** Chiamare UNA volta all'avvio (main.kt). STEP 4g: guardia run-once,
     *  ogni richiamo successivo e' no-op (niente processi `reg` ripetuti). */
    private val setupDone = java.util.concurrent.atomic.AtomicBoolean(false)

    fun setup() {
        if (!setupDone.compareAndSet(false, true)) {
            println("[RiPlay-Identity] setup gia' eseguito: skip (guardia run-once)")
            return
        }
        if (!isWindowsRuntime()) return
        runCatching { setProcessAumid() }
            .onFailure { println("[RiPlay-Identity] AUMID non impostato: ${it.message}") }
        runCatching { ensureRegistryDisplayName() }
            .onFailure { println("[RiPlay-Identity] DisplayName di registro non aggiornato: ${it.message}") }
    }

    private fun isWindowsRuntime(): Boolean =
        System.getProperty("os.name", "").lowercase().contains("windows")

    /**
     * shell32!SetCurrentProcessExplicitAppUserModelID via JNA core (già
     * presente con vlcj). Restituisce S_OK (0) quando l'identita' e' accettata.
     */
    private interface Shell32 : Library {
        @Suppress("FunctionName")
        fun SetCurrentProcessExplicitAppUserModelID(appId: WString): Int
    }

    private fun setProcessAumid() {
        val shell = Native.load("shell32", Shell32::class.java)
        val hr = shell.SetCurrentProcessExplicitAppUserModelID(WString(AUMID))
        println("[RiPlay-Identity] AUMID '$AUMID' impostato sul processo (HRESULT=$hr)")
    }

    /**
     * Registro HKCU (nessun admin necessario):
     *   HKCU\Software\Classes\AppUserModelId\<AUMID>
     *     DisplayName = RiPlay
     *     IconUri     = percorso dell'eseguibile corrente (se rintracciabile)
     * Scritta SOLO se manca o e' diversa (nessun processo `reg` a ogni avvio
     * quando tutto e' gia' a posto).
     */
    private fun ensureRegistryDisplayName() {
        val key = "HKCU\\Software\\Classes\\AppUserModelId\\$AUMID"

        val needsWrite = runCatching {
            val p = ProcessBuilder("reg", "query", key, "/v", "DisplayName")
                .redirectErrorStream(true)
                .start()
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            // chiave/valore assente OPPURE DisplayName diverso da RiPlay
            p.exitValue() != 0 || !out.contains("REG_SZ") || !out.contains(APP_NAME)
        }.getOrDefault(true)

        if (!needsWrite) {
            println("[RiPlay-Identity] DisplayName di registro gia' presente per '$AUMID'")
            return
        }

        val iconUri = runCatching {
            ProcessHandle.current().info().command().orElse(null)
        }.getOrNull()?.takeIf { File(it).isFile }

        val commands = buildList {
            add(listOf("reg", "add", key, "/v", "DisplayName", "/t", "REG_SZ", "/d", APP_NAME, "/f"))
            if (iconUri != null)
                add(listOf("reg", "add", key, "/v", "IconUri", "/t", "REG_SZ", "/d", iconUri, "/f"))
        }

        commands.forEach { cmd ->
            val ok = runCatching {
                val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
                val out = p.inputStream.bufferedReader().readText()
                (p.waitFor() == 0).also { if (!it) println("[RiPlay-Identity] reg out: $out") }
            }.getOrDefault(false)
            println("[RiPlay-Identity] registro ${if (ok) "OK" else "FALLITO"}: ${cmd.drop(4).joinToString(" ")}")
        }
    }
}
