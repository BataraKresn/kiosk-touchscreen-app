#!/usr/bin/env pwsh
# Monitor logs untuk debugging Display CMS

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Monitor Display CMS Fetch" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Silakan tap 'Ambil Display dari CMS' di aplikasi..." -ForegroundColor Yellow
Write-Host "Monitoring logs..." -ForegroundColor Yellow
Write-Host ""

# Monitor logs yang relevan
adb logcat | Select-String -Pattern "DeviceApi|getDisplays|SettingsViewModel|Display|HTTP|Failed|Exception|Error" -Context 1,1
