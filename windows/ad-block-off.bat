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
set /p MODE=<"%B%\dns-mode.txt"
set /p HOST=<"%B%\dns-host.txt"
if "%MODE%"=="null" ( "%ADB%" -s %IP% shell settings delete global private_dns_mode ) else ( "%ADB%" -s %IP% shell settings put global private_dns_mode %MODE% )
if "%HOST%"=="null" ( "%ADB%" -s %IP% shell settings delete global private_dns_specifier ) else ( "%ADB%" -s %IP% shell settings put global private_dns_specifier %HOST% )
echo DNS is back to how it was.
pause
