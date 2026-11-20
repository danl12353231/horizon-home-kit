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
rem -- shorter animations and no Google TV recommendation service (backed up first) --
if not exist "%B%\anim-window.txt" "%ADB%" -s %IP% shell settings get global window_animation_scale > "%B%\anim-window.txt"
if not exist "%B%\anim-transition.txt" "%ADB%" -s %IP% shell settings get global transition_animation_scale > "%B%\anim-transition.txt"
if not exist "%B%\anim-animator.txt" "%ADB%" -s %IP% shell settings get global animator_duration_scale > "%B%\anim-animator.txt"
"%ADB%" -s %IP% shell settings put global window_animation_scale 0.5
"%ADB%" -s %IP% shell settings put global transition_animation_scale 0.5
"%ADB%" -s %IP% shell settings put global animator_duration_scale 0.5
"%ADB%" -s %IP% shell pm disable-user --user 0 com.google.android.tvrecommendations >nul 2>&1
echo Faster. Undo: undo-speed-up.bat
pause
