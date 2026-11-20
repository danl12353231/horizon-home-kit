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
echo Installing Horizon Home...
if not exist HorizonHome.apk ( echo Put HorizonHome.apk next to this file first. & pause & exit /b 1 )
"%ADB%" -s %IP% install -r -g HorizonHome.apk || ( pause & exit /b 1 )
set "P=com.horizon.home"
"%ADB%" -s %IP% shell pm grant %P% android.permission.READ_EXTERNAL_STORAGE >nul 2>&1
"%ADB%" -s %IP% shell pm grant %P% com.android.providers.tv.permission.READ_EPG_DATA >nul 2>&1
"%ADB%" -s %IP% shell pm grant %P% android.permission.READ_TV_LISTINGS >nul 2>&1
"%ADB%" -s %IP% shell pm grant %P% android.permission.DUMP >nul 2>&1
"%ADB%" -s %IP% shell appops set %P% GET_USAGE_STATS allow >nul 2>&1
rem -- remember what the Home button and home screen were before (only the first time) --
if not exist "%B%\home.txt" "%ADB%" -s %IP% shell "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1" > "%B%\home.txt"
if not exist "%B%\a11y-services.txt" "%ADB%" -s %IP% shell settings get secure enabled_accessibility_services > "%B%\a11y-services.txt"
if not exist "%B%\a11y-enabled.txt" "%ADB%" -s %IP% shell settings get secure accessibility_enabled > "%B%\a11y-enabled.txt"
rem -- make Horizon the home screen; a tiny helper sends the remote's Home button to it --
"%ADB%" -s %IP% shell cmd package set-home-activity %P%/.HomeActivity
"%ADB%" -s %IP% shell settings put secure enabled_accessibility_services %P%/%P%.HomeButtonService
"%ADB%" -s %IP% shell settings put secure accessibility_enabled 1
rem -- hand over your settings (kept only on the projector) --
set "X="
if not "%NAME%"=="" set X=%X% --es name "%NAME%"
if not "%TMDB_KEY%"=="" set X=%X% --es tmdb "%TMDB_KEY%"
if not "%LIVETV%"=="" set X=%X% --es livetv "%LIVETV%"
if not "%NEXTDNS_ID%"=="" set X=%X% --es nextdns_id "%NEXTDNS_ID%"
if not "%NEXTDNS_KEY%"=="" set X=%X% --es nextdns_key "%NEXTDNS_KEY%"
"%ADB%" -s %IP% shell am start -n %P%/.HomeActivity %X% >nul
echo Done. Press Home on the remote.
echo To go back to the original home screen any time, run use-original-home.bat.
pause
