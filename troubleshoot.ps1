#!/usr/bin/env pwsh
# Quick Troubleshoot - Cepat cek masalah

$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "🔍 QUICK TROUBLESHOOT - Cosmic Kiosk" -ForegroundColor Cyan
Write-Host "================================================"
Write-Host ""

# 1. Device
Write-Host "1️⃣  Device Status:" -ForegroundColor Yellow
$devices = & $adb devices 2>&1
if ($devices -match "device$") {
    Write-Host "   ✅ Device connected" -ForegroundColor Green
} else {
    Write-Host "   ❌ No device! Run: adb devices" -ForegroundColor Red
    exit 1
}

# 2. App installed
Write-Host ""
Write-Host "2️⃣  App Installation:" -ForegroundColor Yellow
$installed = & $adb shell pm list packages 2>&1 | Select-String "cosmic"
if ($installed) {
    Write-Host "   ✅ App installed" -ForegroundColor Green
} else {
    Write-Host "   ❌ Not installed! Run: .\install.ps1" -ForegroundColor Red
    exit 1
}

# 3. App running
Write-Host ""
Write-Host "3️⃣  App Running Status:" -ForegroundColor Yellow
$running = & $adb shell ps 2>&1 | Select-String "cosmic"
if ($running) {
    Write-Host "   ✅ App is running" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  App NOT running. Launching now..." -ForegroundColor Yellow
    & $adb shell am force-stop com.kiosktouchscreendpr.cosmic 2>&1 | Out-Null
    & $adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity 2>&1
    Write-Host "   ✅ App launched!" -ForegroundColor Green
}

# 4. Recent errors
Write-Host ""
Write-Host "4️⃣  Recent Errors:" -ForegroundColor Yellow
$errors = & $adb logcat -d *:E 2>&1 | Select-String "cosmic|kiosk" -CaseSensitive:$false | Select-Object -Last 5
if ($errors) {
    Write-Host "   ⚠️  Found errors:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host "      $_" -ForegroundColor Red }
} else {
    Write-Host "   ✅ No recent errors" -ForegroundColor Green
}

# 5. Recommendations
Write-Host ""
Write-Host "================================================"
Write-Host "📱 NEXT STEPS:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Check device screen - app should be fullscreen"
Write-Host "2. If blank, run: .\launch-app.ps1"
Write-Host "3. View live logs: .\debug-live.ps1"
Write-Host "4. Check errors: .\check-errors.ps1"
Write-Host ""
Write-Host "Device should show:" -ForegroundColor Yellow
Write-Host "  ✅ Full-screen dashboard"
Write-Host "  ✅ Loading from: https://kiosk.mugshot.dev"
Write-Host "  ✅ No home/navigation buttons visible"
Write-Host ""
