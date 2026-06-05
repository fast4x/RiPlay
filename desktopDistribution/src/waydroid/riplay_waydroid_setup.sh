#!/bin/bash

# ==============================================================================
# Installazione e provisioning WayDroid e RiPlay su Linux
# Autore: Fast4x
# Descrizione: Installa WayDroid, inizializza il container Android e installa
#              un APK direttamente da un URL GitHub.
# ==============================================================================

set -e # Esci immediatamente se un comando fallisce

# --- CONFIGURAZIONE ---
# Sostituisci questo URL con l'URL diretto del tuo APK su GitHub Releases
APK_URL="https://github.com/fast4x/RiPlay/releases/download/v0.7.83/RiPlay-full-release-0.7.83.apk"
APK_NAME="RiPlay-full-release-0.7.83.apk"

# Colori per l'output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# --- FUNZIONI DI UTILITÀ ---
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# --- CONTROLLI PRELIMINARI ---
info "Controllo i prerequisiti di sistema..."

# Verifica che Wayland sia attivo
if [ "$XDG_SESSION_TYPE" != "wayland" ]; then
    warn "WayDroid richiede Wayland. La tua sessione corrente sembra essere $XDG_SESSION_TYPE."
    warn "Se sei su X11, l'avvio della GUI fallirà a meno che tu non usi weston/xcage."
    # Non esco perché l'installazione può avvenire comunque, ma l'avvio richiederà wayland
fi

# Verifica i permessi di root
if [ "$EUID" -ne 0 ]; then
    error "Questo script deve essere eseguito con i privilegi di root (usa sudo)."
fi

# --- FASE 1: INSTALLAZIONE DI WAYDROID ---
install_waydroid() {
    if command -v waydroid &> /dev/null; then
        info "WayDroid è già installato. Salto l'installazione."
        return 0
    fi

    info "Installazione di WayDroid tramite lo script ufficiale..."
    # Lo script ufficiale gestisce automaticamente Debian/Ubuntu, Fedora, Arch, ecc.
    curl -s https://repo.waydro.id | bash

    if ! command -v waydroid &> /dev/null; then
        error "Installazione di WayDroid fallita."
    fi
    info "WayDroid installato con successo."
}

# --- FASE 2: INIZIALIZZAZIONE DI WAYDROID ---
initialize_waydroid() {
    info "Verifica lo stato dell'animazione/container di WayDroid..."

    # Controlla se waydroid è già inizializzato controllando se le immagini esistono
    if [ -f /var/lib/waydroid/images/system.img ]; then
        info "Le immagini di WayDroid sono già presenti. Salto l'inizializzazione."
    else
        info "Inizializzazione di WayDroid (download delle immagini Android)..."
        info "Questo potrebbe richiedere qualche minuto a seconda della tua connessione."
        # -s: modalità silent, -g: senza Google Apps (più leggero e privato)
        waydroid init -s -g
    fi
}

# --- FASE 3: AVVIO DELLA SESSIONE ---
start_session() {
    info "Avvio del demone WayDroid..."
    waydroid session start

    # WayDroid richiede qualche secondo per avviare il container Android
    info "Attendo che il container Android sia pronto..."
    sleep 10

    # Verifica che il container sia su
    if ! waydroid status | grep -q "RUNNING"; then
        error "Il container WayDroid non si è avviato correttamente. Controlla i log con 'waydroid log'."
    fi
    info "Sessione WayDroid avviata con successo."
}

# --- FASE 4: DOWNLOAD E INSTALLAZIONE APK ---
install_apk() {
    local temp_apk="/tmp/${APK_NAME}"

    info "Download dell'APK da GitHub..."
    # -L segue i redirect (comune nei link di download di GitHub)
    curl -L -o "$temp_apk" "$APK_URL"

    if [ ! -f "$temp_apk" ] || [ ! -s "$temp_apk" ]; then
        error "Download dell'APK fallito o file vuoto. Controlla l'URL su GitHub."
    fi

    info "Installazione dell'APK nel container WayDroid..."
    # Il comando app install installa l'apk nel sistema Android
    waydroid app install "$temp_apk"

    info "Pulizia dei file temporanei..."
    rm -f "$temp_apk"
}

# --- ESECUZIONE PRINCIPALE ---
main() {
    info "=== Inizio configurazione WayDroid ==="
    install_waydroid
    initialize_waydroid
    start_session
    install_apk
    info "=== Configurazione completata! ==="
    echo ""
    info "Puoi avviare l'interfaccia Android digitando: waydroid show-full-ui"
    info "Oppure puoi avviare direttamente RiPlay con: waydroid app launch it.fast4x.riplay"
}

main