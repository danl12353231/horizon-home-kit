@echo off
setlocal
cd /d "%~dp0"
if not exist config.bat ( echo config.bat is missing. Copy config.example.bat to config.bat and fill it in. & pause & exit /b 1 )
call config.bat
set "ADB=%~dp0platform-tools\adb.exe"
if not exist "%ADB%" ( echo ADB is missing. Run get-adb.bat first. & pause & exit /b 1 )
set "B=%~dp0backup"
if not exist "%B%" mkdir "%B%"
"%ADB%" connect %IP% >nul 2>&1
"%ADB%" -s %IP% get-state >nul 2>&1 || ( echo Can't reach the projector at %IP%. Is it on, on the same Wi-Fi, with network debugging allowed? Try find-projector.bat. & pause & exit /b 1 )
rem -- XGIMI only: make the remote's power button shut down fully (logo on every start) --
if not exist "%B%\power-key.txt" "%ADB%" -s %IP% shell settings get system power_key_definition > "%B%\power-key.txt"
"%ADB%" -s %IP% shell settings put system power_key_definition 1
echo Power button now shuts down fully. Undo: xgimi-power-standby.bat
pause
