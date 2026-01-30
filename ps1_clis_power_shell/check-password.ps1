# Check Password - Verify APP_PASSWORD in env.properties

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   PASSWORD CHECKER - Cosmic Kiosk                             " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Read current password from env.properties
if (Test-Path "env.properties") {
    $envContent = Get-Content "env.properties" | Where-Object { $_ -match "APP_PASSWORD=" }
    if ($envContent) {
        $password = ($envContent -split "=")[1].Trim()
        Write-Host "Current password in env.properties:" -ForegroundColor Yellow
        Write-Host "  APP_PASSWORD = $password" -ForegroundColor White -BackgroundColor DarkGreen
        Write-Host ""

        Write-Host "This password will be used when you rebuild the APK." -ForegroundColor Gray
        Write-Host ""
    } else {
        Write-Host "⚠️  APP_PASSWORD not found in env.properties!" -ForegroundColor Red
    }
} else {
    Write-Host "❌ env.properties not found!" -ForegroundColor Red
    exit 1
}

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "APK Status:" -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apk = Get-Item $apkPath
    Write-Host "  ✅ APK exists" -ForegroundColor Green
    Write-Host "  Location: $($apk.FullName)" -ForegroundColor Gray
    Write-Host "  Size: $([math]::Round($apk.Length/1MB, 2)) MB" -ForegroundColor Gray
    Write-Host "  Modified: $($apk.LastWriteTime)" -ForegroundColor Gray
    Write-Host ""

    # Check if it's recently built
    $timeDiff = (Get-Date) - $apk.LastWriteTime
    if ($timeDiff.TotalMinutes -lt 10) {
        Write-Host "  ✅ APK is recent (built $([math]::Round($timeDiff.TotalMinutes, 1)) minutes ago)" -ForegroundColor Green
        Write-Host "  This APK should have password: $password" -ForegroundColor White
    } else {
        Write-Host "  ⚠️  APK is old (built $([math]::Round($timeDiff.TotalHours, 1)) hours ago)" -ForegroundColor Yellow
        Write-Host "  May have different password - consider rebuilding" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ No APK found - need to build" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "💡 TIPS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "If password is wrong on device:" -ForegroundColor White
Write-Host "  1. The installed APK has different password" -ForegroundColor Gray
Write-Host "  2. Need to rebuild with current env.properties" -ForegroundColor Gray
Write-Host "  3. Then reinstall: .\install.ps1" -ForegroundColor Gray
Write-Host ""

Write-Host "To change password:" -ForegroundColor White
Write-Host "  1. Edit env.properties" -ForegroundColor Gray
Write-Host "  2. Change APP_PASSWORD=260224 to new value" -ForegroundColor Gray
Write-Host "  3. Rebuild: .\gradlew.bat assembleDebug --no-daemon" -ForegroundColor Gray
Write-Host "  4. Reinstall: .\install.ps1" -ForegroundColor Gray
Write-Host ""

Write-Host "To rebuild NOW:" -ForegroundColor White
Write-Host "  .\gradlew.bat assembleDebug --no-daemon --no-build-cache" -ForegroundColor Cyan
Write-Host ""

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Current password to try: $password" -ForegroundColor Green -BackgroundColor Black
Write-Host ""
