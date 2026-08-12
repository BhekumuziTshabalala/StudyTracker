# IU Study Tracker - Android APK Build Generator (PowerShell)
$ErrorActionPreference = "Stop"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "         IU Study Tracker - Android APK Build Generator" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "[1/3] Checking environment..." -ForegroundColor Yellow
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Error "gradlew.bat not found in $ScriptDir"
}

Write-Host "[2/3] Building Debug APK with Gradle..." -ForegroundColor Yellow
& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "======================================================================" -ForegroundColor Red
    Write-Host "[BUILD FAILED]" -ForegroundColor Red
    Write-Host "If this failed due to missing Android SDK, please:" -ForegroundColor Yellow
    Write-Host "1. Open this folder in Android Studio to automatically configure SDK." -ForegroundColor White
    Write-Host "2. Or set ANDROID_HOME environment variable to your Android SDK folder." -ForegroundColor White
    Write-Host "======================================================================" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "[3/3] Exporting APK to output directory..." -ForegroundColor Yellow
$OutputDir = Join-Path $ScriptDir "output"
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$ApkSrc = Join-Path $ScriptDir "app\build\outputs\apk\debug\app-debug.apk"
$ApkDest = Join-Path $OutputDir "StudyTracker-debug.apk"

if (Test-Path $ApkSrc) {
    Copy-Item -Path $ApkSrc -Destination $ApkDest -Force
    Write-Host ""
    Write-Host "======================================================================" -ForegroundColor Green
    Write-Host "[SUCCESS] APK Generated Successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "File Location:" -ForegroundColor White
    Write-Host "  $ApkDest" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "How to install on your Phone or Tablet:" -ForegroundColor Yellow
    Write-Host "  Option A: Connect device via USB (with USB debugging ON) and run:" -ForegroundColor White
    Write-Host "            adb install -r output\StudyTracker-debug.apk" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  Option B: Transfer StudyTracker-debug.apk to your Android device" -ForegroundColor White
    Write-Host "            (via USB cable, Google Drive, or email) and tap to install." -ForegroundColor Gray
    Write-Host "======================================================================" -ForegroundColor Green
} else {
    Write-Warning "Build completed but APK was not found at $ApkSrc."
}
