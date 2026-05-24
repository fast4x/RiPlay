# ================================================================
#  RiPlay Installer - Windows (PowerShell)
#  Installa Java 17, Android SDK, crea AVD e avvia l'emulatore.
#  Per installare RiPlay: doppio clic su "Installa APK.bat"
#  dopo che Android e' visibile sullo schermo.
#
#  Uso: doppio clic su "Installa RiPlay.bat"
# ================================================================

#Requires -Version 5.1

# -- Configurazione ----------------------------------------------
$APP_NAME     = "RiPlay"
$AVD_NAME     = "RiPlayDevice"
$ANDROID_API  = "34"
$ABI          = "x86_64"
$SYSTEM_IMAGE = "system-images;android-$ANDROID_API;google_apis;$ABI"

$CMDTOOLS_URL    = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$CMDTOOLS_SHA256 = "b771501a747e1d1699cad34f09fd2ed0e30f1ad3c3c75b71b0bbdf18a3e6dce9"
$JDK_URL         = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.msi"

$ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\sdk"
$LOG_FILE         = "$env:TEMP\riplay-install.log"
$SCRIPT_DIR       = Split-Path -Parent $MyInvocation.MyCommand.Path

# -- Helpers -----------------------------------------------------
function Info    { param($m) Write-Host "  [..] $m" -ForegroundColor Cyan;   Add-Content $LOG_FILE "[INFO]  $m" }
function Success { param($m) Write-Host "  [OK] $m" -ForegroundColor Green;  Add-Content $LOG_FILE "[OK]    $m" }
function Warn    { param($m) Write-Host "  [!!] $m" -ForegroundColor Yellow; Add-Content $LOG_FILE "[WARN]  $m" }
function Err     { param($m) Write-Host "  [XX] ERRORE: $m" -ForegroundColor Red; Add-Content $LOG_FILE "[ERR]   $m"; Read-Host "Premi Invio per uscire"; exit 1 }
function Step    { param($n,$t) Write-Host "`n  === Step $n - $t ===" -ForegroundColor White }

function Download-File {
  param($Url, $OutPath, $Label)
  Info "Download $Label ..."
  $wc = New-Object System.Net.WebClient
  $wc.DownloadFile($Url, $OutPath)
  if (-not (Test-Path $OutPath)) { Err "Download fallito: $Label" }
  Success "Download completato: $(Split-Path -Leaf $OutPath)"
}

function Verify-Hash {
  param($FilePath, $Expected)
  if ([string]::IsNullOrWhiteSpace($Expected)) { return }
  Info "Verifica SHA256 ..."
  $actual = (Get-FileHash $FilePath -Algorithm SHA256).Hash.ToLower()
  if ($actual -ne $Expected.ToLower()) {
    Err "Hash SHA256 non corrispondente! Atteso: $Expected  Trovato: $actual"
  }
  Success "Integrita verificata"
}

# -- Banner ------------------------------------------------------
Clear-Host
Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Cyan
Write-Host "  |          RiPlay Installer                |" -ForegroundColor Cyan
Write-Host "  |     Android Virtual Device - Windows     |" -ForegroundColor Cyan
Write-Host "  +------------------------------------------+" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Log: $LOG_FILE" -ForegroundColor DarkGray
Write-Host ""

"" | Out-File $LOG_FILE
Add-Content $LOG_FILE "=== RiPlay Installer $(Get-Date) ==="

# -- Step 1: Prerequisiti ----------------------------------------
Step 1 "Verifica prerequisiti"

$virt = (Get-WmiObject -Class Win32_Processor).VirtualizationFirmwareEnabled
if ($virt) { Success "Virtualizzazione hardware abilitata" }
else { Warn "VT-x non rilevato. Abilita nel BIOS per prestazioni migliori." }

$hv = (Get-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform 2>$null).State
if ($hv -eq "Enabled") { Success "Hyper-V Platform abilitato" }
else {
  Warn "Hyper-V Platform non attivo. Per abilitarlo:"
  Warn "  dism /online /Enable-Feature /FeatureName:HypervisorPlatform /All /NoRestart"
}

$drive  = Split-Path -Qualifier $env:LOCALAPPDATA
$freeGB = [Math]::Round((Get-PSDrive ($drive -replace ':','')).Free / 1GB, 1)
if ($freeGB -lt 8) { Err "Spazio insufficiente: $freeGB GB (minimo 8 GB richiesti)" }
Success "Spazio disco: $freeGB GB disponibili"

# -- Step 2: Java 17 ---------------------------------------------
Step 2 "Verifica Java"

$javaOk = $false
try {
  $jv = & java -version 2>&1 | Out-String
  if ($jv -match "17\.|18\.|19\.|20\.|21\.") { $javaOk = $true; Success "Java gia installato" }
} catch {}

if (-not $javaOk) {
  Info "Java 17 non trovato. Download Adoptium Temurin 17 (~170 MB) ..."
  $jdkMsi = "$env:TEMP\jdk17.msi"
  Download-File $JDK_URL $jdkMsi "OpenJDK 17"
  Info "Installazione JDK silenziosa ..."
  Start-Process msiexec -ArgumentList "/i `"$jdkMsi`" /qn ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome REBOOT=Suppress" -Wait
  $env:PATH = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" +
              [System.Environment]::GetEnvironmentVariable("Path","User")
  Success "Java 17 installato"
}

# -- Step 3: Android cmdline-tools -------------------------------
Step 3 "Android cmdline-tools"

$CMDTOOLS_DIR = "$ANDROID_SDK_ROOT\cmdline-tools\latest"
$SDKMANAGER   = "$CMDTOOLS_DIR\bin\sdkmanager.bat"
$AVDMANAGER   = "$CMDTOOLS_DIR\bin\avdmanager.bat"

if (Test-Path $SDKMANAGER) {
  Success "cmdline-tools gia presenti"
} else {
  $zip = "$env:TEMP\cmdtools.zip"
  Download-File $CMDTOOLS_URL $zip "Android cmdline-tools (~150 MB)"
  Verify-Hash $zip $CMDTOOLS_SHA256
  Info "Estrazione ..."
  New-Item -ItemType Directory -Force -Path "$ANDROID_SDK_ROOT\cmdline-tools" | Out-Null
  Expand-Archive -Path $zip -DestinationPath "$ANDROID_SDK_ROOT\cmdline-tools" -Force
  $extracted = Get-ChildItem "$ANDROID_SDK_ROOT\cmdline-tools" -Directory |
               Where-Object { $_.Name -ne "latest" } | Select-Object -First 1
  if ($extracted) { Rename-Item $extracted.FullName "latest" }
  Remove-Item $zip -Force
  Success "cmdline-tools installati"
}

Info "Accettazione licenze SDK ..."
"y`ny`ny`ny`ny`ny`ny`n" | & $SDKMANAGER --sdk_root="$ANDROID_SDK_ROOT" --licenses 2>&1 | Out-Null
Success "Licenze accettate"

# -- Step 4: System image + emulatore ----------------------------
Step 4 "System image Android $ANDROID_API (~1.5 GB)"

$imgPath = "$ANDROID_SDK_ROOT\system-images\android-$ANDROID_API"
if (Test-Path $imgPath) {
  Success "System image Android $ANDROID_API gia presente"
} else {
  Info "Download system image + platform-tools + emulator ..."
  Info "(Potrebbe richiedere 5-10 minuti)"
  & $SDKMANAGER --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "emulator" $SYSTEM_IMAGE 2>&1 |
    Tee-Object -Append $LOG_FILE
  Success "System image installata"
}

$EMULATOR = "$ANDROID_SDK_ROOT\emulator\emulator.exe"
$ADB      = "$ANDROID_SDK_ROOT\platform-tools\adb.exe"

# -- Step 5: Crea AVD --------------------------------------------
Step 5 "Creazione Android Virtual Device"

$avdList = & $AVDMANAGER list avd 2>$null | Out-String
if ($avdList -match $AVD_NAME) {
  Success "AVD '$AVD_NAME' gia esistente"
} else {
  Info "Creazione AVD '$AVD_NAME' (Android $ANDROID_API, Pixel Tablet 10.95) ..."
  "no" | & $AVDMANAGER create avd `
    --name $AVD_NAME `
    --package $SYSTEM_IMAGE `
    --device "pixel_tablet" `
    --force 2>&1 | Out-Null

  $avdCfg = "$env:USERPROFILE\.android\avd\$AVD_NAME.avd\config.ini"
  if (Test-Path $avdCfg) {
    $cfg = Get-Content $avdCfg
    $cfg = $cfg -replace "hw\.ramSize=.*",     "hw.ramSize=3072"
    $cfg = $cfg -replace "vm\.heapSize=.*",    "vm.heapSize=512"
    $cfg = $cfg -replace "hw\.audioInput=.*",  "hw.audioInput=yes"
    $cfg = $cfg -replace "hw\.audioOutput=.*", "hw.audioOutput=yes"
    $cfg += "hw.gpu.enabled=yes"
    $cfg += "hw.gpu.mode=auto"
    $cfg += "hw.keyboard=yes"
    $cfg += "hw.audioInput=yes"
    $cfg += "hw.audioOutput=yes"
    $cfg | Set-Content $avdCfg
  }
  Success "AVD creato e configurato (Pixel Tablet, audio abilitato)"
}

# -- Step 6: Avvio emulatore -------------------------------------
Step 6 "Avvio emulatore Android"

Info "Avvio emulatore ..."
$env:ANDROID_SDK_ROOT = $ANDROID_SDK_ROOT
Start-Process -FilePath $EMULATOR `
  -ArgumentList "-avd $AVD_NAME -no-boot-anim -gpu auto -no-snapshot-save" `
  -WindowStyle Normal

# -- Shortcut Desktop --------------------------------------------
Info "Creazione shortcut Desktop ..."

$launchDir = "$env:LOCALAPPDATA\RiPlay"
$launchPs1 = "$launchDir\launch.ps1"
New-Item -ItemType Directory -Force -Path $launchDir | Out-Null

@"
`$env:ANDROID_SDK_ROOT = "$ANDROID_SDK_ROOT"
Start-Process "$EMULATOR" -ArgumentList "-avd $AVD_NAME -no-boot-anim -gpu auto"
"@ | Set-Content $launchPs1

$WshShell = New-Object -ComObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut("$env:PUBLIC\Desktop\RiPlayDevice.lnk")
$Shortcut.TargetPath       = "powershell.exe"
$Shortcut.Arguments        = "-WindowStyle Hidden -ExecutionPolicy Bypass -File `"$launchPs1`""
$Shortcut.Description      = "Avvia RiPlay Android Virtual Device"
$Shortcut.IconLocation     = "$EMULATOR,0"
$Shortcut.WorkingDirectory = Split-Path $EMULATOR
$Shortcut.Save()
Success "Shortcut 'RiPlay Device' creato sul Desktop"

# -- Fine --------------------------------------------------------
Write-Host ""
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host "  |   Ambiente RiPlay pronto!                |" -ForegroundColor Green
Write-Host "  +------------------------------------------+" -ForegroundColor Green
Write-Host ""
Write-Host "  L'emulatore Android si sta avviando." -ForegroundColor White
Write-Host ""
Write-Host "  PROSSIMO PASSO:" -ForegroundColor Yellow
Write-Host "  Quando vedi la schermata Android, doppio clic su" -ForegroundColor White
Write-Host "  'Installa APK.bat' per installare RiPlay." -ForegroundColor White
Write-Host ""
Write-Host "  Log completo: $LOG_FILE" -ForegroundColor DarkGray
Write-Host ""
Read-Host "  Premi Invio per chiudere"
