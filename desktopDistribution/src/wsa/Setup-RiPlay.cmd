@ECHO OFF
SETLOCAL EnableExtensions
CHCP 65001 >NUL
TITLE Setup RiPlay - WSA - v12
CD /D "%~dp0"

SET "LOG_FILE=%~dp0Setup-RiPlay.log"
SET "PS1_FILE=%~dp0Setup-RiPlay.ps1"
SET "PS_ERR_FILE=%TEMP%\Setup-RiPlay-powershell-error-%RANDOM%.tmp"
SET "SETUP_SELF=%~f0"
SET "SETUP_DIR=%~dp0"

IF /I "%~1"=="--elevated" GOTO ELEVATED_START

REM Log pulito a ogni nuova installazione. Il file resta nella stessa cartella del setup.
>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Avvio Setup-RiPlay - v12
>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Cartella setup: %~dp0
GOTO CHECK_FILES

:ELEVATED_START
IF NOT EXIST "%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Avvio elevato - v12>"%LOG_FILE%"
>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Proseguo come amministratore - v12
>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Cartella setup: %~dp0

:CHECK_FILES
IF EXIST "%PS_ERR_FILE%" DEL /F /Q "%PS_ERR_FILE%" >NUL 2>NUL

ECHO.
ECHO Log installazione in tempo reale:
ECHO %LOG_FILE%
ECHO.

IF NOT EXIST "%PS1_FILE%" GOTO PS1_MISSING

NET SESSION >NUL 2>NUL
IF "%ERRORLEVEL%"=="0" GOTO RUN_PS1

ECHO Richiesta autorizzazione amministratore...
>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Richiesta autorizzazione amministratore...

REM Auto-elevazione robusta anche se la cartella contiene spazi.
POWERSHELL.EXE -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$p=$env:SETUP_SELF; $w=$env:SETUP_DIR; Start-Process -FilePath $p -ArgumentList '--elevated' -WorkingDirectory $w -Verb RunAs"
IF NOT "%ERRORLEVEL%"=="0" GOTO ELEVATION_FAILED
EXIT /B 0

:RUN_PS1
>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Avvio PowerShell principale...
POWERSHELL.EXE -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%PS1_FILE%" -LogFile "%LOG_FILE%" -FromBat 2>"%PS_ERR_FILE%"
SET "EC=%ERRORLEVEL%"

IF EXIST "%PS_ERR_FILE%" (
  FOR %%A IN ("%PS_ERR_FILE%") DO IF %%~zA GTR 0 (
    >>"%LOG_FILE%" ECHO.
    >>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] Output errori PowerShell:
    TYPE "%PS_ERR_FILE%" >>"%LOG_FILE%"
  )
  DEL /F /Q "%PS_ERR_FILE%" >NUL 2>NUL
)

>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] PowerShell terminato con codice %EC%.
ECHO.
IF "%EC%"=="0" (
  ECHO Setup completato.
) ELSE (
  ECHO Setup terminato con errore. Controlla il log:
  ECHO %LOG_FILE%
)
ECHO.
ECHO Premi un tasto per chiudere...
PAUSE >NUL
EXIT /B %EC%

:PS1_MISSING
ECHO ERRORE: Setup-RiPlay.ps1 non trovato nella stessa cartella.
>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] ERRORE: Setup-RiPlay.ps1 non trovato.
ECHO.
ECHO Controlla il log:
ECHO %LOG_FILE%
ECHO.
PAUSE
EXIT /B 1

:ELEVATION_FAILED
ECHO ERRORE: autorizzazione amministratore non concessa o PowerShell non avviato.
>>"%LOG_FILE%" ECHO [%DATE% %TIME%] [CMD] ERRORE: elevazione amministratore fallita.
ECHO.
ECHO Controlla il log:
ECHO %LOG_FILE%
ECHO.
PAUSE
EXIT /B 1
