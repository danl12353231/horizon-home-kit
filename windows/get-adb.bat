@echo off
cd /d "%~dp0"
if exist platform-tools\adb.exe ( echo ADB is already here. & timeout /t 3 >nul & exit /b 0 )
echo Downloading Google's Android platform-tools (ADB)...
powershell -NoProfile -Command "Invoke-WebRequest https://dl.google.com/android/repository/platform-tools-latest-windows.zip -OutFile platform-tools.zip; Expand-Archive platform-tools.zip -DestinationPath . -Force; Remove-Item platform-tools.zip"
if exist platform-tools\adb.exe ( echo Done. ) else ( echo Download failed. )
timeout /t 5 >nul
