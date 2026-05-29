# --- CONFIGURAZIONE ADB ---
# Forza l'uso del file adb.exe presente nella stessa cartella dello script
 $localAdb = Join-Path $PSScriptRoot "adb.exe"

# Verifica che adb esista davvero nella cartella
if (-not (Test-Path $localAdb)) {
    Write-Host "ERRORE CRITICO: Non trovo 'adb.exe' in questa cartella:" -ForegroundColor Red
    Write-Host $PSScriptRoot -ForegroundColor Red
    Write-Host ""
    Write-Host "Assicurati di aver copiato il file 'adb.exe' (e le eventuali dll) qui dentro." -ForegroundColor Yellow
    Write-Host "Premi un tasto per uscire..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit
}

# --- RILEVAMENTO FILE APK ---

# 1. Controlla Drag & Drop
if ($args.Count -gt 0) {
    $apkPath = $args[0]
}
# 2. Cerca APK nella cartella
else {
    $currentDir = $PSScriptRoot
    $foundApks = Get-ChildItem -Path $currentDir -Filter *.apk

    if ($foundApks.Count -eq 1) {
        $apkPath = $foundApks.FullName
        Write-Host "Nessun file trascinato: utilizzo automatico di $($foundApks.Name)" -ForegroundColor Cyan
    } 
    elseif ($foundApks.Count -gt 1) {
        Write-Host "ERRORE: Ci sono più file .apk." -ForegroundColor Red
        Write-Host "Trascina quello giusto sul file .bat." -ForegroundColor Red
        pause
        exit
    }
    else {
        Write-Host "ERRORE: Nessun file .apk trovato nella cartella." -ForegroundColor Red
        pause
        exit
    }
}

# --- VALIDAZIONE APK ---
if (-not (Test-Path $apkPath)) {
    Write-Host "ERRORE: File APK non valido: $apkPath" -ForegroundColor Red
    pause
    exit
}

# --- CONFIGURAZIONE PORTA WSA ---
# Se usi una porta statica in WSA (es. 58526), inseriscila qui.
# Se usi quella dinamica, lascia vuoto o rimuovi la sezione di connessione sotto.
 $WSA_Port = "58526" 
 
# --- CONNESSIONE FORZATA ---
if ($WSA_Port) {
    Write-Host "Tentativo di connessione forzata a 127.0.0.1:$WSA_Port..." -ForegroundColor Yellow
    # Esegue il comando connect, sopprime l'output standard ma controlla l'errore
    $connectResult = & $localAdb connect "127.0.0.1:$WSA_Port" 2>&1
    
    if ($connectResult -match "connected") {
        Write-Host "[OK] Connessione stabilita." -ForegroundColor Green
    } elseif ($connectResult -match "already connected") {
        Write-Host "[INFO] Già connesso." -ForegroundColor DarkGray
    } else {
        Write-Host "[ATTENZIONE] Impossibile connettersi alla porta $WSA_Port." -ForegroundColor Red
        Write-Host "Verifica che WSA sia acceso e che la porta sia corretta." -ForegroundColor Red
        # Non facciamo exit, perché magari era già connesso in un altro modo e il comando devices sotto funzionerà comunque
    }
    Write-Host ""
} 

# --- ESECUZIONE COMANDI ADB ---
# Nota l'uso di & "$localAdb" per eseguire il file specifico trovato sopra

Write-Host "--------------------------------------------------" -ForegroundColor Cyan
Write-Host "  Installatore APK per WSA (ADB Locale)" -ForegroundColor Cyan
Write-Host "--------------------------------------------------" -ForegroundColor Cyan
Write-Host ""

# 1. Lista dispositivi
Write-Host "Ricerca dispositivi WSA..." -ForegroundColor Yellow
 $devicesOutput = & $localAdb devices -l

# 2. Cerca WSA (127.0.0.1)
 $target = $devicesOutput | Select-String "127.0.0.1" | Select-Object -First 1

if ($target) {
    $deviceId = ($target.ToString().Split(" ", [StringSplitOptions]::RemoveEmptyEntries))[0]
    
    Write-Host "[OK] Trovato WSA con ID: $deviceId" -ForegroundColor Green
    Write-Host "Installazione in corso di $apkPath..." -ForegroundColor Yellow
    
    # 3. Installa
    & $localAdb -s $deviceId install -r $apkPath

    Write-Host ""
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Completato!" -ForegroundColor Green
} else {
    Write-Host "[ERRORE] Dispositivo WSA non trovato." -ForegroundColor Red
    Write-Host "Output di adb devices:" -ForegroundColor Red
    Write-Host $devicesOutput
}

Write-Host ""
Write-Host "Premi un tasto per chiudere..."
 $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")