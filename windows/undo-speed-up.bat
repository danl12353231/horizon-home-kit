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
set /p W=<"%B%\anim-window.txt"
set /p T=<"%B%\anim-transition.txt"
set /p N=<"%B%\anim-animator.txt"
if "%W%"=="null" ("%ADB%" -s %IP% shell settings delete global window_animation_scale) else ("%ADB%" -s %IP% shell settings put global window_animation_scale %W%)
if "%T%"=="null" ("%ADB%" -s %IP% shell settings delete global transition_animation_scale) else ("%ADB%" -s %IP% shell settings put global transition_animation_scale %T%)
if "%N%"=="null" ("%ADB%" -s %IP% shell settings delete global animator_duration_scale) else ("%ADB%" -s %IP% shell settings put global animator_duration_scale %N%)
"%ADB%" -s %IP% shell pm enable com.google.android.tvrecommendations >nul 2>&1
echo Back to how it was.
pause
