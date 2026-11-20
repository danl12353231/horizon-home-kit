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
rem -- undo install-horizon.bat: original launcher, Home button and helper --
"%ADB%" -s %IP% shell pm enable com.google.android.tvlauncher >nul 2>&1
"%ADB%" -s %IP% shell pm enable com.google.android.apps.tv.launcherx >nul 2>&1
set /p H=<"%B%\home.txt"
if defined H "%ADB%" -s %IP% shell cmd package set-home-activity %H%
set /p S=<"%B%\a11y-services.txt"
set /p E=<"%B%\a11y-enabled.txt"
if "%S%"=="null" ( "%ADB%" -s %IP% shell settings delete secure enabled_accessibility_services ) else ( "%ADB%" -s %IP% shell settings put secure enabled_accessibility_services "%S%" )
if "%E%"=="null" ( "%ADB%" -s %IP% shell settings delete secure accessibility_enabled ) else ( "%ADB%" -s %IP% shell settings put secure accessibility_enabled %E% )
"%ADB%" -s %IP% shell input keyevent KEYCODE_HOME
echo Back on the original home screen. Horizon is still installed; run use-horizon-home.bat to switch back.
pause
