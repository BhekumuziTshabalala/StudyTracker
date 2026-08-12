@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo          IU Study Tracker - Android APK Build Generator
echo ======================================================================
echo.

cd /d "%~dp0"

echo [1/3] Checking environment...
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found in "%~dp0".
    pause
    exit /b 1
)

echo [2/3] Building Debug APK with Gradle...
echo Running: gradlew.bat assembleDebug
call .\gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ======================================================================
    echo [BUILD FAILED]
    echo.
    echo If this failed due to missing Android SDK, please:
    echo 1. Open this folder in Android Studio to automatically configure SDK.
    echo 2. Or set ANDROID_HOME environment variable to your Android SDK folder.
    echo ======================================================================
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [3/3] Exporting APK to output directory...
if not exist "output" mkdir "output"

set APK_SRC=app\build\outputs\apk\debug\app-debug.apk
set APK_DEST=output\StudyTracker-debug.apk

if exist "%APK_SRC%" (
    copy /y "%APK_SRC%" "%APK_DEST%" >nul
    echo.
    echo ======================================================================
    echo [SUCCESS] APK Generated Successfully!
    echo.
    echo File Location:
    echo %~dp0output\StudyTracker-debug.apk
    echo.
    echo How to install on your Phone or Tablet:
    echo  Option A: Connect device via USB (with USB debugging ON) and run:
    echo            adb install -r output\StudyTracker-debug.apk
    echo.
    echo  Option B: Transfer StudyTracker-debug.apk to your Android device
    echo            (via USB cable, Google Drive, or email) and tap to install.
    echo ======================================================================
) else (
    echo [WARNING] Build completed but APK was not found at %APK_SRC%.
)

echo.
pause
