#!/usr/bin/env pwsh
# Test Backend API Endpoints

param(
    [string]$BaseUrl = "https://kiosk.mugshot.dev"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Testing Backend API Endpoints" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host ""

# Test 1: Health Check
Write-Host "1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/health" -Method GET -Headers @{"Accept"="application/json"} -ErrorAction Stop
    Write-Host "   Status: $($health.status)" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "   Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 2: List Displays
Write-Host "2. Testing Displays List Endpoint..." -ForegroundColor Yellow
try {
    $displays = Invoke-RestMethod -Uri "$BaseUrl/api/displays?per_page=50" -Method GET -Headers @{"Accept"="application/json"} -ErrorAction Stop
    
    if ($displays.data -and $displays.data.Count -gt 0) {
        Write-Host "   Found $($displays.data.Count) displays:" -ForegroundColor Green
        foreach ($display in $displays.data) {
            Write-Host "   - ID: $($display.id), Name: $($display.name)" -ForegroundColor Cyan
        }
    } else {
        Write-Host "   No displays found (data is empty)" -ForegroundColor Yellow
    }
    Write-Host ""
} catch {
    Write-Host "   Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Message -match "404") {
        Write-Host "   Backend belum implement endpoint /api/displays" -ForegroundColor Red
        Write-Host "   Lihat BACKEND_API_REQUIRED.md untuk implementasi" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Test 3: Search Displays
Write-Host "3. Testing Display Search..." -ForegroundColor Yellow
try {
    $searchResult = Invoke-RestMethod -Uri "$BaseUrl/api/displays?search=DISPLAY" -Method GET -Headers @{"Accept"="application/json"} -ErrorAction Stop
    Write-Host "   Search 'DISPLAY' found: $($searchResult.data.Count) results" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "   Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 4: Register Device (POST)
Write-Host "4. Testing Device Registration..." -ForegroundColor Yellow
try {
    $registerData = @{
        device_id = "test-device-$(Get-Random -Maximum 9999)"
        device_name = "Test Tablet"
        device_info = @{
            manufacturer = "samsung"
            model = "SM-T510"
            android_version = "11"
        }
    } | ConvertTo-Json
    
    $registerResult = Invoke-RestMethod -Uri "$BaseUrl/api/displays/register" -Method POST -Body $registerData -ContentType "application/json" -Headers @{"Accept"="application/json"} -ErrorAction Stop
    Write-Host "   Device registered successfully!" -ForegroundColor Green
    Write-Host "   Token: $($registerResult.display.token)" -ForegroundColor Cyan
    Write-Host "   Name: $($registerResult.display.name)" -ForegroundColor Cyan
    Write-Host ""
} catch {
    Write-Host "   Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Summary
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. If /api/displays returns 404:" -ForegroundColor White
Write-Host "   - Backend perlu implement endpoint ini" -ForegroundColor White
Write-Host "   - Lihat: BACKEND_API_REQUIRED.md" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. If /api/displays returns empty data:" -ForegroundColor White
Write-Host "   - Seed dummy data di database" -ForegroundColor White
Write-Host "   - php artisan db:seed --class=DisplaySeeder" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. After backend ready:" -ForegroundColor White
Write-Host "   - Test dari Android app" -ForegroundColor White
Write-Host "   - Settings → Ambil Display dari CMS" -ForegroundColor Cyan
Write-Host ""
