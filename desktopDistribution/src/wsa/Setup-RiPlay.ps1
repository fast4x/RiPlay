# ================================================================
#  RiPlay Setup automatico per WSA / WSABuilds - v12
#
#  Avviare solo Setup-RiPlay.cmd oppure Setup-RiPlay.bat.
#  Log in tempo reale: Setup-RiPlay.log nella stessa cartella.
# ================================================================

#Requires -Version 5.1
param(
    [string]$LogFile = '',
    [switch]$FromBat
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# ---------------- Configurazione ----------------
$APP_NAME       = 'RiPlay'
$PACKAGE_NAME   = 'it.fast4x.riplay'
$APK_NAME       = 'RiPlay.apk'
$APK_GITHUB_URL = 'https://github.com/fast4x/RiPlay/releases/download/v0.7.80/RiPlay-full-release-0.7.80.apk'

$WSA_URL = 'https://github.com/MustardChef/WSABuilds/releases/download/Windows_11_2407.40000.4.0_LTS_7_HOTFIX_1/WSA_2407.40000.4.0_x64_Release-Nightly-with-magisk-30.6.30600.-stable-NoGApps-NoAmazon.7z'
$WSA_INSTALL_DIR = 'C:\WSA-RiPlay'
$LAUNCHER_ICON_NAME = 'it.fast4x.riplay.ico'

$SCRIPT_DIR = Split-Path -Parent $PSCommandPath
$TOOLS_DIR  = Join-Path $SCRIPT_DIR 'tools'
$TEMP_ROOT  = Join-Path $env:TEMP 'RiPlay-WSA-Setup'
$EXTRACT_DIR = Join-Path $TEMP_ROOT 'extract'
$WSA_ARCHIVE = Join-Path $TEMP_ROOT 'WSA-RiPlay.7z'
$APK_PATH_TEMP = Join-Path $TEMP_ROOT $APK_NAME
$PLATFORM_TOOLS_ZIP = Join-Path $TEMP_ROOT 'platform-tools-latest-windows.zip'

if ([string]::IsNullOrWhiteSpace($LogFile)) {
    $LOG_FILE = Join-Path $SCRIPT_DIR 'Setup-RiPlay.log'
}
else {
    $LOG_FILE = [System.IO.Path]::GetFullPath($LogFile)
}

$ADB_TARGETS = @('127.0.0.1:58526', 'localhost:58526')
$ADB_WAIT_SECONDS = 900

# ---------------- Helper base ----------------
function Ensure-Folder {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path)) { return }
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Write-LogLine {
    param(
        [string]$Level,
        [string]$Message,
        [ConsoleColor]$Color = [ConsoleColor]::White
    )
    $line = '[{0}] [{1}] {2}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Level, $Message
    try { Add-Content -LiteralPath $LOG_FILE -Value $line -Encoding UTF8 } catch {}
    Write-Host $line -ForegroundColor $Color
}

function Info { param([string]$Message) Write-LogLine 'INFO' $Message Cyan }
function Ok   { param([string]$Message) Write-LogLine 'OK'   $Message Green }
function Warn { param([string]$Message) Write-LogLine 'WARN' $Message Yellow }
function Fail { param([string]$Message) Write-LogLine 'ERR'  $Message Red; throw $Message }

function Section {
    param([string]$Title)
    Write-Host ''
    Write-Host '============================================================' -ForegroundColor DarkCyan
    Write-Host " $Title" -ForegroundColor Cyan
    Write-Host '============================================================' -ForegroundColor DarkCyan
    try { Add-Content -LiteralPath $LOG_FILE -Value ("`r`n===== $Title =====") -Encoding UTF8 } catch {}
}

function Format-MB {
    param([int64]$Bytes)
    return ('{0:N1} MB' -f ($Bytes / 1MB))
}

function LogNativeLine {
    param([object]$Line)
    if ($null -eq $Line) { return }
    $text = $Line.ToString()
    if ([string]::IsNullOrWhiteSpace($text)) { return }
    Write-LogLine 'CMD' $text DarkGray
}

function Invoke-LoggedNative {
    param(
        [scriptblock]$Command,
        [string]$Label,
        [string]$WorkingDirectory = $SCRIPT_DIR
    )
    Info "Avvio processo: $Label"
    Info "Working directory: $WorkingDirectory"
    Push-Location -LiteralPath $WorkingDirectory
    try {
        $global:LASTEXITCODE = $null
        & $Command 2>&1 | ForEach-Object { LogNativeLine $_ }
        $code = $LASTEXITCODE
        if ($null -eq $code) { $code = 0 }
        Info ('Exit code {0}: {1}' -f $Label, $code)
        return [int]$code
    }
    finally {
        Pop-Location
    }
}

function Test-IsAdmin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

# ---------------- Download ----------------
function Download-File {
    param(
        [string]$Url,
        [string]$OutFile,
        [string]$Label,
        [int64]$MinBytes = 1
    )

    Ensure-Folder (Split-Path -Parent $OutFile)

    if (Test-Path -LiteralPath $OutFile) {
        $len = (Get-Item -LiteralPath $OutFile).Length
        if ($len -ge $MinBytes) {
            Ok "$Label gia presente: $OutFile ($(Format-MB $len))"
            return
        }
        Remove-Item -LiteralPath $OutFile -Force -ErrorAction SilentlyContinue
    }

    Info "Download $Label..."
    Info $Url
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

    $tmp = "$OutFile.download"
    Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue

    try {
        $request = [System.Net.HttpWebRequest]::Create($Url)
        $request.UserAgent = 'RiPlay-WSA-Setup'
        $request.AllowAutoRedirect = $true
        $response = $request.GetResponse()
        $total = [int64]$response.ContentLength
        $stream = $response.GetResponseStream()
        $fileStream = [System.IO.File]::Open($tmp, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write, [System.IO.FileShare]::Read)

        try {
            $buffer = New-Object byte[] (1024 * 1024)
            $received = [int64]0
            $lastPercent = -1
            $lastProgressLog = (Get-Date).AddSeconds(-30)

            while (($read = $stream.Read($buffer, 0, $buffer.Length)) -gt 0) {
                $fileStream.Write($buffer, 0, $read)
                $fileStream.Flush()
                $received += $read
                $now = Get-Date

                if ($total -gt 0) {
                    $percent = [int][Math]::Floor(($received * 100.0) / $total)
                    if (($percent -ge ($lastPercent + 5)) -or (($now - $lastProgressLog).TotalSeconds -ge 20)) {
                        $lastPercent = $percent
                        $lastProgressLog = $now
                        Info ("$Label download: $percent% ({0} / {1})" -f (Format-MB $received), (Format-MB $total))
                    }
                }
                else {
                    if (($now - $lastProgressLog).TotalSeconds -ge 20) {
                        $lastProgressLog = $now
                        Info ("$Label download: {0}" -f (Format-MB $received))
                    }
                }
            }
        }
        finally {
            if ($fileStream) { $fileStream.Dispose() }
            if ($stream) { $stream.Dispose() }
            if ($response) { $response.Dispose() }
        }

        Move-Item -LiteralPath $tmp -Destination $OutFile -Force
    }
    catch {
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
        Fail "Download fallito per $Label. Dettaglio: $($_.Exception.Message)"
    }

    if (-not (Test-Path -LiteralPath $OutFile)) { Fail "$Label non scaricato correttamente." }
    $len2 = (Get-Item -LiteralPath $OutFile).Length
    if ($len2 -lt $MinBytes) { Fail "$Label scaricato ma troppo piccolo: $len2 byte." }
    Ok "$Label scaricato: $OutFile ($(Format-MB $len2))"
}

# ---------------- Preparazione tool ----------------
function Get-Installed7ZipPath {
    $candidates = @(
        (Join-Path $env:ProgramFiles '7-Zip\7z.exe'),
        (Join-Path ${env:ProgramFiles(x86)} '7-Zip\7z.exe')
    )
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate }
    }
    $cmd = Get-Command '7z.exe' -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Ensure-Extractor {
    Section 'Preparazione estrattore 7z'
    $installed = Get-Installed7ZipPath
    if ($installed) {
        Ok "Uso 7-Zip installato: $installed"
        return $installed
    }

    Ensure-Folder $TOOLS_DIR
    $local7zr = Join-Path $TOOLS_DIR '7zr.exe'
    try {
        Download-File -Url 'https://www.7-zip.org/a/7zr.exe' -OutFile $local7zr -Label '7zr.exe portatile' -MinBytes 200000
        Ok "Uso 7zr portatile: $local7zr"
        return $local7zr
    }
    catch {
        Warn "Download 7zr portatile non riuscito: $($_.Exception.Message)"
    }

    $winget = Get-Command 'winget.exe' -ErrorAction SilentlyContinue
    if ($winget) {
        Info 'Provo installazione automatica 7-Zip con winget...'
        $wingetExit = Invoke-LoggedNative -Label 'Installazione 7-Zip via winget' -WorkingDirectory $SCRIPT_DIR -Command {
            & winget install --id 7zip.7zip -e --accept-package-agreements --accept-source-agreements
        }
        if ($wingetExit -ne 0) { Warn "winget ha restituito codice $wingetExit." }
        $installed2 = Get-Installed7ZipPath
        if ($installed2) {
            Ok "7-Zip installato: $installed2"
            return $installed2
        }
    }

    Fail 'Non riesco a ottenere 7-Zip/7zr. Installa 7-Zip e rilancia Setup-RiPlay.bat.'
}

function Ensure-Adb {
    Section 'Preparazione ADB'
    Ensure-Folder $TOOLS_DIR
    $adb = Join-Path $TOOLS_DIR 'platform-tools\adb.exe'
    if (Test-Path -LiteralPath $adb) {
        Ok "ADB locale gia presente: $adb"
        return $adb
    }

    Download-File -Url 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $PLATFORM_TOOLS_ZIP -Label 'Android Platform Tools' -MinBytes 1000000

    $platformDest = Join-Path $TOOLS_DIR 'platform-tools'
    if (Test-Path -LiteralPath $platformDest) { Remove-Item -LiteralPath $platformDest -Recurse -Force }
    Info 'Estrazione Android Platform Tools...'
    Expand-Archive -LiteralPath $PLATFORM_TOOLS_ZIP -DestinationPath $TOOLS_DIR -Force

    if (-not (Test-Path -LiteralPath $adb)) { Fail "ADB non trovato dopo estrazione Platform Tools: $adb" }
    Ok "ADB pronto: $adb"
    return $adb
}

# ---------------- WSA ----------------
function Enable-WindowsFeaturesIfNeeded {
    Section 'Verifica funzionalita Windows'
    $featureNames = @('VirtualMachinePlatform')
    $rebootNeeded = $false

    foreach ($name in $featureNames) {
        try {
            $feature = Get-WindowsOptionalFeature -Online -FeatureName $name -ErrorAction Stop
            if ($feature.State -ne 'Enabled') {
                Warn "$name non e' attiva. Provo ad abilitarla..."
                $result = Enable-WindowsOptionalFeature -Online -FeatureName $name -All -NoRestart
                Ok "$name abilitata. RestartNeeded: $($result.RestartNeeded)"
                if ($result.RestartNeeded) { $rebootNeeded = $true }
            }
            else {
                Ok "$name gia attiva"
            }
        }
        catch {
            Warn "Non riesco a verificare/abilitare ${name}: $($_.Exception.Message)"
        }
    }

    if ($rebootNeeded) {
        Fail 'Windows richiede un riavvio prima di completare WSA. Riavvia il PC e poi rilancia Setup-RiPlay.bat.'
    }
}

function Ensure-WsaArchive {
    Section 'Download WSABuilds'
    Download-File -Url $WSA_URL -OutFile $WSA_ARCHIVE -Label 'WSABuilds' -MinBytes 500000000
}

function Extract-Wsa {
    param([string]$Extractor)
    Section 'Estrazione WSA in C:\WSA-RiPlay'

    if (Test-Path -LiteralPath (Join-Path $WSA_INSTALL_DIR 'Run.bat')) {
        Ok "Cartella WSA gia presente e valida: $WSA_INSTALL_DIR"
        return
    }

    if (Test-Path -LiteralPath $EXTRACT_DIR) { Remove-Item -LiteralPath $EXTRACT_DIR -Recurse -Force }
    Ensure-Folder $EXTRACT_DIR

    Info 'Estrazione archivio WSA. Puo richiedere diversi minuti...'
    $extractExit = Invoke-LoggedNative -Label 'Estrazione archivio WSA' -WorkingDirectory $SCRIPT_DIR -Command {
        & $Extractor x $WSA_ARCHIVE ("-o$EXTRACT_DIR") -y
    }
    if ($extractExit -ne 0) { Fail "Errore durante estrazione WSA. Codice: $extractExit" }

    $source = $null
    if (Test-Path -LiteralPath (Join-Path $EXTRACT_DIR 'Run.bat')) {
        $source = Get-Item -LiteralPath $EXTRACT_DIR
    }
    else {
        $source = Get-ChildItem -Path $EXTRACT_DIR -Directory -Recurse -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'Run.bat') } |
            Select-Object -First 1
    }

    if (-not $source) { Fail 'Non trovo Run.bat nel pacchetto WSA estratto. Struttura archivio inattesa.' }

    if (Test-Path -LiteralPath $WSA_INSTALL_DIR) {
        $backup = '{0}_backup_{1}' -f $WSA_INSTALL_DIR, (Get-Date -Format 'yyyyMMdd_HHmmss')
        Warn "La cartella $WSA_INSTALL_DIR esiste ma non contiene Run.bat. La rinomino in $backup"
        Rename-Item -LiteralPath $WSA_INSTALL_DIR -NewName (Split-Path $backup -Leaf)
    }

    Ensure-Folder $WSA_INSTALL_DIR
    Copy-Item -Path (Join-Path $source.FullName '*') -Destination $WSA_INSTALL_DIR -Recurse -Force

    try {
        Get-ChildItem -Path $WSA_INSTALL_DIR -Recurse -ErrorAction SilentlyContinue | Unblock-File -ErrorAction SilentlyContinue
    } catch {}

    Ok "WSA copiato in $WSA_INSTALL_DIR"
}

function Install-Wsa {
    Section 'Installazione / registrazione WSA'
    $runBat = Join-Path $WSA_INSTALL_DIR 'Run.bat'
    $installPs1 = Join-Path $WSA_INSTALL_DIR 'Install.ps1'

    if (Test-Path -LiteralPath $runBat) {
        Info 'Avvio Run.bat di WSABuilds. Accetta eventuali finestre di conferma di Windows.'
        $runExit = Invoke-LoggedNative -Label 'Run.bat WSABuilds' -WorkingDirectory $WSA_INSTALL_DIR -Command {
            & $runBat
        }
        if ($runExit -eq 0) {
            Ok 'Run.bat completato.'
            return
        }
        Warn "Run.bat ha restituito codice $runExit. Provo Install.ps1 se disponibile."
    }

    if (Test-Path -LiteralPath $installPs1) {
        Info 'Avvio Install.ps1 di WSABuilds...'
        $installExit = Invoke-LoggedNative -Label 'Install.ps1 WSABuilds' -WorkingDirectory $WSA_INSTALL_DIR -Command {
            & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $installPs1
        }
        if ($installExit -eq 0) {
            Ok 'Install.ps1 completato.'
            return
        }
        Fail "Install.ps1 ha restituito codice $installExit."
    }

    Fail 'Non trovo Run.bat o Install.ps1 nella cartella WSA.'
}

function Get-WsaClientExePath {
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA 'Microsoft\WindowsApps\MicrosoftCorporationII.WindowsSubsystemForAndroid_8wekyb3d8bbwe\WsaClient.exe'),
        (Join-Path $env:LOCALAPPDATA 'Microsoft\WindowsApps\WsaClient.exe')
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }

    try {
        $found = Get-ChildItem -Path $WSA_INSTALL_DIR -Filter 'WsaClient.exe' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    } catch {}

    return $null
}

function Open-WsaSettings {
    Section 'Apertura Windows Subsystem for Android'
    $opened = $false

    # Obiettivo: aprire la schermata principale dell'app Windows Subsystem for Android.
    # L'utente deve solo attivare Developer Mode. Non serve aprire USB debugging.
    try {
        Start-Process 'wsa-settings://'
        $opened = $true
        Ok 'Schermata Windows Subsystem for Android aperta tramite wsa-settings://'
        Start-Sleep -Seconds 2
    } catch {
        Warn "Apertura con wsa-settings:// non riuscita: $($_.Exception.Message)"
    }

    if (-not $opened) {
        try {
            $pkg = Get-AppxPackage -Name '*WindowsSubsystemForAndroid*' -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($pkg) {
                Start-Process 'explorer.exe' -ArgumentList "shell:AppsFolder\$($pkg.PackageFamilyName)!App"
                $opened = $true
                Ok 'Windows Subsystem for Android aperto dal pacchetto Appx.'
            }
        } catch {
            Warn "Apertura WSA da AppsFolder non riuscita: $($_.Exception.Message)"
        }
    }

    if (-not $opened) {
        try {
            $settingsExe = Get-ChildItem -Path $WSA_INSTALL_DIR -Filter 'WsaSettings.exe' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($settingsExe) {
                Start-Process -FilePath $settingsExe.FullName
                $opened = $true
                Ok "WsaSettings.exe avviato: $($settingsExe.FullName)"
            }
        } catch {
            Warn "WsaSettings.exe non avviato: $($_.Exception.Message)"
        }
    }

    if (-not $opened) {
        Warn 'Non riesco ad aprire WSA automaticamente. Apri manualmente dal menu Start: Windows Subsystem for Android.'
    }
}


function Test-TcpPortOpen {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMs = 800
    )
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect($HostName, $Port, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
        if (-not $ok) {
            $client.Close()
            return $false
        }
        $client.EndConnect($iar)
        $client.Close()
        return $true
    }
    catch {
        try { if ($client) { $client.Close() } } catch {}
        return $false
    }
}

function Start-WsaRuntime {
    param(
        [switch]$Silent,
        [switch]$Foreground
    )

    $client = Get-WsaClientExePath
    if (-not $client) {
        if (-not $Silent) { Warn 'WsaClient.exe non trovato: non posso forzare automaticamente l avvio del runtime Android.' }
        return $false
    }

    # Developer Mode da solo puo non aprire/mostrare il prompt RSA ADB se il runtime Android non e davvero in foreground.
    # Avvio Android Settings tramite WsaClient: non serve ad abilitare USB debugging, serve a portare Android/WSA davanti.
    $uris = @('wsa://com.android.settings')
    foreach ($uri in $uris) {
        try {
            Info "Avvio runtime Android WSA tramite WsaClient: $uri"
            if ($Foreground) {
                Start-Process -FilePath $client -ArgumentList @('/launch', $uri) -ErrorAction SilentlyContinue | Out-Null
            }
            else {
                Start-Process -FilePath $client -ArgumentList @('/launch', $uri) -WindowStyle Hidden -ErrorAction SilentlyContinue | Out-Null
            }
            Start-Sleep -Seconds 4
            return $true
        }
        catch {
            Warn "Non riesco ad avviare WSA runtime con WsaClient: $($_.Exception.Message)"
        }
    }
    return $false
}

function Show-AdbAuthorizationInstructions {
    Write-Host ''
    Write-Host 'AZIONE RICHIESTA IN WSA' -ForegroundColor Yellow
    Write-Host 'ADB ha trovato WSA, ma il PC non e ancora autorizzato.' -ForegroundColor Yellow
    Write-Host 'Se compare la finestra Android "Allow ADB debugging":' -ForegroundColor Yellow
    Write-Host '  1) seleziona Always allow from this computer, se presente' -ForegroundColor Yellow
    Write-Host '  2) premi Allow / Consenti' -ForegroundColor Yellow
    Write-Host ''
    Write-Host 'Non devi abilitare USB debugging a mano: serve solo confermare questa autorizzazione una volta.' -ForegroundColor Yellow
    Write-Host ''
}

function Get-AdbDeviceState {
    param(
        [string]$DevicesOutput,
        [string]$Target
    )
    $escaped = [regex]::Escape($Target)
    $line = ($DevicesOutput -split "`r?`n" | Where-Object { $_ -match "^\s*$escaped\s+" } | Select-Object -First 1)
    if (-not $line) { return '' }
    if ($line -match '\bdevice\b') { return 'device' }
    if ($line -match '\bunauthorized\b') { return 'unauthorized' }
    if ($line -match '\boffline\b') { return 'offline' }
    return 'unknown'
}

# ---------------- ADB / RiPlay ----------------
function Wait-ForWsaAdb {
    param([string]$Adb)

    Section 'Attesa Developer Mode WSA'
    Write-Host ''
    Write-Host 'Ora devi fare solo questo in WSA:' -ForegroundColor Yellow
    Write-Host '  1) Vai in Advanced settings / Impostazioni avanzate' -ForegroundColor Yellow
    Write-Host '  2) Attiva Developer mode / Modalita sviluppatore = ON' -ForegroundColor Yellow
    Write-Host ''
    Write-Host 'Se compare la finestra Android "Allow ADB debugging", premi Allow / Consenti.' -ForegroundColor Yellow
    Write-Host 'NON chiudere questa finestra: appena ADB e autorizzato, lo script prosegue da solo.' -ForegroundColor Yellow
    Write-Host ''

    try { & $Adb kill-server | Out-Null } catch {}
    try { & $Adb start-server | Out-Null } catch {}

    $deadline = (Get-Date).AddSeconds($ADB_WAIT_SECONDS)
    $lastStatus = ''
    $loop = 0
    $runtimeKickDone = $false
    $manualHintShown = $false
    $authorizationHelpShown = $false
    $lastRuntimeKick = (Get-Date).AddMinutes(-10)
    $lastUnauthorizedKick = (Get-Date).AddMinutes(-10)
    $firstUnauthorizedAt = $null
    $restartedAdbAfterUnauthorized = $false

    while ((Get-Date) -lt $deadline) {
        $loop++

        # Prima controllo se la porta locale ADB di WSA e aperta. Se non e aperta, spesso il runtime Android non e ancora avviato.
        $portOpen = Test-TcpPortOpen -HostName '127.0.0.1' -Port 58526 -TimeoutMs 700
        if (-not $portOpen) {
            if (-not $runtimeKickDone -or (((Get-Date) - $lastRuntimeKick).TotalSeconds -ge 45)) {
                $runtimeKickDone = $true
                $lastRuntimeKick = Get-Date
                Info 'Porta ADB WSA 127.0.0.1:58526 non ancora aperta: provo ad avviare il runtime Android.'
                [void](Start-WsaRuntime -Silent)
            }
        }

        foreach ($target in $ADB_TARGETS) {
            try {
                $connectOutput = (& $Adb connect $target 2>&1) -join "`n"
                $devicesOutput = (& $Adb devices 2>&1) -join "`n"
                try { Add-Content -LiteralPath $LOG_FILE -Value "ADB connect ${target}:`r`n$connectOutput`r`n$devicesOutput" -Encoding UTF8 } catch {}

                $state = Get-AdbDeviceState -DevicesOutput $devicesOutput -Target $target
                if ($state -eq 'device') {
                    Ok "ADB collegato e autorizzato su $target"
                    return $target
                }
                elseif ($state -eq 'unauthorized') {
                    if ($null -eq $firstUnauthorizedAt) { $firstUnauthorizedAt = Get-Date }

                    $status = 'ADB vede WSA ma e unauthorized: serve confermare la finestra Android "Allow ADB debugging".'
                    if ($status -ne $lastStatus) { Warn $status; $lastStatus = $status }

                    if (-not $authorizationHelpShown) {
                        $authorizationHelpShown = $true
                        Show-AdbAuthorizationInstructions
                        Info 'Porto WSA/Android Settings in primo piano per far comparire la richiesta di autorizzazione ADB.'
                        [void](Start-WsaRuntime -Foreground -Silent)
                    }
                    elseif (((Get-Date) - $lastUnauthorizedKick).TotalSeconds -ge 25) {
                        $lastUnauthorizedKick = Get-Date
                        Info 'WSA e ancora unauthorized: ritento adb connect e porto WSA in foreground.'
                        [void](Start-WsaRuntime -Foreground -Silent)
                    }

                    # Se il prompt non compare, un riavvio leggero del server ADB sul PC spesso rigenera la richiesta RSA.
                    if ((-not $restartedAdbAfterUnauthorized) -and (((Get-Date) - $firstUnauthorizedAt).TotalSeconds -ge 60)) {
                        $restartedAdbAfterUnauthorized = $true
                        Warn 'WSA resta unauthorized da circa 60 secondi: riavvio il server ADB locale e ritento.'
                        try { & $Adb disconnect $target | Out-Null } catch {}
                        try { & $Adb kill-server | Out-Null } catch {}
                        Start-Sleep -Seconds 2
                        try { & $Adb start-server | Out-Null } catch {}
                    }
                }
                elseif ($state -eq 'offline') {
                    $status = "ADB vede WSA su $target ma risulta offline. Attendo riavvio del runtime..."
                    if ($status -ne $lastStatus) { Warn $status; $lastStatus = $status }
                    try { & $Adb disconnect $target | Out-Null } catch {}
                }
                elseif ($connectOutput -match 'connected to|already connected') {
                    $status = "ADB si collega a $target, attendo che Android sia pronto..."
                    if ($status -ne $lastStatus) { Info $status; $lastStatus = $status }
                }
            }
            catch {
                try { Add-Content -LiteralPath $LOG_FILE -Value "ADB errore su ${target}: $($_.Exception.Message)" -Encoding UTF8 } catch {}
            }
        }

        if (($loop % 6) -eq 0) {
            Info 'Sto ancora aspettando autorizzazione ADB/WSA...'
        }

        if (($loop -ge 18) -and (-not $manualHintShown)) {
            $manualHintShown = $true
            Warn 'Se non compare il popup: clicca una volta dentro la finestra Android/WSA aperta, oppure chiudi e riapri Developer Mode. Poi conferma Allow ADB debugging.'
        }

        Start-Sleep -Seconds 5
    }

    Fail 'Timeout: WSA e raggiungibile ma ADB non e autorizzato. Attiva Developer Mode e conferma Allow ADB debugging quando compare.'
}

function Wait-ForAndroidReady {
    param(
        [string]$Adb,
        [string]$DeviceId
    )

    Section 'Attesa avvio Android dentro WSA'
    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline) {
        $boot = ''
        $pm = ''
        try { $boot = (& $Adb -s $DeviceId shell getprop sys.boot_completed 2>$null | Out-String).Trim() } catch {}
        try { $pm = (& $Adb -s $DeviceId shell pm path android 2>$null | Out-String).Trim() } catch {}

        if ($boot -eq '1' -and $pm -match '^package:') {
            Ok 'Android/Package Manager pronto.'
            return
        }
        Info 'WSA collegato, attendo completamento avvio Android...'
        Start-Sleep -Seconds 5
    }
    Fail 'ADB collegato, ma Android/Package Manager non e pronto entro il timeout.'
}

function Get-RiPlayApkPath {
    Section 'Preparazione APK RiPlay'

    $localCandidates = @()
    $directLocal = Join-Path $SCRIPT_DIR $APK_NAME
    if (Test-Path -LiteralPath $directLocal) { $localCandidates += (Get-Item -LiteralPath $directLocal) }
    $localCandidates += Get-ChildItem -Path $SCRIPT_DIR -Filter '*RiPlay*.apk' -File -ErrorAction SilentlyContinue

    $apk = $localCandidates | Select-Object -First 1
    if ($apk) {
        Ok "Uso APK locale: $($apk.FullName)"
        return $apk.FullName
    }

    Download-File -Url $APK_GITHUB_URL -OutFile $APK_PATH_TEMP -Label 'RiPlay APK' -MinBytes 1000000
    return $APK_PATH_TEMP
}

function Install-RiPlayApk {
    param(
        [string]$Adb,
        [string]$DeviceId,
        [string]$ApkPath
    )

    Section 'Installazione RiPlay APK'
    Info "Installazione APK: $ApkPath"
    $out = (& $Adb -s $DeviceId install -r $ApkPath 2>&1) -join "`n"
    try { Add-Content -LiteralPath $LOG_FILE -Value "ADB install output:`r`n$out" -Encoding UTF8 } catch {}

    if ($out -match 'Success') {
        Ok 'RiPlay installata in WSA.'
        return
    }

    if ($out -match 'INSTALL_FAILED_VERSION_DOWNGRADE') {
        Warn 'Sul sistema e presente una versione piu recente di RiPlay. Non faccio downgrade automatico per non perdere dati.'
        Warn 'Se vuoi forzare, disinstalla RiPlay da WSA e rilancia il setup.'
        return
    }

    Fail "Installazione RiPlay fallita. Dettaglio: $out"
}

function Start-RiPlay {
    param(
        [string]$Adb,
        [string]$DeviceId
    )

    Section 'Avvio RiPlay'
    try {
        & $Adb -s $DeviceId shell monkey -p $PACKAGE_NAME -c android.intent.category.LAUNCHER 1 | Out-Null
        Ok 'Comando di avvio RiPlay inviato.'
    }
    catch {
        Warn "Non sono riuscito ad avviare RiPlay automaticamente: $($_.Exception.Message)"
    }
}

function Install-RiPlayLauncherFiles {
    param([string]$AdbPath)

    Section 'Installazione launcher RiPlay'

    Ensure-Folder $WSA_INSTALL_DIR

    $sourceIco = Join-Path $SCRIPT_DIR $LAUNCHER_ICON_NAME
    $destIco   = Join-Path $WSA_INSTALL_DIR $LAUNCHER_ICON_NAME
    $destPlatformTools = Join-Path $WSA_INSTALL_DIR 'platform-tools'
    $launchBat = Join-Path $WSA_INSTALL_DIR 'Avvia RiPlay.bat'
    $oldVbs = Join-Path $WSA_INSTALL_DIR 'Avvia-Riplay.vbs'

    # Copio ADB dentro C:\WSA-RiPlay, cosi il collegamento funziona anche su PC senza ADB nel PATH.
    try {
        $sourcePlatformTools = Split-Path -Parent $AdbPath
        if (Test-Path -LiteralPath $sourcePlatformTools) {
            if (Test-Path -LiteralPath $destPlatformTools) {
                Remove-Item -LiteralPath $destPlatformTools -Recurse -Force -ErrorAction SilentlyContinue
            }
            Ensure-Folder $destPlatformTools
            Copy-Item -Path (Join-Path $sourcePlatformTools '*') -Destination $destPlatformTools -Recurse -Force
            Ok "Platform-tools copiati in: $destPlatformTools"
        }
        else {
            Warn "Cartella platform-tools non trovata: $sourcePlatformTools"
        }
    } catch {
        Warn "Non riesco a copiare platform-tools in C:\WSA-RiPlay: $($_.Exception.Message)"
    }

    # Il VBS non serve piu: se esiste da una versione precedente lo rimuovo.
    if (Test-Path -LiteralPath $oldVbs) {
        try {
            Remove-Item -LiteralPath $oldVbs -Force -ErrorAction Stop
            Info "Vecchio launcher VBS rimosso: $oldVbs"
        } catch {
            Warn "Non riesco a rimuovere il vecchio VBS: $($_.Exception.Message)"
        }
    }

    # Creo il launcher BAT diretto. Prima prova WsaClient /launch, cosi dopo l'installazione non serve ADB.
    # Se WsaClient non e disponibile, resta il fallback ADB.
    $launcherLines = @(
        '@echo off',
        'setlocal EnableExtensions',
        'chcp 65001 >nul',
        'cd /d "%~dp0"',
        'set "CLIENT=%LOCALAPPDATA%\Microsoft\WindowsApps\MicrosoftCorporationII.WindowsSubsystemForAndroid_8wekyb3d8bbwe\WsaClient.exe"',
        'if exist "%CLIENT%" (',
        ('  start "" "%CLIENT%" /launch wsa://' + $PACKAGE_NAME),
        '  exit /b 0',
        ')',
        'set "CLIENT=%LOCALAPPDATA%\Microsoft\WindowsApps\WsaClient.exe"',
        'if exist "%CLIENT%" (',
        ('  start "" "%CLIENT%" /launch wsa://' + $PACKAGE_NAME),
        '  exit /b 0',
        ')',
        'set "ADB=%~dp0platform-tools\adb.exe"',
        'if not exist "%ADB%" set "ADB=adb.exe"',
        '"%ADB%" start-server >nul 2>&1',
        '"%ADB%" connect 127.0.0.1:58526 >nul 2>&1',
        ('"%ADB%" -s 127.0.0.1:58526 shell monkey -p ' + $PACKAGE_NAME + ' -c android.intent.category.LAUNCHER 1 >nul 2>&1'),
        'if "%errorlevel%"=="0" exit /b 0',
        'echo Non riesco ad avviare RiPlay.',
        'echo Apri Windows Subsystem for Android e verifica che Developer Mode sia attivo.',
        'pause',
        'exit /b 1'
    )
    Set-Content -LiteralPath $launchBat -Value $launcherLines -Encoding ASCII
    Ok "Launcher BAT creato: $launchBat"

    if (Test-Path -LiteralPath $sourceIco) {
        Copy-Item -LiteralPath $sourceIco -Destination $destIco -Force
        Ok "Icona copiata in: $destIco"
    }
    else {
        Warn "Icona non trovata nella cartella setup: $sourceIco"
    }

    try {
        $desktop = [Environment]::GetFolderPath('Desktop')
        $oldShortcuts = @(
            (Join-Path $desktop 'RiPlay WSA.lnk'),
            (Join-Path $env:PUBLIC 'Desktop\RiPlay WSA.lnk')
        )
        foreach ($oldShortcut in $oldShortcuts) {
            if (Test-Path -LiteralPath $oldShortcut) {
                Remove-Item -LiteralPath $oldShortcut -Force -ErrorAction SilentlyContinue
                Info "Vecchio collegamento rimosso: $oldShortcut"
            }
        }

        $shortcut = Join-Path $desktop 'RiPlay.lnk'
        $shell = New-Object -ComObject WScript.Shell
        $lnk = $shell.CreateShortcut($shortcut)
        $clientForShortcut = Get-WsaClientExePath
        if ($clientForShortcut) {
            $lnk.TargetPath = $clientForShortcut
            $lnk.Arguments = '/launch wsa://' + $PACKAGE_NAME
            $lnk.WorkingDirectory = Split-Path -Parent $clientForShortcut
        }
        else {
            $lnk.TargetPath = $launchBat
            $lnk.Arguments = ''
            $lnk.WorkingDirectory = $WSA_INSTALL_DIR
        }
        if (Test-Path -LiteralPath $destIco) {
            $lnk.IconLocation = "$destIco,0"
        }
        else {
            $lnk.IconLocation = "$env:SystemRoot\System32\shell32.dll,220"
        }
        $lnk.Save()
        Ok "Collegamento Desktop creato: $shortcut"
    } catch {
        Warn "Collegamento Desktop non creato: $($_.Exception.Message)"
    }
}

# ---------------- Main ----------------
$global:SetupExitCode = 0
try {
    Clear-Host
    Ensure-Folder (Split-Path -Parent $LOG_FILE)
    Add-Content -LiteralPath $LOG_FILE -Value ('[{0}] [INFO] PowerShell setup avviato - v12.' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')) -Encoding UTF8

    if (-not (Test-IsAdmin)) {
        Fail 'Lo script PowerShell non risulta avviato come amministratore. Avvia Setup-RiPlay.bat e accetta UAC.'
    }

    Write-Host ''
    Write-Host '  +------------------------------------------------------+' -ForegroundColor Cyan
    Write-Host '  |              Setup automatico WSA RiPlay             |' -ForegroundColor Cyan
    Write-Host '  +------------------------------------------------------+' -ForegroundColor Cyan
    Write-Host ''
    Write-Host "  Log: $LOG_FILE" -ForegroundColor DarkGray
    Write-Host ''

    Ensure-Folder $TOOLS_DIR
    Ensure-Folder $TEMP_ROOT

    Enable-WindowsFeaturesIfNeeded
    $extractor = Ensure-Extractor
    $adb = Ensure-Adb
    Ensure-WsaArchive
    Extract-Wsa -Extractor $extractor
    Install-Wsa

    Start-Sleep -Seconds 3
    Open-WsaSettings
    $deviceId = Wait-ForWsaAdb -Adb $adb
    Wait-ForAndroidReady -Adb $adb -DeviceId $deviceId

    $apkPath = Get-RiPlayApkPath
    Install-RiPlayApk -Adb $adb -DeviceId $deviceId -ApkPath $apkPath
    Start-RiPlay -Adb $adb -DeviceId $deviceId
    Install-RiPlayLauncherFiles -AdbPath $adb

    Section 'Completato'
    Ok 'Setup WSA/RiPlay completato.'
    Write-Host ''
    Write-Host "RiPlay puo essere avviato dal nuovo collegamento Desktop 'RiPlay'." -ForegroundColor Green
    Write-Host "Ho creato il launcher BAT diretto, copiato l'icona in C:\WSA-RiPlay e creato il collegamento Desktop RiPlay." -ForegroundColor Green
}
catch {
    $global:SetupExitCode = 1
    Write-Host ''
    Write-Host 'ERRORE SETUP' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ''
    Write-Host "Controlla il log: $LOG_FILE" -ForegroundColor Yellow
    try { Add-Content -LiteralPath $LOG_FILE -Value ('[{0}] [ERR] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $_.Exception.Message) -Encoding UTF8 } catch {}
}
finally {
    try {
        Add-Content -LiteralPath $LOG_FILE -Value ('[{0}] [INFO] Setup terminato con codice {1}.' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $global:SetupExitCode) -Encoding UTF8
    } catch {}
    exit $global:SetupExitCode
}
