package it.fast4x.riplay.utils

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.io.File
import java.util.concurrent.TimeUnit


object WindowsAppIdentity {

    const val AUMID = "it.fast4x.riplay.desktop"
    const val APP_NAME = "RiPlay"

    // ------------------------------------------------------------------
    // Diario su file (STEP 7a)
    // ------------------------------------------------------------------

    private val logLock = Any()
    private var logPathAnnounced = false

    private fun identityLogFile(): File =
        File(System.getProperty("java.io.tmpdir"), "riplay-identity.log")

    /** log su console E su file (con timestamp): la console puo' perdere
     *  righe (finestra chiusa, copia parziale), il file no. */
    private fun ilog(msg: String) {
        println("[RiPlay-Identity] $msg")
        runCatching {
            synchronized(logLock) {
                val f = identityLogFile()
                if (!logPathAnnounced) {
                    println("[RiPlay-Identity] (diario completo dell'identita' su: ${f.absolutePath})")
                    logPathAnnounced = true
                }
                val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
                f.appendText("[$ts] $msg\n", Charsets.UTF_8)
            }
        }
    }

    /** esegue un processo esterno con timeout; NULL se timeout/crash.
     *  L'output e' letto in un thread separato per non rischiare il
     *  blocco sul buffer del pipe mentre attendiamo la fine. */
    private fun runProcess(cmd: List<String>, timeoutSec: Long): Pair<String, Int>? = runCatching {
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val outFuture = java.util.concurrent.CompletableFuture.supplyAsync {
            p.inputStream.bufferedReader().readText()
        }
        val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
        if (!finished) {
            p.destroyForcibly()
            ilog("processo oltre il timeout di ${timeoutSec}s: ${cmd.firstOrNull()}")
            return null
        }
        val out = outFuture.get(5, TimeUnit.SECONDS)
        out to p.exitValue()
    }.getOrNull()

    // ------------------------------------------------------------------
    // Setup all'avvio
    // ------------------------------------------------------------------

    /** Chiamare UNA volta all'avvio (main.kt). Guardia run-once. */
    private val setupDone = java.util.concurrent.atomic.AtomicBoolean(false)

    fun setup() {
        if (!setupDone.compareAndSet(false, true)) {
            ilog("setup gia' eseguito: skip (guardia run-once)")
            return
        }
        ilog("setup avviato (os=${System.getProperty("os.name")}, " +
                "APPDATA=${System.getenv("APPDATA") ?: "<non impostata>"}, " +
                "cmd=${runCatching { ProcessHandle.current().info().command().orElse("?") }.getOrDefault("?")})")
        if (!isWindowsRuntime()) {
            ilog("setup: piattaforma non-Windows, nessuna azione")
            return
        }
        runCatching { setProcessAumid() }
            .onFailure { ilog("AUMID non impostato: ${it.message}") }
        runCatching { writeRegistryIdentity() }
            .onFailure { ilog("DisplayName di registro non aggiornato: ${it.message}") }

        // Diagnostica SMTC: script pronto in temp (lo scriviamo ORA cosi' il
        // watchdog di main.kt puo' eseguirlo anche se l'utente parte subito).
        runCatching { writeSmtcDiagScript() }
            .onFailure { ilog("script diagnostico SMTC non scrivibile: ${it.message}") }

        // Scorciatoia Start Menu con proprieta' AUMID. Thread separato: la
        // creazione (compilazione helper al primo avvio) puo' impiegare un
        // paio di secondi e non deve ritardare l'avvio. OGNI passo e' loggato.
        Thread {
            runCatching { ensureStartMenuShortcut() }
                .onFailure { ilog("shortcut Start Menu: errore inatteso: ${it.message}") }
        }.apply {
            name = "riplay-identity-shortcut"
            isDaemon = true
            start()
        }
    }


    fun reassertAfterCef() {
        if (!isWindowsRuntime()) return
        if (!setupDone.get()) return
        val current = runCatching { readbackAumid() }.getOrNull()
        if (current == AUMID) {
            ilog("verifica post-CEF OK: AUMID del processo = '$current'")
        } else {
            ilog("ATTENZIONE post-CEF: AUMID del processo e' '$current' (atteso '$AUMID') -> ripristino")
            runCatching { setProcessAumid() }
                .onFailure { ilog("ripristino AUMID fallito: ${it.message}") }
            runCatching { writeRegistryIdentity() }
                .onFailure { ilog("riscrittura registro fallita: ${it.message}") }
        }
        val regName = queryRegistryDisplayName()
        ilog("diagnostica: DisplayName registro = '${regName ?: "<assente>"}', " +
                "scorciatoia Start Menu = ${if (startMenuShortcutFile()?.isFile == true) "presente" else "ASSENTE"}")
    }

    // ------------------------------------------------------------------
    // STEP 7a: diagnostica delle sessioni SMTC reali
    // ------------------------------------------------------------------

    /** Elenca le sessioni multimediali DI SISTEMA con il loro
     *  SourceAppUserModelId (l'AUMID che Windows usa per il nome).
     *  [tag] distingue le chiamate nel diario (es. "45s", "105s"). */
    fun runSmtcSessionDiagnostic(tag: String) {
        if (!isWindowsRuntime()) return
        if (!setupDone.get()) return
        val script = smtcDiagScriptFile()
        if (script == null || !script.isFile) {
            ilog("SMTC-diag[$tag]: script diagnostico non disponibile")
            return
        }
        val res = runProcess(
            listOf("powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden", "-File", script.absolutePath),
            timeoutSec = 30
        )
        if (res == null) {
            ilog("SMTC-diag[$tag]: powershell non ha risposto entro il timeout")
            return
        }
        val (out, code) = res
        out.lineSequence().filter { it.isNotBlank() }.forEach { ilog("SMTC-diag[$tag] $it") }
        if (code != 0) ilog("SMTC-diag[$tag]: powershell exit=$code")
    }

    private fun smtcDiagScriptFile(): File? =
        runCatching { File(System.getProperty("java.io.tmpdir"), "riplay_smtc_diag.ps1") }.getOrNull()

    private fun writeSmtcDiagScript() {
        val f = smtcDiagScriptFile() ?: return
        f.writeText(SMTC_DIAG_SCRIPT.trimIndent(), Charsets.ISO_8859_1)
        ilog("script diagnostico SMTC pronto: ${f.absolutePath} (eseguibile anche a mano con un brano in riproduzione)")
    }


    private val SMTC_DIAG_SCRIPT = """
${'$'}ErrorActionPreference = 'Stop'
# 1) build/product di Windows
try {
  ${'$'}cv = Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion'
  Write-Output ('WIN-BUILD: ' + ${'$'}cv.CurrentBuildNumber + ' | ' + ${'$'}cv.ProductName + ' | ' + ${'$'}cv.DisplayVersion)
} catch { Write-Output ('WIN-BUILD-ERROR: ' + ${'$'}_.Exception.Message) }
# 2) metadata WinRT del tipo sessioni SMTC presenti?
try {
  ${'$'}w1 = Join-Path ${'$'}env:windir 'System32\WinMetadata\Windows.Media.Control.winmd'
  ${'$'}w2 = Join-Path ${'$'}env:windir 'System32\WinMetadata\Windows.Media.winmd'
  Write-Output ('WINMD-PRESENT: Control=' + (Test-Path ${'$'}w1) + ' Media=' + (Test-Path ${'$'}w2))
} catch { Write-Output ('WINMD-ERROR: ' + ${'$'}_.Exception.Message) }
# 3) app con AUMID viste dalla shell (con le scorciatoie Start Menu incluse)
try {
  ${'$'}apps = Get-StartApps | Where-Object { (${'$'}_.AppID -like '*riplay*') -or (${'$'}_.Name -like '*RiPlay*') }
  if (${'$'}apps) { foreach (${'$'}a in ${'$'}apps) { Write-Output ('START-APP: ' + ${'$'}a.AppID + ' | ' + ${'$'}a.Name) } }
  else { Write-Output 'START-APP: nessuna traccia di RiPlay nella lista Start' }
} catch { Write-Output ('START-APPS-ERROR: ' + ${'$'}_.Exception.Message) }
# 4) sessioni SMTC reali (richiede build >= 1809)
try {
  Add-Type -AssemblyName System.Runtime.WindowsRuntime
  # STEP 9: ContentType=WindowsRuntime (ERA "Content =": senza "Type" il
  # literal e' invalido e PowerShell non risolve mai il tipo - la causa del
  # SMTC-ERROR nei log step7/step8)
  ${'$'}mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
  ${'$'}null = ${'$'}mgrType
  ${'$'}asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { ${'$'}_.Name -eq 'AsTask' -and ${'$'}_.GetParameters().Count -eq 1 -and ${'$'}_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
  function Await([object]${'$'}WinRtTask, [type]${'$'}ResultType) {
    ${'$'}asTask = ${'$'}asTaskGeneric.MakeGenericMethod(${'$'}ResultType)
    ${'$'}netTask = ${'$'}asTask.Invoke(${'$'}null, @(${'$'}WinRtTask))
    ${'$'}netTask.Wait(-1) | Out-Null
    return ${'$'}netTask.Result
  }
  ${'$'}mgr = Await (${'$'}mgrType::RequestAsync()) ${'$'}mgrType
  ${'$'}sessions = ${'$'}mgr.GetSessions()
  if (${'$'}sessions.Count -eq 0) {
    Write-Output 'SMTC-SESSIONS: 0 (nessuna sessione attiva: riproduci un brano e riprova)'
    return
  }
  Write-Output ('SMTC-SESSIONS: ' + ${'$'}sessions.Count)
  foreach (${'$'}s in ${'$'}sessions) {
    ${'$'}props = ${'$'}s.TryGetMediaPropertiesAsync().GetAwaiter().GetResult()
    Write-Output ('SMTC-APP: ' + ${'$'}s.SourceAppUserModelId + '  |  ' + ${'$'}props.Title)
  }
} catch {
  Write-Output ('SMTC-ERROR: ' + ${'$'}_.Exception.Message)
}
"""

    // ------------------------------------------------------------------
    // Lettura/verifica AUMID (JNA, shell32)
    // ------------------------------------------------------------------

    /** Lettura di VERIFICA dell'AUMID corrente del processo. NULL se non leggibile. */
    private fun readbackAumid(): String? {
        val shell = Native.load("shell32", Shell32::class.java)
        val outPtr = PointerByReference()
        val outLen = IntByReference()
        val hr = shell.GetCurrentProcessExplicitAppUserModelID(outPtr, outLen)
        if (hr != 0) return null
        val p: Pointer = outPtr.value ?: return null
        return p.getWideString(0)
    }

    private fun isWindowsRuntime(): Boolean =
        System.getProperty("os.name", "").lowercase().contains("windows")

    private interface Shell32 : Library {
        @Suppress("FunctionName")
        fun SetCurrentProcessExplicitAppUserModelID(appId: WString): Int

        @Suppress("FunctionName")
        fun GetCurrentProcessExplicitAppUserModelID(appId: PointerByReference, appIdLength: IntByReference): Int
    }

    private fun setProcessAumid() {
        val shell = Native.load("shell32", Shell32::class.java)
        val hr = shell.SetCurrentProcessExplicitAppUserModelID(WString(AUMID))
        ilog("AUMID '$AUMID' impostato sul processo (HRESULT=$hr)")
    }

    // ------------------------------------------------------------------
    // Registro HKCU: DisplayName + IconUri (via notifiche toast)
    // ------------------------------------------------------------------

    private fun writeRegistryIdentity() {
        val key = "HKCU\\Software\\Classes\\AppUserModelId\\$AUMID"
        val iconUri = currentExecutablePath()

        val commands = buildList {
            add(listOf("reg", "add", key, "/v", "DisplayName", "/t", "REG_SZ", "/d", APP_NAME, "/f"))
            if (iconUri != null)
                add(listOf("reg", "add", key, "/v", "IconUri", "/t", "REG_SZ", "/d", iconUri, "/f"))
        }

        commands.forEach { cmd ->
            val res = runProcess(cmd, timeoutSec = 15)
            val ok = res?.second == 0
            ilog("registro ${if (ok) "OK" else "FALLITO (res=$res)"}: ${cmd.drop(4).joinToString(" ")}")
        }
    }

    /** diagnostica: rilegge il DisplayName scritto nel registro (reg query) */
    private fun queryRegistryDisplayName(): String? {
        val res = runProcess(
            listOf("reg", "query", "HKCU\\Software\\Classes\\AppUserModelId\\$AUMID", "/v", "DisplayName"),
            timeoutSec = 15
        ) ?: return null
        if (res.second != 0) return null
        return res.first.lineSequence()
            .firstOrNull { it.contains("DisplayName", ignoreCase = true) }
            ?.substringAfter("REG_SZ", "")?.trim()?.takeIf { it.isNotBlank() }
    }

    // ------------------------------------------------------------------
    // STEP 7a: scorciatoia Start Menu - via primaria csc, ripiego PowerShell
    // ------------------------------------------------------------------

    private val shortcutThreadStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    /** percorso canonico del .lnk utente: %APPDATA%\...\Programs\RiPlay.lnk */
    private fun startMenuShortcutFile(): File? {
        val appData = System.getenv("APPDATA")
            ?: runCatching { System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming" }.getOrNull()
            ?: return null
        val programs = File(appData, "Microsoft${File.separator}Windows${File.separator}Start Menu${File.separator}Programs")
        if (!programs.isDirectory) return null
        return File(programs, "$APP_NAME.lnk")
    }

    /** eseguibile del processo corrente, se assomiglia al launcher RiPlay
     *  (in dev con gradle il processo e' java.exe: in quel caso skip) */
    private fun currentExecutablePath(): String? =
        runCatching { ProcessHandle.current().info().command().orElse(null) }.getOrNull()
            ?.takeIf { it.endsWith(".exe", ignoreCase = true) && File(it).isFile }
            ?.takeIf { File(it).nameWithoutExtension.contains("riplay", ignoreCase = true) }

    /**
     * TRUE se il .lnk esiste ed e' GIA' quello "nostro": deve contenere,
     * in UTF-16LE dentro il binario, sia il percorso dell'eseguibile sia la
     * stringa dell'AUMID. Una scorciatoia manuale dell'utente NON porta
     * l'AUMID: in quel caso la ricreiamo per aggiungere la proprieta'.
     */
    private fun shortcutAlreadyPointsTo(lnk: File, exe: String): Boolean = runCatching {
        val hay = lnk.readBytes()
        fun contains(text: String): Boolean {
            val needle = text.toByteArray(Charsets.UTF_16LE)
            var i = 0
            while (i <= hay.size - needle.size) {
                var j = 0
                while (j < needle.size && hay[i + j] == needle[j]) j++
                if (j == needle.size) return true
                i++
            }
            return false
        }
        contains(exe) && contains(AUMID)
    }.getOrDefault(false)

    /**
     * Crea (se serve) la scorciatoia "RiPlay.lnk" nel menu Start dell'utente
     * con la proprieta' AppUserModelID. OGNI passo scrive nel diario:
     *  - il comando del processo rilevato (cosi' vediamo se il filtro exe ha
     *    scartato l'avvio: era una delle cause possibili del silenzio);
     *  - il percorso del .lnk e la decisione presa;
     *  - l'esito di OGNI tentativo (csc e PowerShell) con exit code.
     */
    private fun ensureStartMenuShortcut() {
        if (!shortcutThreadStarted.compareAndSet(false, true)) return
        ilog("shortcut: avvio verifica/creazione scorciatoia Start Menu")

        val rawCmd = runCatching { ProcessHandle.current().info().command().orElse("<sconosciuto>") }
            .getOrDefault("<sconosciuto>")
        val exe = currentExecutablePath()
        if (exe == null) {
            ilog("shortcut: SKIP - il comando del processo non e' un RiPlay.exe (ambiente dev?): '$rawCmd'")
            return
        }
        val lnk = startMenuShortcutFile()
        if (lnk == null) {
            ilog("shortcut: SKIP - cartella Start Menu\\Programs non trovata (APPDATA=${System.getenv("APPDATA")})")
            return
        }
        ilog("shortcut: exe='$exe'")
        ilog("shortcut: lnk='${lnk.absolutePath}' (esiste=${lnk.isFile})")

        if (lnk.isFile && shortcutAlreadyPointsTo(lnk, exe)) {
            ilog("shortcut: gia' presente e aggiornata (target+AUMID corretti): nessuna azione")
            return
        }

        if (lnk.isFile) runCatching { lnk.delete() }

        // --- VIA PRIMARIA: helper C# compilato con csc (niente PowerShell) ---
        val helper = runCatching { ensureShortcutHelperExe() }.getOrNull()
        if (helper != null) {
            ilog("shortcut: tentativo via helper csc: '${helper.absolutePath}'")
            val res = runProcess(
                listOf(helper.absolutePath, lnk.absolutePath, exe, AUMID, exe, File(exe).parent ?: ""),
                timeoutSec = 20
            )
            if (res != null && res.second == 0) {
                ilog("shortcut Start Menu CREATA (csc): ${lnk.absolutePath} -> $exe (AUMID=$AUMID)")
                return
            }
            ilog("shortcut: helper csc FALLITO (exit=${res?.second ?: "timeout"}, out=${res?.first?.take(250)?.trim()}) -> ripiego PowerShell")
        } else {
            ilog("shortcut: csc/.NET Framework non disponibile -> ripiego PowerShell")
        }

        // --- RIPIEGO: PowerShell Add-Type (passo 6) con TIMEOUT ---
        val script = File(System.getProperty("java.io.tmpdir"), "riplay_identity_shortcut.ps1")
        runCatching {
            script.writeText(POWERSHELL_SHORTCUT_SCRIPT, Charsets.ISO_8859_1) // contenuto solo ASCII
        }.onFailure {
            ilog("shortcut: impossibile scrivere lo script powershell: ${it.message}")
            return
        }

        val cmd = listOf(
            "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
            "-WindowStyle", "Hidden", "-File", script.absolutePath,
            "-LnkPath", lnk.absolutePath,
            "-Target", exe,
            "-Aumid", AUMID,
            "-Icon", exe,
            "-WorkDir", File(exe).parent ?: ""
        )
        val res = runProcess(cmd, timeoutSec = 25)
        val ok = res != null && res.second == 0
        if (!ok)
            ilog("shortcut: powershell FALLITO (exit=${res?.second ?: "timeout"}, out=${res?.first?.take(250)?.trim()})")

        ilog(if (ok) "shortcut Start Menu CREATA (powershell): ${lnk.absolutePath} -> $exe (AUMID=$AUMID)"
             else "shortcut Start Menu FALLITA dopo entrambi i tentativi (csc + powershell)")
    }

    // ---- helper compilato con csc -------------------------------------

    /** csc di .NET Framework 4: presente su ogni Windows 10/11. */
    private fun cscCandidates(): List<File> = listOf(
        File("C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\csc.exe"),
        File("C:\\Windows\\Microsoft.NET\\Framework\\v4.0.30319\\csc.exe")
    )

    /**
     * Compila (una volta, poi cache in temp) un piccolissimo eseguibile C#
     * che crea il .lnk con la proprieta' AUMID via IShellLinkW+IPropertyStore.
     * Non dipende da PowerShell: nessuno script da analizzare, nessun AMSI,
     * exit code deterministico. Restituisce NULL se csc o la compilazione
     * falliscono (il diario dice perche').
     */
    private fun ensureShortcutHelperExe(): File? {
        val csc = cscCandidates().firstOrNull { it.isFile }
        if (csc == null) {
            ilog("shortcut: nessun csc trovato in ${cscCandidates().joinToString(" | ") { it.absolutePath }}")
            return null
        }
        val tmp = System.getProperty("java.io.tmpdir")
        val helper = File(tmp, "riplay_shortcut_helper.exe")
        if (helper.isFile) {
            ilog("shortcut: riutilizzo helper gia' compilato: '${helper.absolutePath}'")
            return helper
        }
        val src = File(tmp, "riplay_shortcut_helper.cs")
        src.writeText(CSHARP_SHORTCUT_HELPER, Charsets.ISO_8859_1)
        val res = runProcess(
            listOf(csc.absolutePath, "/nologo", "/target:exe", "/optimize+",
                "/out:${helper.absolutePath}", src.absolutePath),
            timeoutSec = 60
        )
        if (res == null || res.second != 0) {
            ilog("shortcut: compilazione csc FALLITA (exit=${res?.second ?: "timeout"}, out=${res?.first?.take(300)?.trim()})")
            return null
        }
        ilog("shortcut: helper compilato: '${helper.absolutePath}'")
        return helper.takeIf { it.isFile }
    }

    /**
     * Sorgente C# dell'helper (C# 5, solo ASCII, nessuna dipendenza oltre
     * mscorlib: csc lo compila senza riferimenti extra). Stessa tecnica COM
     * dello script PowerShell (IShellLinkW + IPropertyStore + IPersistFile),
     * tecnica documentata Microsoft, identica a quella degli installer.
     * Exit code: 0 OK, 1 eccezione, 2 argomenti.
     */
    private val CSHARP_SHORTCUT_HELPER = """
using System;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.ComTypes;

namespace RiPlayTools
{
  internal static class ShortcutTool
  {
    [STAThread]
    static int Main(string[] args)
    {
      if (args.Length < 4) return 2;
      string lnkPath = args[0];
      string target = args[1];
      string aumid = args[2];
      string icon = args[3];
      string workDir = args.Length > 4 ? args[4] : "";
      try
      {
        IShellLinkW link = (IShellLinkW)(object)new CShellLink();
        link.SetPath(target);
        if (!string.IsNullOrEmpty(workDir)) link.SetWorkingDirectory(workDir);
        if (!string.IsNullOrEmpty(icon)) link.SetIconLocation(icon, 0);
        link.SetDescription("RiPlay");

        IPropertyStore store = (IPropertyStore)link;
        PropertyKey pkeyAppUserModelId = new PropertyKey(new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3"), 5);
        PropVariant pv = new PropVariant();
        pv.vt = 31; /* VT_LPWSTR */
        pv.pointer = Marshal.StringToCoTaskMemUni(aumid);
        store.SetValue(ref pkeyAppUserModelId, ref pv);
        Marshal.FreeCoTaskMem(pv.pointer);
        store.Commit();

        IPersistFile persist = (IPersistFile)link;
        persist.Save(lnkPath, true);
        return 0;
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine(ex.ToString());
        return 1;
      }
    }
  }

  [ComImport, Guid("00021401-0000-0000-C000-000000000046")]
  internal class CShellLink { }

  [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown), Guid("000214F9-0000-0000-C000-000000000046")]
  internal interface IShellLinkW
  {
    void GetPath([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszFile, int cch, IntPtr pfd, uint fFlags);
    void GetIDList(out IntPtr ppidl);
    void SetIDList(IntPtr pidl);
    void GetDescription([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszName, int cch);
    void SetDescription([MarshalAs(UnmanagedType.LPWStr)] string pszName);
    void GetWorkingDirectory([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszDir, int cch);
    void SetWorkingDirectory([MarshalAs(UnmanagedType.LPWStr)] string pszDir);
    void GetArguments([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszArgs, int cch);
    void SetArguments([MarshalAs(UnmanagedType.LPWStr)] string pszArgs);
    void GetHotkey(out short pwHotkey);
    void SetHotkey(short wHotkey);
    void GetShowCmd(out int piShowCmd);
    void SetShowCmd(int iShowCmd);
    void GetIconLocation([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszIconPath, int cch, out int piIcon);
    void SetIconLocation([MarshalAs(UnmanagedType.LPWStr)] string pszIconPath, int iIcon);
    void SetRelativePath([MarshalAs(UnmanagedType.LPWStr)] string pszPathRel, uint dwReserved);
    void Resolve(IntPtr hwnd, uint fFlags);
    void SetPath([MarshalAs(UnmanagedType.LPWStr)] string pszFile);
  }

  [StructLayout(LayoutKind.Sequential)]
  internal struct PropertyKey
  {
    public Guid fmtid;
    public int pid;
    public PropertyKey(Guid f, int p) { fmtid = f; pid = p; }
  }

  [StructLayout(LayoutKind.Explicit)]
  internal struct PropVariant
  {
    [FieldOffset(0)] public short vt;
    [FieldOffset(8)] public IntPtr pointer;
  }

  [ComImport, Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
  internal interface IPropertyStore
  {
    void GetCount(out uint count);
    void GetAt(uint index, out PropertyKey key);
    void GetValue(ref PropertyKey key, out PropVariant value);
    void SetValue(ref PropertyKey key, ref PropVariant value);
    void Commit();
  }
}
"""

    /**
     * Script PowerShell di ripiego (quello del passo 6, invariato di
     * contenuto): qui con timeout e log passo-passo nel chiamante.
     * ATTENZIONE al formato: l'apertura dell'here-string @' deve chiudere la
     * riga, la chiusura '@ deve stare a inizio riga; il contenuto e' C# 5
     * (compatibile col compilatore di PowerShell 5.1) e solo ASCII.
     */
    private val POWERSHELL_SHORTCUT_SCRIPT = """
param(
  [Parameter(Mandatory=${'$'}true)][string]${'$'}LnkPath,
  [Parameter(Mandatory=${'$'}true)][string]${'$'}Target,
  [Parameter(Mandatory=${'$'}true)][string]${'$'}Aumid,
  [string]${'$'}Icon = '',
  [string]${'$'}WorkDir = ''
)
${'$'}ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.ComTypes;

namespace RiPlayTools
{
  [ComImport, Guid("00021401-0000-0000-C000-000000000046")]
  internal class CShellLink { }

  [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown), Guid("000214F9-0000-0000-C000-000000000046")]
  internal interface IShellLinkW
  {
    void GetPath([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszFile, int cch, IntPtr pfd, uint fFlags);
    void GetIDList(out IntPtr ppidl);
    void SetIDList(IntPtr pidl);
    void GetDescription([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszName, int cch);
    void SetDescription([MarshalAs(UnmanagedType.LPWStr)] string pszName);
    void GetWorkingDirectory([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszDir, int cch);
    void SetWorkingDirectory([MarshalAs(UnmanagedType.LPWStr)] string pszDir);
    void GetArguments([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszArgs, int cch);
    void SetArguments([MarshalAs(UnmanagedType.LPWStr)] string pszArgs);
    void GetHotkey(out short pwHotkey);
    void SetHotkey(short wHotkey);
    void GetShowCmd(out int piShowCmd);
    void SetShowCmd(int iShowCmd);
    void GetIconLocation([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszIconPath, int cch, out int piIcon);
    void SetIconLocation([MarshalAs(UnmanagedType.LPWStr)] string pszIconPath, int iIcon);
    void SetRelativePath([MarshalAs(UnmanagedType.LPWStr)] string pszPathRel, uint dwReserved);
    void Resolve(IntPtr hwnd, uint fFlags);
    void SetPath([MarshalAs(UnmanagedType.LPWStr)] string pszFile);
  }

  [StructLayout(LayoutKind.Sequential)]
  internal struct PropertyKey
  {
    public Guid fmtid;
    public int pid;
    public PropertyKey(Guid f, int p) { fmtid = f; pid = p; }
  }

  [StructLayout(LayoutKind.Explicit)]
  internal struct PropVariant
  {
    [FieldOffset(0)] public short vt;
    [FieldOffset(8)] public IntPtr pointer;
  }

  [ComImport, Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
  internal interface IPropertyStore
  {
    void GetCount(out uint count);
    void GetAt(uint index, out PropertyKey key);
    void GetValue(ref PropertyKey key, out PropVariant value);
    void SetValue(ref PropertyKey key, ref PropVariant value);
    void Commit();
  }

  internal static class ShortcutTool
  {
    public static void Create(string lnkPath, string target, string aumid, string icon, string workDir)
    {
      IShellLinkW link = (IShellLinkW)(object)new CShellLink();
      link.SetPath(target);
      if (!string.IsNullOrEmpty(workDir)) link.SetWorkingDirectory(workDir);
      if (!string.IsNullOrEmpty(icon)) link.SetIconLocation(icon, 0);
      link.SetDescription("RiPlay");

      IPropertyStore store = (IPropertyStore)link;
      PropertyKey pkeyAppUserModelId = new PropertyKey(new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3"), 5);
      PropVariant pv = new PropVariant();
      pv.vt = 31; /* VT_LPWSTR */
      pv.pointer = Marshal.StringToCoTaskMemUni(aumid);
      store.SetValue(ref pkeyAppUserModelId, ref pv);
      Marshal.FreeCoTaskMem(pv.pointer);
      store.Commit();

      IPersistFile persist = (IPersistFile)link;
      persist.Save(lnkPath, true);
    }
  }
}
'@
[RiPlayTools.ShortcutTool]::Create(${'$'}LnkPath, ${'$'}Target, ${'$'}Aumid, ${'$'}Icon, ${'$'}WorkDir)
Write-Output 'SHORTCUT-OK'
""".trimIndent()
}
