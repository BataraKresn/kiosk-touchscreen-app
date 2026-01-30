# Manual Check & Launch Script
# Quick commands to check and launch app

$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "=== DEVICE STATUS ===" -ForegroundColor Cyan
& $adb devices

Write-Host ""
Write-Host "=== CHECK IF APP INSTALLED ===" -ForegroundColor Cyan
$installed = & $adb shell pm list packages | Select-String "cosmic"
if ($installed) {
    Write-Host "✅ Installed: $installed" -ForegroundColor Green
} else {
    Write-Host "❌ Not installed" -ForegroundColor Red
    Write-Host "Run: .\install.ps1" -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "=== LAUNCH APP ===" -ForegroundColor Cyan
Write-Host "Stopping old instance..."
& $adb shell am force-stop com.kiosktouchscreendpr.cosmic

Write-Host "Starting app..."
& $adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity

Write-Host ""
Write-Host "✅ App should now be visible on device screen!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Check device screen - app should be fullscreen" -ForegroundColor White
Write-Host "2. Run: .\debug-live.ps1  (to see live logs)" -ForegroundColor White
Write-Host "3. If blank screen, check: .\check-errors.ps1" -ForegroundColor White
Write-Host ""
