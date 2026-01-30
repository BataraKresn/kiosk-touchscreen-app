# View Cosmic Kiosk Logs
# Live log viewer for debugging

$adbPath = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   Cosmic Kiosk - Live Log Viewer              " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtering logs for 'cosmic', 'WebView', 'WebSocket'..." -ForegroundColor Gray
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Clear old logs first
& $adbPath logcat -c 2>&1 | Out-Null

# Start watching logs
& $adbPath logcat | Select-String -Pattern "cosmic|WebView|WebSocket|MainActivity|HomeViewModel" -CaseSensitive:$false
