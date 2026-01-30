#!/usr/bin/env pwsh
# Install APK to connected Android device with proper error handling

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Installing Cosmic Kiosk APK" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if ADB is available
$adbPath = "adb"
if (-not (Get-Command $adbPath -ErrorAction SilentlyContinue)) {
    $localSdk = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $localSdk) {
        $adbPath = $localSdk
    } else {
        Write-Host "❌ ADB not found. Please install Android SDK Platform Tools." -ForegroundColor Red
        exit 1
    }
}

# Check if device is connected
Write-Host "🔍 Checking for connected devices..." -ForegroundColor Yellow
$devices = & $adbPath devices
if ($devices -match "device$") {
    Write-Host "✅ Device connected" -ForegroundColor Green
} else {
    Write-Host "❌ No device connected. Please connect your device via USB or WiFi ADB." -ForegroundColor Red
    Write-Host ""
    Write-Host "Available devices:" -ForegroundColor Yellow
    & $adbPath devices
    exit 1
}

# APK path
$apkPath = ".\app\build\outputs\apk\release\app-release.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK not found at: $apkPath" -ForegroundColor Red
    Write-Host "Please build the APK first using: .\gradlew.bat assembleRelease" -ForegroundColor Yellow
    exit 1
}

# Get APK info
$apkSize = (Get-Item $apkPath).Length / 1MB
Write-Host "📦 APK Size: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Cyan
Write-Host ""

# Check if app is already installed
Write-Host "🔍 Checking if app is already installed..." -ForegroundColor Yellow
$packageName = "com.kiosktouchscreendpr.cosmic"
$installed = & $adbPath shell pm list packages | Select-String $packageName

if ($installed) {
    Write-Host "⚠️  App already installed. Uninstalling old version..." -ForegroundColor Yellow
    & $adbPath uninstall $packageName
    Start-Sleep -Seconds 2
}

# Install APK
Write-Host "📲 Installing APK..." -ForegroundColor Yellow
$installOutput = & $adbPath install -r $apkPath 2>&1

if ($LASTEXITCODE -eq 0 -and $installOutput -match "Success") {
    Write-Host ""
    Write-Host "✅ APK installed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🚀 Launching app..." -ForegroundColor Yellow
    & $adbPath shell am start -n "$packageName/.MainActivity"
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Installation Complete!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "❌ Installation failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Error details:" -ForegroundColor Yellow
    Write-Host $installOutput
    Write-Host ""
    Write-Host "Common solutions:" -ForegroundColor Yellow
    Write-Host "1. Enable 'Install from unknown sources' on your device" -ForegroundColor White
    Write-Host "2. Uninstall the old version manually if exists" -ForegroundColor White
    Write-Host "3. Check if device storage is full" -ForegroundColor White
    Write-Host "4. Try: adb uninstall $packageName" -ForegroundColor White
    Write-Host ""
    exit 1
}
