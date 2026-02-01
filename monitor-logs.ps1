# Monitor Android Logcat untuk ConnectionManager, AppViewModel, DeviceRegistration
# PowerShell tidak punya grep, gunakan Select-String

Write-Host "📱 Monitoring Android Logcat..." -ForegroundColor Cyan
Write-Host "Filtering: ConnectionManager | AppViewModel | DeviceRegistration | Heartbeat" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Gray

# Clear logcat buffer dulu
adb logcat -c

# Monitor dengan filter
adb logcat | Select-String -Pattern "ConnectionManager|AppViewModel|DeviceRegistration|Heartbeat" --Context 0,0
