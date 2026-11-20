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
if "%NEXTDNS_ID%"=="" ( echo Set NEXTDNS_ID in config.bat first. & pause & exit /b 1 )
if not exist "%B%\dns-mode.txt" "%ADB%" -s %IP% shell settings get global private_dns_mode > "%B%\dns-mode.txt"
if not exist "%B%\dns-host.txt" "%ADB%" -s %IP% shell settings get global private_dns_specifier > "%B%\dns-host.txt"
"%ADB%" -s %IP% shell settings put global private_dns_specifier %NEXTDNS_ID%.dns.nextdns.io
"%ADB%" -s %IP% shell settings put global private_dns_mode hostname
echo Ad and tracker blocking is on (NextDNS, encrypted DNS). Undo: ad-block-off.bat
pause
