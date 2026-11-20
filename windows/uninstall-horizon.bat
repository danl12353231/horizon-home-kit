@echo off
cd /d "%~dp0"
call use-original-home.bat
call config.bat
"%~dp0platform-tools\adb.exe" -s %IP% uninstall com.horizon.home
echo Horizon Home removed.
pause
