# Install Cosmic Kiosk APK
# Quick installation script

$adbPath = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   Installing Cosmic Kiosk on Android Device   " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Check if APK exists
if (!(Test-Path $apkPath)) {
    Write-Host "❌ APK not found at: $apkPath" -ForegroundColor Red
    Write-Host "Run: .\gradlew.bat assembleDebug" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ APK found: $apkPath" -ForegroundColor Green
Write-Host "   Size: $([math]::Round((Get-Item $apkPath).Length/1MB, 2)) MB" -ForegroundColor Gray
Write-Host ""

# Check devices
Write-Host "Checking connected devices..." -ForegroundColor Cyan
$devices = & $adbPath devices | Select-String "device$"
if ($devices) {
    Write-Host "✅ Device(s) connected:" -ForegroundColor Green
    & $adbPath devices | Select-String "device" | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
    Write-Host ""
} else {
    Write-Host "❌ No devices connected!" -ForegroundColor Red
    Write-Host ""
    Write-Host "For USB device:" -ForegroundColor Yellow
    Write-Host "  1. Enable USB Debugging on phone" -ForegroundColor White
    Write-Host "  2. Connect via USB cable" -ForegroundColor White
    Write-Host "  3. Trust this computer on device" -ForegroundColor White
    Write-Host ""
    Write-Host "For WiFi device (already paired):" -ForegroundColor Yellow
    Write-Host "  Run: adb connect <device-ip>:5555" -ForegroundColor White
    Write-Host ""
    exit 1
}

# Install APK
Write-Host "Installing APK..." -ForegroundColor Cyan
Write-Host "This may take 30-60 seconds..." -ForegroundColor Gray
Write-Host ""

$result = & $adbPath install -r $apkPath 2>&1
$success = $false

foreach ($line in $result) {
    if ($line -like "*Success*") {
        $success = $true
    }
    Write-Host $line
}

Write-Host ""
if ($success) {
    Write-Host "================================================" -ForegroundColor Green
    Write-Host "   ✅ INSTALLATION SUCCESSFUL!                  " -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "The Cosmic Kiosk app has been installed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. App should launch automatically on device" -ForegroundColor White
    Write-Host "2. If not, find 'Cosmic' app in app drawer" -ForegroundColor White
    Write-Host "3. App will load in full-screen kiosk mode" -ForegroundColor White
    Write-Host ""
    Write-Host "View logs:" -ForegroundColor Yellow
    Write-Host '  & "' + $adbPath + '" logcat | Select-String "cosmic"' -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "================================================" -ForegroundColor Red
    Write-Host "   ❌ INSTALLATION FAILED                       " -ForegroundColor Red
    Write-Host "================================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "- Device storage full" -ForegroundColor White
    Write-Host "- USB debugging not enabled" -ForegroundColor White
    Write-Host "- Previous version needs uninstall first" -ForegroundColor White
    Write-Host ""
    Write-Host "Try:" -ForegroundColor Cyan
    Write-Host '  & "' + $adbPath + '" uninstall com.kiosktouchscreendpr.cosmic' -ForegroundColor White
    Write-Host "  Then run this script again" -ForegroundColor White
    Write-Host ""
}
