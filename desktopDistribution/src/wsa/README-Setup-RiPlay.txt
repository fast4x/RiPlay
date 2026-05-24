SETUP AUTOMATICO WSA + RIPLAY - v12

File da avviare:
  Setup-RiPlay.cmd

File per disinstallare:
  Disinstalla-RiPlay.cmd

Log in tempo reale:
  Setup-RiPlay.log

Procedura utente:
1. Avvia Setup-RiPlay.cmd.
2. Accetta i permessi amministratore.
3. Quando si apre Windows Subsystem for Android, vai in Advanced settings / Impostazioni avanzate.
4. Attiva Developer mode / Modalita sviluppatore.
5. Se compare la finestra Android "Allow ADB debugging", premi Allow / Consenti.
   Consigliato: spunta "Always allow from this computer" se disponibile.

Nota v12:
- USB debugging non va modificato manualmente.
- Se ADB vede WSA come unauthorized, il setup porta WSA/Android Settings in primo piano e continua a ritentare.
- Quando l'autorizzazione ADB viene confermata, l'installazione di RiPlay prosegue automaticamente.
- Il collegamento Desktop creato si chiama RiPlay e usa l'icona copiata in C:\WSA-RiPlay.
