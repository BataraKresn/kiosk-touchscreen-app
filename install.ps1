# Quick Install Command for Cosmic Kiosk
# Save typing - just run: .\install.ps1

Write-Host ""
Write-Host "Installing Cosmic Kiosk APK..." -ForegroundColor Cyan
Write-Host ""

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

if (Test-Path $apkPath) {
    Write-Host "✅ APK found: $apkPath" -ForegroundColor Green
    Write-Host "   Size: $([math]::Round((Get-Item $apkPath).Length/1MB, 2)) MB"
    Write-Host ""
    Write-Host "Installing on device..." -ForegroundColor Yellow

    adb install -r $apkPath

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Installation successful!" -ForegroundColor Green
        Write-Host ""
        Write-Host "View logs: .\view-logs.ps1" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "❌ Installation failed" -ForegroundColor Red
        Write-Host "Try: adb uninstall com.kiosktouchscreendpr.cosmic" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ APK not found at: $apkPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Build first with:" -ForegroundColor Yellow
    Write-Host "  .\gradlew.bat assembleDebug" -ForegroundColor White
}
