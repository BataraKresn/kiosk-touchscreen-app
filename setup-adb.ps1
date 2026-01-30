# ADB Setup Script for Cosmic Kiosk
# This script adds Android SDK Platform Tools (ADB) to your PATH

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   ADB Setup for Cosmic Kiosk Android App      " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$adbPath = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools"

# Check if ADB exists
if (Test-Path "$adbPath\adb.exe") {
    Write-Host "✅ ADB found at: $adbPath" -ForegroundColor Green

    # Add to current session PATH
    $env:Path = "$adbPath;$env:Path"
    Write-Host "✅ ADB added to current session PATH" -ForegroundColor Green

    # Add to User PATH permanently
    $currentUserPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
    if ($currentUserPath -notlike "*$adbPath*") {
        [Environment]::SetEnvironmentVariable("Path", "$currentUserPath;$adbPath", [EnvironmentVariableTarget]::User)
        Write-Host "✅ ADB added to User PATH permanently" -ForegroundColor Green
        Write-Host "   (Restart PowerShell for permanent effect)" -ForegroundColor Yellow
    } else {
        Write-Host "✅ ADB already in User PATH" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "Testing ADB..." -ForegroundColor Cyan

    # Test ADB
    & "$adbPath\adb.exe" version | Out-String | Write-Host

    Write-Host ""
    Write-Host "Checking for connected devices..." -ForegroundColor Cyan
    & "$adbPath\adb.exe" devices

    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "ADB is ready! You can now use these commands:" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Install APK:" -ForegroundColor Yellow
    Write-Host '  & "$adbPath\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk' -ForegroundColor White
    Write-Host ""
    Write-Host "View logs:" -ForegroundColor Yellow
    Write-Host '  & "$adbPath\adb.exe" logcat | Select-String "cosmic"' -ForegroundColor White
    Write-Host ""
    Write-Host "List devices:" -ForegroundColor Yellow
    Write-Host '  & "$adbPath\adb.exe" devices' -ForegroundColor White
    Write-Host ""
    Write-Host "Or restart PowerShell and use 'adb' directly" -ForegroundColor Cyan
    Write-Host ""

} else {
    Write-Host "❌ ADB not found at: $adbPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Android Studio or Android SDK Platform Tools" -ForegroundColor Yellow
    Write-Host "Download from: https://developer.android.com/studio/releases/platform-tools" -ForegroundColor Cyan
}
