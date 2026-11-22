@echo off
call "%~dp0Start-Server.cmd" -RememberGemini %*
exit /b %ERRORLEVEL%
