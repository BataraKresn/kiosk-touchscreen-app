# Check for Errors and Crashes
# Shows last errors from device

$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Red
Write-Host "   CHECKING FOR ERRORS & CRASHES                               " -ForegroundColor Red
Write-Host "================================================================" -ForegroundColor Red
Write-Host ""

Write-Host "[1] Last app crashes (AndroidRuntime):" -ForegroundColor Yellow
Write-Host "---------------------------------------------------"
& $adb logcat -d AndroidRuntime:E *:S | Select-Object -Last 50
Write-Host ""

Write-Host "[2] Fatal errors:" -ForegroundColor Yellow
Write-Host "---------------------------------------------------"
& $adb logcat -d *:F | Select-Object -Last 30
Write-Host ""

Write-Host "[3] Cosmic app errors:" -ForegroundColor Yellow
Write-Host "---------------------------------------------------"
& $adb logcat -d *:E | Select-String "cosmic|kiosk" -CaseSensitive:$false | Select-Object -Last 30
Write-Host ""

Write-Host "[4] WebView errors:" -ForegroundColor Yellow
Write-Host "---------------------------------------------------"
& $adb logcat -d chromium:E WebView:E *:S | Select-Object -Last 20
Write-Host ""

Write-Host "[5] Check if app is running:" -ForegroundColor Yellow
Write-Host "---------------------------------------------------"
$running = & $adb shell ps | Select-String "cosmic"
if ($running) {
    Write-Host "✅ App is running: $running" -ForegroundColor Green
} else {
    Write-Host "❌ App is NOT running!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Launch it with: .\launch-app.ps1" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   COMMON ISSUES & SOLUTIONS                                   " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "If app crashes on startup:" -ForegroundColor Yellow
Write-Host "  1. Check env.properties values are correct" -ForegroundColor White
Write-Host "  2. Check backend server is accessible" -ForegroundColor White
Write-Host "  3. Check device has internet connection" -ForegroundColor White
Write-Host ""

Write-Host "If WebView shows blank:" -ForegroundColor Yellow
Write-Host "  1. Check WEBVIEW_BASEURL in env.properties" -ForegroundColor White
Write-Host "  2. Test URL in device Chrome browser" -ForegroundColor White
Write-Host "  3. Check SSL certificate is valid" -ForegroundColor White
Write-Host ""

Write-Host "If WebSocket won't connect:" -ForegroundColor Yellow
Write-Host "  1. Check WS_URL in env.properties" -ForegroundColor White
Write-Host "  2. Check firewall allows WebSocket port" -ForegroundColor White
Write-Host "  3. Verify backend WebSocket server is running" -ForegroundColor White
Write-Host ""

Write-Host "To see live logs: .\debug-live.ps1" -ForegroundColor Cyan
Write-Host ""
