@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" Server %*
set "serverExit=%ERRORLEVEL%"
if not "%serverExit%"=="0" pause
endlocal & exit /b %serverExit%
