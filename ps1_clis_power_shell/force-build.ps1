# Force Build - Aggressive OneDrive Fix
# Use when normal build fails

Write-Host ""
Write-Host "================================================================" -ForegroundColor Red
Write-Host "   FORCE BUILD - Aggressive OneDrive Fix                       " -ForegroundColor Red
Write-Host "================================================================" -ForegroundColor Red
Write-Host ""

Write-Host "⚠️  This will:" -ForegroundColor Yellow
Write-Host "   1. Kill all Java processes" -ForegroundColor White
Write-Host "   2. Stop OneDrive temporarily" -ForegroundColor White
Write-Host "   3. Force delete build directories" -ForegroundColor White
Write-Host "   4. Build APK" -ForegroundColor White
Write-Host "   5. Restart OneDrive" -ForegroundColor White
Write-Host ""

$response = Read-Host "Continue? (Y/N)"
if ($response -ne "Y" -and $response -ne "y") {
    Write-Host "Cancelled" -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "[1/6] Stopping OneDrive..." -ForegroundColor Yellow
try {
    Stop-Process -Name "OneDrive" -Force -ErrorAction SilentlyContinue
    Write-Host "   ✅ OneDrive stopped" -ForegroundColor Green
    Start-Sleep -Seconds 2
} catch {
    Write-Host "   ⚠️  Could not stop OneDrive" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[2/6] Killing Java/Gradle processes..." -ForegroundColor Yellow
.\gradlew.bat --stop 2>&1 | Out-Null
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name javaw -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Write-Host "   ✅ Processes killed" -ForegroundColor Green
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "[3/6] Force deleting build directories..." -ForegroundColor Yellow

# Try multiple times
$maxAttempts = 3
for ($i = 1; $i -le $maxAttempts; $i++) {
    Write-Host "   Attempt $i/$maxAttempts..." -ForegroundColor Gray

    if (Test-Path "app\build") {
        cmd /c "rmdir /s /q app\build" 2>&1 | Out-Null
    }

    if (Test-Path ".gradle") {
        cmd /c "rmdir /s /q .gradle" 2>&1 | Out-Null
    }

    Start-Sleep -Seconds 1
}

if (!(Test-Path "app\build")) {
    Write-Host "   ✅ app\build deleted" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  app\build still exists (will try to build anyway)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[4/6] Building APK (no daemon, no cache)..." -ForegroundColor Yellow
Write-Host "   This will take 3-5 minutes..." -ForegroundColor Gray
Write-Host ""

$buildOutput = .\gradlew.bat assembleDebug --no-daemon --no-build-cache --offline 2>&1

Write-Host ""
Write-Host "[5/6] Checking build result..." -ForegroundColor Yellow

if ($buildOutput -like "*BUILD SUCCESSFUL*") {
    Write-Host "   ✅ BUILD SUCCESSFUL!" -ForegroundColor Green

    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = [math]::Round((Get-Item $apkPath).Length/1MB, 2)
        Write-Host ""
        Write-Host "APK created:" -ForegroundColor Green
        Write-Host "  $apkPath" -ForegroundColor White
        Write-Host "  Size: $apkSize MB" -ForegroundColor White
    }
} else {
    Write-Host "   ❌ BUILD FAILED" -ForegroundColor Red
    Write-Host ""
    Write-Host "Last errors:" -ForegroundColor Yellow
    $buildOutput | Select-Object -Last 20 | Write-Host
}

Write-Host ""
Write-Host "[6/6] Restarting OneDrive..." -ForegroundColor Yellow
try {
    Start-Process "$env:LOCALAPPDATA\Microsoft\OneDrive\OneDrive.exe" -ErrorAction SilentlyContinue
    Write-Host "   ✅ OneDrive restarted" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️  Start OneDrive manually if needed" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

if ($buildOutput -like "*BUILD SUCCESSFUL*") {
    Write-Host "✅ SUCCESS! Install with: .\install.ps1" -ForegroundColor Green
} else {
    Write-Host "❌ FAILED - Try:" -ForegroundColor Red
    Write-Host "   1. Move project to C:\dev\" -ForegroundColor Yellow
    Write-Host "   2. Build from there (outside OneDrive)" -ForegroundColor Yellow
    Write-Host "   3. Or use Android Studio: Build → Build APK(s)" -ForegroundColor Yellow
}
Write-Host ""
