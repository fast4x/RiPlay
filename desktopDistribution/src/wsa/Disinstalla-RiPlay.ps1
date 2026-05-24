#Requires -Version 5.1
$ErrorActionPreference = 'Continue'
$WSA_INSTALL_DIR = 'C:\WSA-RiPlay'
$SCRIPT_DIR = Split-Path -Parent $PSCommandPath
$LOG_FILE = Join-Path $SCRIPT_DIR 'Uninstall-RiPlay.log'

function LogLine {
    param([string]$Level, [string]$Message, [ConsoleColor]$Color = [ConsoleColor]::White)
    $line = '[{0}] [{1}] {2}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Level, $Message
    try { Add-Content -LiteralPath $LOG_FILE -Value $line -Encoding UTF8 } catch {}
    Write-Host $line -ForegroundColor $Color
}
function Info { param([string]$m) LogLine 'INFO' $m Cyan }
function Ok { param([string]$m) LogLine 'OK' $m Green }
function Warn { param([string]$m) LogLine 'WARN' $m Yellow }
function AskYesNo { param([string]$Text) $r = Read-Host "$Text [s/N]"; return ($r -match '^[sSyY]$') }
function Test-IsAdmin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

Clear-Host
try { Remove-Item -LiteralPath $LOG_FILE -Force -ErrorAction SilentlyContinue } catch {}
Info 'Avvio disinstallazione RiPlay / WSA.'

if (-not (Test-IsAdmin)) {
    Warn 'Lo script non e avviato come amministratore. Usa Uninstall-RiPlay.cmd.'
    exit 1
}

Write-Host ''
Write-Host 'Questo rimuovera WSA-RiPlay, RiPlay installato dentro WSA, cartella C:\WSA-RiPlay e collegamenti Desktop.' -ForegroundColor Yellow
Write-Host ''
if (-not (AskYesNo 'Vuoi procedere')) {
    Warn 'Disinstallazione annullata dall utente.'
    exit 0
}

Info 'Chiudo processi WSA...'
Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'Wsa|WsaClient|WsaService|vmmem' } | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Ok 'Processi WSA chiusi o non presenti.'

Info 'Rimuovo pacchetto Windows Subsystem for Android se presente...'
$pkgs = Get-AppxPackage -Name '*WindowsSubsystemForAndroid*' -ErrorAction SilentlyContinue
if ($pkgs) {
    foreach ($pkg in $pkgs) {
        try {
            Info "Rimuovo Appx: $($pkg.PackageFullName)"
            Remove-AppxPackage -Package $pkg.PackageFullName -ErrorAction Continue
            Ok 'Pacchetto Appx rimosso.'
        } catch {
            Warn "Errore rimozione Appx: $($_.Exception.Message)"
        }
    }
} else {
    Warn 'Pacchetto WSA non trovato tra le app installate.'
}

Info 'Rimuovo collegamenti Desktop...'
$desktopPaths = @(
    (Join-Path ([Environment]::GetFolderPath('Desktop')) 'RiPlay.lnk'),
    (Join-Path ([Environment]::GetFolderPath('Desktop')) 'RiPlay WSA.lnk'),
    (Join-Path $env:PUBLIC 'Desktop\RiPlay.lnk'),
    (Join-Path $env:PUBLIC 'Desktop\RiPlay WSA.lnk')
)
foreach ($p in $desktopPaths) {
    if (Test-Path -LiteralPath $p) {
        Remove-Item -LiteralPath $p -Force -ErrorAction SilentlyContinue
        Ok "Rimosso: $p"
    }
}

if (Test-Path -LiteralPath $WSA_INSTALL_DIR) {
    Info "Rimuovo cartella: $WSA_INSTALL_DIR"
    try {
        Remove-Item -LiteralPath $WSA_INSTALL_DIR -Recurse -Force -ErrorAction Stop
        Ok 'Cartella C:\WSA-RiPlay rimossa.'
    } catch {
        Warn "Errore rimozione cartella C:\WSA-RiPlay: $($_.Exception.Message)"
        Warn 'Se qualche file e in uso, riavvia Windows e rilancia l uninstall.'
    }
} else {
    Warn 'Cartella C:\WSA-RiPlay non presente.'
}

$wsaData = Join-Path $env:LOCALAPPDATA 'Packages\MicrosoftCorporationII.WindowsSubsystemForAndroid_8wekyb3d8bbwe'
if (Test-Path -LiteralPath $wsaData) {
    if (AskYesNo 'Vuoi rimuovere anche i dati utente WSA in AppData') {
        try {
            Remove-Item -LiteralPath $wsaData -Recurse -Force -ErrorAction Stop
            Ok 'Dati utente WSA rimossi.'
        } catch {
            Warn "Errore rimozione dati utente WSA: $($_.Exception.Message)"
        }
    } else {
        Info 'Dati utente WSA lasciati intatti.'
    }
}

Write-Host ''
Ok 'Disinstallazione completata.'
Write-Host "Log: $LOG_FILE" -ForegroundColor DarkGray
exit 0
