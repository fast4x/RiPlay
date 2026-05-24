# ================================================================
#  RiPlay - Installa APK su WSA
#  Scarica RiPlay.apk da GitHub e la installa in WSA via ADB.
#
#  IMPORTANTE: avvia SOLO dopo che WSA e' visibile nella taskbar.
#
#  Uso: doppio clic su "Installa APK.bat"
# ================================================================

#Requires -Version 5.1

# -- Configurazione ----------------------------------------------
$APP_NAME       = "RiPlay"
$APK_NAME       = "RiPlay.apk"
$PACKAGE_NAME   = "it.fast4x.riplay"
$MAIN_ACTIVITY  = "it.fast4x.riplay.MainActivity"
$APK_GITHUB_URL = "https://github.com/fast4x/RiPlay/releases/download/v0.7.80/RiPlay-full-release-0.7.80.apk"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$LOG_FILE   = "$env:TEMP\riplay-apk.log"

$WSA_INSTALL_DIR = "C:\WSA-RiPlay"
$ADB = "$SCRIPT_DIR\adb.exe" 


# -- Helpers -----------------------------------------------------
function Info    { param($m) Write-Host "  [..] $m" -ForegroundColor Cyan;   Add-Content $LOG_FILE "[INFO]  $m" }
function Success { param($m) Write-Host "  [OK] $m" -ForegroundColor Green;  Add-Content $LOG_FILE "[OK]    $m" }
function Warn    { param($m) Write-Host "  [!!] $m" -ForegroundColor Yellow; Add-Content $LOG_FILE "[WARN]  $m" }
function Err     { param($m) Write-Host "  [XX] ERRORE: $m" -ForegroundColor Red; Add-Content $LOG_FILE "[ERR]   $m"; Read-Host "Premi Invio per uscire"; exit 1 }

# -- Banner ------------------------------------------------------
Clear-Host
Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Cyan
Write-Host "  |     RiPlay - Installazione APK su WSA   |" -ForegroundColor Cyan
Write-Host "  +------------------------------------------+" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Log: $LOG_FILE" -ForegroundColor DarkGray
Write-Host ""

"" | Out-File $LOG_FILE
Add-Content $LOG_FILE "=== RiPlay APK Installer (WSA) $(Get-Date) ==="

# -- Verifica ADB ------------------------------------------------
if (-not (Test-Path $ADB)) { Err "ADB non trovato. Esegui prima Installa RiPlay.bat" }
Success "ADB trovato: $ADB"

# -- Connessione ADB a WSA (porta 58526) -------------------------
Info "Connessione ADB a WSA (porta 58526) ..."
& $ADB connect 127.0.0.1:58526 2>&1 | Out-Null
Start-Sleep -Seconds 2

$devices = & $ADB devices 2>$null | Out-String
if ($devices -notmatch "127.0.0.1:58526") {
  Err "WSA non raggiungibile via ADB.`nVerifica che WSA sia avviato e che il Debug USB sia attivo in WSA Settings > Avanzate."
}
Success "WSA connesso via ADB (127.0.0.1:58526)"

# -- Verifica che Android sia pronto (3 controlli) ---------------
Info "Verifica che Android sia pronto ..."

Write-Host "  [..] Controllo kernel ..." -ForegroundColor Cyan -NoNewline
try { $val = & $ADB -s 127.0.0.1:58526 shell getprop sys.boot_completed 2>$null } catch { $val = "" }
if ($val -notmatch "1") { Write-Host ""; Err "WSA non ancora pronto. Attendi e riprova." }
Write-Host " OK" -ForegroundColor Green

Write-Host "  [..] Controllo animazione boot ..." -ForegroundColor Cyan -NoNewline
try { $val = & $ADB -s 127.0.0.1:58526 shell getprop init.svc.bootanim 2>$null } catch { $val = "" }
if ($val -notmatch "stopped") { Write-Host ""; Err "Animazione boot in corso. Attendi e riprova." }
Write-Host " OK" -ForegroundColor Green

Write-Host "  [..] Controllo package manager ..." -ForegroundColor Cyan -NoNewline
try { $val = & $ADB -s 127.0.0.1:58526 shell pm path android 2>$null } catch { $val = "" }
if ($val -notmatch "package:") { Write-Host ""; Err "Package manager non pronto. Attendi e riprova." }
Write-Host " OK" -ForegroundColor Green
Success "Android completamente pronto"

# -- Download APK ------------------------------------------------
$APK_PATH = "$env:TEMP\$APK_NAME"
Info "Download RiPlay.apk da GitHub ..."
try {
  $wc = New-Object System.Net.WebClient
  $wc.DownloadFile($APK_GITHUB_URL, $APK_PATH)
} catch {
  Err "Download APK fallito: $_"
}
if (-not (Test-Path $APK_PATH)) { Err "APK non scaricata correttamente." }
Success "RiPlay.apk scaricata"

# -- Installazione APK -------------------------------------------
Info "Installazione RiPlay.apk in WSA ..."
$adbOut = & $ADB -s 127.0.0.1:58526 install -r $APK_PATH 2>&1 | Out-String
Add-Content $LOG_FILE $adbOut
if ($adbOut -match "Success") {
  Success "RiPlay installata in WSA"
} else {
  Err "Installazione fallita. Dettagli: $adbOut"
}

# -- Avvio app ---------------------------------------------------
Info "Avvio RiPlay ..."
& $ADB -s 127.0.0.1:58526 shell am start -n "${PACKAGE_NAME}/${MAIN_ACTIVITY}" 2>&1 | Out-Null
Success "RiPlay avviata"


# -- Fine --------------------------------------------------------
Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host "  |      RiPlay installata con successo!     |" -ForegroundColor Green
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host ""
Write-Host ""
Read-Host "  Premi Invio per chiudere"
