@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" Client %*
set "clientExit=%ERRORLEVEL%"
if not "%clientExit%"=="0" pause
endlocal & exit /b %clientExit%
