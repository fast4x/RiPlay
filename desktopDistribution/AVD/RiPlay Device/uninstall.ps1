# ================================================================
#  RiPlay Uninstaller — Windows (PowerShell)
#  Rimuove: AVD, Android SDK, Java Adoptium (opzionale), shortcut
#
#  Uso: PowerShell come Amministratore
#       Set-ExecutionPolicy Bypass -Scope Process -Force
#       .\uninstall.ps1
# ================================================================

#Requires -Version 5.1

$APP_NAME        = "RiPlay"
$AVD_NAME        = "RiPlayDevice"
$ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\sdk"
$LAUNCH_DIR      = "$env:LOCALAPPDATA\RiPlay"

function Info    { param($m) Write-Host "  [..] $m" -ForegroundColor Cyan }
function Success { param($m) Write-Host "  [OK] $m" -ForegroundColor Green }
function Warn    { param($m) Write-Host "  [!!] $m" -ForegroundColor Yellow }

function Ask {
  param($Prompt)
  $r = Read-Host "  $Prompt [y/N]"
  return ($r -match '^[yY]$')
}

function Get-FolderSizeGB {
  param($Path)
  if (-not (Test-Path $Path)) { return 0 }
  $bytes = (Get-ChildItem $Path -Recurse -ErrorAction SilentlyContinue |
            Measure-Object -Property Length -Sum).Sum
  return [Math]::Round($bytes / 1GB, 1)
}

# ── Banner ──────────────────────────────────────────────────────
Clear-Host
Write-Host ""
Write-Host "  +==========================================+" -ForegroundColor Red
Write-Host "  |   RiPlay Uninstaller  ·  Windows          |" -ForegroundColor Red
Write-Host "  +==========================================+" -ForegroundColor Red
Write-Host ""
Warn "Questa operazione rimuoverà RiPlay e l'ambiente Android dal tuo sistema."
Write-Host ""

if (-not (Ask "Vuoi procedere con la disinstallazione?")) {
  Write-Host "  Annullato." -ForegroundColor Gray
  Read-Host "  Premi Invio per uscire"
  exit 0
}
Write-Host ""

# ── 1. Termina emulatore ─────────────────────────────────────────
Info "Chiusura emulatore Android (se attivo)..."
$ADB = "$ANDROID_SDK_ROOT\platform-tools\adb.exe"
if (Test-Path $ADB) {
  try { & $ADB emu kill 2>$null } catch {}
  Start-Sleep -Seconds 2
}
# Forza chiusura processi residui
Get-Process | Where-Object { $_.Name -match "emulator|qemu" } |
  Stop-Process -Force -ErrorAction SilentlyContinue
Success "Emulatore terminato"

# ── 2. Elimina AVD ───────────────────────────────────────────────
Info "Rimozione AVD '$AVD_NAME'..."
$AVDMANAGER = "$ANDROID_SDK_ROOT\cmdline-tools\latest\bin\avdmanager.bat"
if (Test-Path $AVDMANAGER) {
  try { & $AVDMANAGER delete avd --name $AVD_NAME 2>$null } catch {}
}
# Cartelle AVD residue
$avdBase = "$env:USERPROFILE\.android\avd"
Remove-Item "$avdBase\$AVD_NAME.avd" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$avdBase\$AVD_NAME.ini"          -Force -ErrorAction SilentlyContinue
Success "AVD rimosso"

# ── 3. Android SDK ───────────────────────────────────────────────
if (Test-Path $ANDROID_SDK_ROOT) {
  $sdkGB = Get-FolderSizeGB $ANDROID_SDK_ROOT
  if (Ask "Rimuovere Android SDK ($sdkGB GB in $ANDROID_SDK_ROOT)?") {
    Remove-Item $ANDROID_SDK_ROOT -Recurse -Force -ErrorAction SilentlyContinue
    Success "Android SDK rimosso ($sdkGB GB liberati)"
  } else {
    Warn "Android SDK mantenuto"
  }
} else {
  Info "Android SDK non trovato"
}

# ── 4. Cartella .android ─────────────────────────────────────────
$dotAndroid = "$env:USERPROFILE\.android"
if (Test-Path $dotAndroid) {
  $sz = Get-FolderSizeGB $dotAndroid
  if (Ask "Rimuovere la cartella .android (cache AVD, $sz GB)?") {
    Remove-Item $dotAndroid -Recurse -Force -ErrorAction SilentlyContinue
    Success ".android rimosso"
  }
}

# ── 5. Java Adoptium (opzionale) ─────────────────────────────────
$javaInstalls = Get-WmiObject -Class Win32_Product 2>$null |
  Where-Object { $_.Name -match "Temurin|Adoptium|OpenJDK" } |
  Select-Object -First 1

if ($javaInstalls) {
  Warn "Java trovato: $($javaInstalls.Name)"
  if (Ask "Rimuovere Java? (potrebbe essere usato da altri programmi)") {
    Start-Process msiexec -ArgumentList "/x `"$($javaInstalls.IdentifyingNumber)`" /qn" -Wait
    Success "Java rimosso"
  } else {
    Warn "Java mantenuto"
  }
} else {
  Info "Nessuna installazione Java Adoptium trovata"
}

# ── 6. Shortcut Desktop e script di lancio ───────────────────────
Info "Rimozione shortcut e script di lancio..."
Remove-Item "$env:PUBLIC\Desktop\$APP_NAME.lnk"          -Force -ErrorAction SilentlyContinue
Remove-Item "$env:PUBLIC\Desktop\$AVD_NAME.lnk"          -Force -ErrorAction SilentlyContinue
Remove-Item "$env:USERPROFILE\Desktop\$APP_NAME.lnk"     -Force -ErrorAction SilentlyContinue
Remove-Item "$env:USERPROFILE\Desktop\$AVD_NAME.lnk"     -Force -ErrorAction SilentlyContinue
Remove-Item $LAUNCH_DIR -Recurse -Force                   -ErrorAction SilentlyContinue
Success "Shortcut e script di lancio rimossi"

# ── 7. Log installer ─────────────────────────────────────────────
Remove-Item "$env:TEMP\RiPlay-install.log" -Force -ErrorAction SilentlyContinue

# ── Fine ─────────────────────────────────────────────────────────
Write-Host ""
Write-Host "  +==========================================+" -ForegroundColor Green
Write-Host "  |   Disinstallazione completata!           |" -ForegroundColor Green
Write-Host "  +==========================================+" -ForegroundColor Green
Write-Host ""
Write-Host "  Tutto ciò che era stato installato da RiPlay Installer è stato rimosso." -ForegroundColor White
Write-Host ""
Read-Host "  Premi Invio per chiudere"
