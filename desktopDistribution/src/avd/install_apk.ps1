# ================================================================
#  RiPlay - Installa APK + Shortcut (Icona da EXE)
#  Cerca un file .exe (es. launcher.exe) nella cartella dello script
#  per usarlo come fonte dell'icona del collegamento.
# ================================================================

#Requires -Version 5.1

# -- Configurazione ----------------------------------------------
 $APP_NAME       = "RiPlay"
 $APK_NAME       = "RiPlay.apk"
 $APK_GITHUB_URL = "https://github.com/fast4x/RiPlay/releases/download/v0.7.80/RiPlay-full-release-0.7.80.apk"

# Dati per l'avvio diretto
 $APP_PACKAGE    = "it.fast4x.riplay"
 $APP_ACTIVITY   = "it.fast4x.riplay.MainActivity"

 $ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\sdk"
 $ADB              = "$ANDROID_SDK_ROOT\platform-tools\adb.exe"
 $LOG_FILE         = "$env:TEMP\riplay-apk.log"
 $DESKTOP_PATH     = "$env:PUBLIC\Desktop"
 $DEVICE_SERIAL    = "" 

# -- Configurazione Icona (Priorità al file EXE) ------------------
 $SCRIPT_DIR       = $PSScriptRoot
# Modifica qui se il tuo file ha un nome diverso da "launcher.exe"
 $ICON_EXE_PATH    = "$SCRIPT_DIR\launcher.exe" 

# -- Helpers -----------------------------------------------------
function Info    { param($m) Write-Host "  [..] $m" -ForegroundColor Cyan;   Add-Content $LOG_FILE "[INFO]  $m" }
function Success { param($m) Write-Host "  [OK] $m" -ForegroundColor Green;  Add-Content $LOG_FILE "[OK]    $m" }
function Warn    { param($m) Write-Host "  [!!] $m" -ForegroundColor Yellow; Add-Content $LOG_FILE "[WARN]  $m" }
function Err     { param($m) Write-Host "  [XX] ERRORE: $m" -ForegroundColor Red; Add-Content $LOG_FILE "[ERR]   $m"; Read-Host "Premi Invio per uscire"; exit 1 }

# -- Funzione Helper per ADB ------------------------------------
function Invoke-AdbCommand {
    param(
        [Parameter(ValueFromRemainingArguments=$true)]
        [string[]]$Arguments
    )
    if (-not $DEVICE_SERIAL) { throw "DEVICE_SERIAL non impostato" }
    $cmdArgs = @("-s", $DEVICE_SERIAL)
    $cmdArgs += $Arguments
    & $ADB @cmdArgs 2>&1
}

# -- Banner ------------------------------------------------------
Clear-Host
Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Cyan
Write-Host "  |  RiPlay - Installazione + Icona EXE     |" -ForegroundColor Cyan
Write-Host "  +------------------------------------------+" -ForegroundColor Cyan
Write-Host ""

"" | Out-File $LOG_FILE
Add-Content $LOG_FILE "=== RiPlay APK Installer $(Get-Date) ==="

# -- Verifica Icona EXE -------------------------------------------
 $useCustomIcon = $false
 $finalIconPath = ""

if (Test-Path $ICON_EXE_PATH) {
    $useCustomIcon = $true
    $finalIconPath = "$ICON_EXE_PATH, 0" # ", 0" dice a Windows di prendere la prima icona risorsa
    Success "Icona EXE trovata: launcher.exe"
} else {
    Warn "File 'launcher.exe' NON trovato nella cartella dello script."
    Write-Host "     Verrà usata l'icona predefinita di ADB." -ForegroundColor DarkGray
    $finalIconPath = "$ADB, 0"
}

# -- Verifica ADB -----------------------------------------------
if (-not (Test-Path $ADB)) { Err "ADB non trovato in $ADB." }
Success "ADB trovato"

# -- Rilevamento Dispositivo -------------------------------------
Info "Ricerca dispositivo target ..."
try {
    $rawDevices = & $ADB devices 2>$null
    $emulatorSerials = @()
    foreach ($line in $rawDevices -split "`r`n") {
        if ($line -match "^(emulator-\d+).*device$") { $emulatorSerials += $matches[1] }
    }
    if ($emulatorSerials.Count -eq 0) { Err "Nessun emulatore trovato." }
    $DEVICE_SERIAL = $emulatorSerials[0]
    Success "Emulatore: $DEVICE_SERIAL"
} catch {
    Err "Errore rilevamento: $_"
}

# -- Verifica Boot ------------------------------------------------
Info "Verifica stato Android ..."
 $val = Invoke-AdbCommand shell getprop sys.boot_completed
if ($val -notmatch "1") { Err "Android non pronto." }
 $val = Invoke-AdbCommand shell getprop init.svc.bootanim
if ($val -notmatch "stopped") { Err "Boot animation non finita." }
Success "Android pronto"

# -- Download APK -------------------------------------------------
Info "Download APK ..."
 $TEMP_APK_PATH = "$env:TEMP\$APK_NAME"
try {
  $wc = New-Object System.Net.WebClient
  $wc.DownloadFile($APK_GITHUB_URL, $TEMP_APK_PATH)
} catch { Err "Download fallito: $_" }
if (-not (Test-Path $TEMP_APK_PATH)) { Err "Download fallito (file non trovato)." }
Success "Download completato"

# -- Installazione APK --------------------------------------------
Info "Installazione APK..."
 $adbOut = Invoke-AdbCommand install -r $TEMP_APK_PATH | Out-String
Add-Content $LOG_FILE $adbOut
if ($adbOut -match "Success") {
  Success "RiPlay installata"
} else {
  Err "Installazione fallita: $adbOut"
}

# -- Avvio App ----------------------------------------------------
Info "Avvio app ..."
 $pkg = (Invoke-AdbCommand shell pm list packages | Select-String "riplay").ToString().Replace("package:","").Trim()
Invoke-AdbCommand shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null
Success "RiPlay avviata"

# -- Creazione Shortcut Desktop -----------------------------------
Info "Creazione collegamento..."
try {
    $WshShell = New-Object -ComObject WScript.Shell
    $ShortcutPath = "$DESKTOP_PATH\RiPlay.lnk"
    $Shortcut = $WshShell.CreateShortcut($ShortcutPath)
    
    # IL TARGET RIMANE ADB (l'EXE serve solo per l'immagine)
    $Shortcut.TargetPath = $ADB
    $Shortcut.Arguments = "-s $DEVICE_SERIAL shell am start -n $APP_PACKAGE/$APP_ACTIVITY"
    $Shortcut.Description = "Avvia RiPlay AVD"
    $Shortcut.WorkingDirectory = "$ANDROID_SDK_ROOT\platform-tools"
    
    # Imposta l'icona dall'EXE fornito o da default
    $Shortcut.IconLocation = $finalIconPath
    
    $Shortcut.Save()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($WshShell) | Out-Null
    
    Start-Sleep -Milliseconds 500
    if (Test-Path $ShortcutPath) {
        Success "Collegamento creato con successo."
    } else {
        throw "File .lnk non trovato."
    }

} catch {
    Err "Errore shortcut: $_"
}

# -- Fine --------------------------------------------------------
Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host "  |      Installazione Completata!          |" -ForegroundColor Green
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host ""
Read-Host "  Premi Invio per chiudere"