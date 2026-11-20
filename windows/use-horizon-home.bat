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
"%ADB%" -s %IP% shell cmd package set-home-activity com.horizon.home/.HomeActivity
"%ADB%" -s %IP% shell settings put secure enabled_accessibility_services com.horizon.home/com.horizon.home.HomeButtonService
"%ADB%" -s %IP% shell settings put secure accessibility_enabled 1
"%ADB%" -s %IP% shell input keyevent KEYCODE_HOME
echo Horizon Home is the home screen again.
pause
