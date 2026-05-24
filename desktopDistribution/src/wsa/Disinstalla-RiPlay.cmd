@ECHO OFF
SETLOCAL EnableExtensions
CHCP 65001 >NUL
TITLE Uninstall RiPlay - WSA - v12
CD /D "%~dp0"

SET "PS1_FILE=%~dp0Disinstalla-RiPlay.ps1"
SET "UNINSTALL_SELF=%~f0"
SET "UNINSTALL_DIR=%~dp0"

IF NOT EXIST "%PS1_FILE%" GOTO PS1_MISSING

NET SESSION >NUL 2>NUL
IF "%ERRORLEVEL%"=="0" GOTO RUN_PS1

ECHO Richiesta autorizzazione amministratore...
POWERSHELL.EXE -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$p=$env:UNINSTALL_SELF; $w=$env:UNINSTALL_DIR; Start-Process -FilePath $p -ArgumentList '--elevated' -WorkingDirectory $w -Verb RunAs"
EXIT /B 0

:RUN_PS1
POWERSHELL.EXE -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%PS1_FILE%"
ECHO.
ECHO Premi un tasto per chiudere...
PAUSE >NUL
EXIT /B %ERRORLEVEL%

:PS1_MISSING
ECHO ERRORE: Disinstalla-RiPlay.ps1 non trovato nella stessa cartella.
ECHO.
PAUSE
EXIT /B 1
