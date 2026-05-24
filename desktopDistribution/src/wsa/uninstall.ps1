# ================================================================
#  RiPlay Uninstaller - Windows (WSABuilds)
#  Rimuove WSA, cartella installazione e shortcut.
#
#  Uso: doppio clic su "Disinstalla RiPlay.bat"
# ================================================================

#Requires -Version 5.1

$WSA_INSTALL_DIR = "C:\WSA-RiPlay"
$SCRIPT_DIR      = Split-Path -Parent $MyInvocation.MyCommand.Path
$LOG_FILE        = "$env:TEMP\riplay-wsa-uninstall.log"

function Info    { param($m) Write-Host "  [..] $m" -ForegroundColor Cyan;   Add-Content $LOG_FILE "[INFO]  $m" }
function Success { param($m) Write-Host "  [OK] $m" -ForegroundColor Green;  Add-Content $LOG_FILE "[OK]    $m" }
function Warn    { param($m) Write-Host "  [!!] $m" -ForegroundColor Yellow; Add-Content $LOG_FILE "[WARN]  $m" }
function Ask     { param($p) $r = Read-Host "  $p [y/N]"; return ($r -match "^[yY]$") }

Clear-Host
Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Red
Write-Host "  |       RiPlay Uninstaller (WSA)           |" -ForegroundColor Red
Write-Host "  +------------------------------------------+" -ForegroundColor Red
Write-Host ""

if (-not (Ask "Vuoi procedere con la disinstallazione?")) {
  Write-Host "  Annullato." -ForegroundColor Gray
  Read-Host "  Premi Invio per uscire"
  exit 0
}

"" | Out-File $LOG_FILE

Info "Chiusura Windows Subsystem for Android ..."
Get-Process | Where-Object { $_.Name -match "Wsa|WsaClient|vmmem" } |
  Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3
Success "Processi WSA terminati"

Info "Rimozione pacchetto WSA ..."
$pkg = Get-AppxPackage -Name "MicrosoftCorporationII.WindowsSubsystemForAndroid" 2>$null
if ($pkg) {
  Remove-AppxPackage -Package $pkg.PackageFullName 2>&1 | Out-Null
  Success "Pacchetto WSA rimosso"
} else {
  Warn "Pacchetto WSA non trovato nel registro Appx"
}

if (Test-Path $WSA_INSTALL_DIR) {
  $szGB = [Math]::Round((Get-ChildItem $WSA_INSTALL_DIR -Recurse -ErrorAction SilentlyContinue |
    Measure-Object -Property Length -Sum).Sum / 1GB, 1)
  if (Ask "Rimuovere la cartella WSA ($szGB GB in $WSA_INSTALL_DIR)?") {
    Remove-Item $WSA_INSTALL_DIR -Recurse -Force -ErrorAction SilentlyContinue
    Success "Cartella WSA rimossa ($szGB GB liberati)"
  }
}

$wsaData = "$env:LOCALAPPDATA\Packages\MicrosoftCorporationII.WindowsSubsystemForAndroid_8wekyb3d8bbwe"
if (Test-Path $wsaData) {
  if (Ask "Rimuovere dati utente WSA (app installate, impostazioni)?") {
    Remove-Item $wsaData -Recurse -Force -ErrorAction SilentlyContinue
    Success "Dati utente WSA rimossi"
  }
}

Info "Rimozione shortcut ..."
Remove-Item "$env:PUBLIC\Desktop\RiPlay.lnk"           -Force -ErrorAction SilentlyContinue
Remove-Item "$env:USERPROFILE\Desktop\RiPlay.lnk"      -Force -ErrorAction SilentlyContinue
Remove-Item "$env:LOCALAPPDATA\RiPlay" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$SCRIPT_DIR\wsa_config.txt" -Force        -ErrorAction SilentlyContinue
Success "Shortcut rimossi"

Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host "  |      Disinstallazione completata!        |" -ForegroundColor Green
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host ""
Read-Host "  Premi Invio per chiudere"
