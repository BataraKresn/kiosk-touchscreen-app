# Build Script with OneDrive Fix
# Handles OneDrive file locking issues

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   BUILDING COSMIC KIOSK APK (OneDrive Safe)                   " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Stop any running Gradle processes
Write-Host "[1/4] Stopping Gradle daemons..." -ForegroundColor Yellow
.\gradlew.bat --stop 2>&1 | Out-Null
Start-Sleep -Seconds 1

# Kill any Java processes (Gradle)
Write-Host "[2/4] Cleaning up processes..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Clean build directories
Write-Host "[3/4] Cleaning build directories..." -ForegroundColor Yellow
if (Test-Path "app\build") {
    try {
        Remove-Item -Recurse -Force "app\build" -ErrorAction Stop
        Write-Host "   ✅ app\build cleaned" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Could not clean app\build (OneDrive locked)" -ForegroundColor Yellow
        Write-Host "   Trying to build anyway..." -ForegroundColor Gray
    }
}

if (Test-Path ".gradle") {
    try {
        Remove-Item -Recurse -Force ".gradle" -ErrorAction Stop
        Write-Host "   ✅ .gradle cleaned" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Could not clean .gradle (OneDrive locked)" -ForegroundColor Yellow
    }
}

# Build with OneDrive-safe settings
Write-Host ""
Write-Host "[4/4] Building APK (this may take 3-5 minutes)..." -ForegroundColor Yellow
Write-Host "   Using: --no-daemon --no-build-cache" -ForegroundColor Gray
Write-Host ""

$buildStart = Get-Date

# Run build
$result = .\gradlew.bat clean assembleDebug --no-daemon --no-build-cache 2>&1

$buildEnd = Get-Date
$duration = $buildEnd - $buildStart

# Check result
Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan

if ($result -like "*BUILD SUCCESSFUL*") {
    Write-Host "   ✅ BUILD SUCCESSFUL!                                        " -ForegroundColor Green
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Build time: $($duration.Minutes)m $($duration.Seconds)s" -ForegroundColor Gray
    Write-Host ""

    # Check APK
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = [math]::Round((Get-Item $apkPath).Length/1MB, 2)
        Write-Host "APK created:" -ForegroundColor Green
        Write-Host "  Location: $apkPath" -ForegroundColor White
        Write-Host "  Size: $apkSize MB" -ForegroundColor White
        Write-Host ""
        Write-Host "Next step:" -ForegroundColor Yellow
        Write-Host "  .\install.ps1" -ForegroundColor Cyan
        Write-Host ""
    }
} else {
    Write-Host "   ❌ BUILD FAILED                                             " -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host ""

    # Show last 30 lines of error
    Write-Host "Error details:" -ForegroundColor Yellow
    $result | Select-Object -Last 30 | ForEach-Object {
        if ($_ -match "FAILED|ERROR|exception|AccessDenied") {
            Write-Host $_ -ForegroundColor Red
        } else {
            Write-Host $_
        }
    }

    Write-Host ""
    Write-Host "Common fixes:" -ForegroundColor Yellow
    Write-Host "1. Close Android Studio if running" -ForegroundColor White
    Write-Host "2. Pause OneDrive sync temporarily" -ForegroundColor White
    Write-Host "3. Run as Administrator" -ForegroundColor White
    Write-Host "4. Try: .\build.ps1" -ForegroundColor White
    Write-Host ""
}
