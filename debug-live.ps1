# Debug Cosmic Kiosk on Device
# Comprehensive debugging and app launcher

$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   COSMIC KIOSK - LIVE DEBUGGING & LAUNCHER                    " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check ADB
Write-Host "[1/6] Checking ADB..." -ForegroundColor Yellow
if (Test-Path $adb) {
    Write-Host "   ✅ ADB found" -ForegroundColor Green
} else {
    Write-Host "   ❌ ADB not found at: $adb" -ForegroundColor Red
    exit 1
}

# Step 2: Check devices
Write-Host ""
Write-Host "[2/6] Checking connected devices..." -ForegroundColor Yellow
$devicesList = & $adb devices 2>&1 | Out-String
Write-Host $devicesList
$devices = & $adb devices | Select-String "device$" | Where-Object { $_ -notmatch "List of devices" }
if ($devices) {
    Write-Host "   ✅ Device(s) connected" -ForegroundColor Green
} else {
    Write-Host "   ❌ No devices connected!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Solutions:" -ForegroundColor Yellow
    Write-Host "  - Check WiFi connection" -ForegroundColor White
    Write-Host "  - Run: adb connect <device-ip>:5555" -ForegroundColor White
    Write-Host "  - For USB: Enable USB Debugging on device" -ForegroundColor White
    exit 1
}

# Step 3: Check if app is installed
Write-Host ""
Write-Host "[3/6] Checking if Cosmic Kiosk is installed..." -ForegroundColor Yellow
$installed = & $adb shell pm list packages 2>&1 | Select-String "com.kiosktouchscreendpr.cosmic"
if ($installed) {
    Write-Host "   ✅ App is installed: $installed" -ForegroundColor Green
} else {
    Write-Host "   ❌ App not installed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Install first:" -ForegroundColor Yellow
    Write-Host "  .\install.ps1" -ForegroundColor White
    exit 1
}

# Step 4: Check app info
Write-Host ""
Write-Host "[4/6] Getting app info..." -ForegroundColor Yellow
$appInfo = & $adb shell dumpsys package com.kiosktouchscreendpr.cosmic 2>&1 | Select-String "versionName|versionCode|firstInstallTime" | Select-Object -First 3
$appInfo | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }

# Step 5: Force stop then start app
Write-Host ""
Write-Host "[5/6] Launching Cosmic Kiosk app..." -ForegroundColor Yellow
Write-Host "   Stopping any running instance..." -ForegroundColor Gray
& $adb shell am force-stop com.kiosktouchscreendpr.cosmic 2>&1 | Out-Null

Start-Sleep -Milliseconds 500

Write-Host "   Starting app on device..." -ForegroundColor Gray
$launchResult = & $adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity 2>&1
if ($launchResult -like "*Error*") {
    Write-Host "   ❌ Failed to launch: $launchResult" -ForegroundColor Red
} else {
    Write-Host "   ✅ App launched successfully!" -ForegroundColor Green
    Write-Host "   $launchResult" -ForegroundColor Gray
}

# Step 6: Start watching logs
Write-Host ""
Write-Host "[6/6] Starting live log viewer..." -ForegroundColor Yellow
Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   LIVE LOGS (Press Ctrl+C to stop)                            " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Watching for:" -ForegroundColor Gray
Write-Host "  - App startup" -ForegroundColor White
Write-Host "  - WebView loading" -ForegroundColor White
Write-Host "  - WebSocket connection" -ForegroundColor White
Write-Host "  - Errors and crashes" -ForegroundColor White
Write-Host ""
Write-Host "----------------------------------------------------------------" -ForegroundColor Cyan
Write-Host ""

# Clear old logs and start watching
& $adb logcat -c 2>&1 | Out-Null
Start-Sleep -Milliseconds 200

# Watch logs with filters
& $adb logcat -v time `
    "*:E" `
    "MainActivity:D" `
    "HomeViewModel:D" `
    "WebView:D" `
    "WebSocket:*" `
    "chromium:W" `
    "AndroidRuntime:E" `
    "cosmic:*" `
    2>&1 | ForEach-Object {
        $line = $_.ToString()

        # Color coding for better visibility
        if ($line -match "ERROR|FATAL|crash|exception" -and $line -notmatch "No pending exception") {
            Write-Host $line -ForegroundColor Red
        }
        elseif ($line -match "WebSocket.*Connect|Connected|Success") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "WebView|Loading|Loaded") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "MainActivity|Starting|onCreate") {
            Write-Host $line -ForegroundColor Yellow
        }
        elseif ($line -match "WARNING|WARN") {
            Write-Host $line -ForegroundColor DarkYellow
        }
        else {
            Write-Host $line
        }
    }
