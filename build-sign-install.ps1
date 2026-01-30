#!/usr/bin/env pwsh
# Build and automatically sign APK

param(
    [switch]$Clean,
    [switch]$Install
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Build & Sign Cosmic Kiosk APK" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build APK
if ($Clean) {
    Write-Host "Cleaning previous build..." -ForegroundColor Yellow
    .\gradlew.bat clean
}

Write-Host "Building release APK..." -ForegroundColor Yellow
$buildResult = .\gradlew.bat assembleRelease 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    Write-Host $buildResult
    exit 1
}

Write-Host "Build successful!" -ForegroundColor Green
Write-Host ""

# Step 2: Check if APK exists
$apkPath = ".\app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "APK not found at: $apkPath" -ForegroundColor Red
    exit 1
}

# Step 3: Verify signing
Write-Host "Verifying APK signature..." -ForegroundColor Yellow
$buildToolsPath = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1

if ($null -eq $buildToolsPath) {
    Write-Host "Android SDK build-tools not found. Skipping verification." -ForegroundColor Yellow
} else {
    $apksigner = Join-Path $buildToolsPath.FullName "apksigner.bat"
    
    if (Test-Path $apksigner) {
        $verifyResult = & $apksigner verify --verbose $apkPath 2>&1
        
        if ($verifyResult -match "Verified using v2 scheme.*: true") {
            Write-Host "APK is properly signed!" -ForegroundColor Green
        } else {
            Write-Host "APK not signed. Signing now..." -ForegroundColor Yellow
            
            $keystore = "$env:USERPROFILE\.android\debug.keystore"
            if (-not (Test-Path $keystore)) {
                Write-Host "Debug keystore not found at: $keystore" -ForegroundColor Red
                exit 1
            }
            
            & $apksigner sign --ks $keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android $apkPath
            
            $verifyResult2 = & $apksigner verify --verbose $apkPath 2>&1
            if ($verifyResult2 -match "Verified using v2 scheme.*: true") {
                Write-Host "APK signed successfully!" -ForegroundColor Green
            } else {
                Write-Host "Failed to sign APK!" -ForegroundColor Red
                exit 1
            }
        }
    }
}

Write-Host ""

# Step 4: Show APK info
$apkSize = (Get-Item $apkPath).Length / 1MB
Write-Host "APK Information:" -ForegroundColor Cyan
Write-Host "  Path: $apkPath" -ForegroundColor White
Write-Host "  Size: $([math]::Round($apkSize, 2)) MB" -ForegroundColor White
Write-Host ""

# Step 5: Install if requested
if ($Install) {
    Write-Host "Installing to device..." -ForegroundColor Yellow
    
    $adbPath = "adb"
    if (-not (Get-Command $adbPath -ErrorAction SilentlyContinue)) {
        $localSdk = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
        if (Test-Path $localSdk) {
            $adbPath = $localSdk
        } else {
            Write-Host "ADB not found!" -ForegroundColor Red
            exit 0
        }
    }
    
    $devices = & $adbPath devices
    if ($devices -notmatch "device$") {
        Write-Host "No device connected!" -ForegroundColor Red
        exit 0
    }
    
    $packageName = "com.kiosktouchscreendpr.cosmic"
    $installed = & $adbPath shell pm list packages | Select-String $packageName
    if ($installed) {
        Write-Host "Uninstalling old version..." -ForegroundColor Yellow
        & $adbPath uninstall $packageName | Out-Null
        Start-Sleep -Seconds 2
    }
    
    Write-Host "Installing APK..." -ForegroundColor Yellow
    $installOutput = & $adbPath install $apkPath 2>&1
    
    if ($LASTEXITCODE -eq 0 -and $installOutput -match "Success") {
        Write-Host "APK installed successfully!" -ForegroundColor Green
        Write-Host "Launching app..." -ForegroundColor Yellow
        & $adbPath shell am start -n "$packageName/.MainActivity"
    } else {
        Write-Host "Installation failed!" -ForegroundColor Red
        Write-Host $installOutput
    }
}

Write-Host ""
Write-Host "Process Complete!" -ForegroundColor Green
Write-Host "APK ready at: $apkPath" -ForegroundColor Green

if (-not $Install) {
    Write-Host ""
    Write-Host "To install, run: .\build-and-sign.ps1 -Install" -ForegroundColor Yellow
}
