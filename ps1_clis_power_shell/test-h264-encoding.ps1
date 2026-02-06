#!/usr/bin/env pwsh
<#
.SYNOPSIS
Test H.264 encoding on demo device - capture logs, monitor bandwidth, verify frame format
.DESCRIPTION
- Capture logcat filtered for H.264 logs
- Monitor frame transmission rates and sizes
- Verify keyframe generation every 2 seconds
- Track adaptive quality transitions
#>

param(
    [string]$DeviceId = "",
    [int]$DurationSeconds = 60,
    [string]$OutputDir = "."
)

# Colors
$GREEN = "`e[92m"
$YELLOW = "`e[93m"
$RED = "`e[91m"
$CYAN = "`e[96m"
$RESET = "`e[0m"

function Get-AdbCommand {
    if ($DeviceId) {
        return "adb -s $DeviceId"
    }
    return "adb"
}

function Get-ConnectedDevices {
    $output = adb devices | Select-Object -Skip 1 | Where-Object { $_ -and $_ -notmatch "List of attached" }
    return $output | ForEach-Object {
        $parts = $_ -split '\s+'
        if ($parts.Count -ge 2) { $parts[0] }
    }
}

function Test-H264Encoding {
    Write-Host "${CYAN}═══════════════════════════════════════${RESET}"
    Write-Host "${CYAN}     H.264 Encoding Test (Demo Device)     ${RESET}"
    Write-Host "${CYAN}═══════════════════════════════════════${RESET}`n"

    # Get device list
    $devices = Get-ConnectedDevices
    if (-not $devices) {
        Write-Host "${RED}❌ No devices connected!${RESET}"
        exit 1
    }

    if (-not $DeviceId) {
        Write-Host "${YELLOW}Available devices:${RESET}"
        $devices | ForEach-Object { Write-Host "  • $_" }
        $DeviceId = $devices[0]
        Write-Host "${GREEN}Using first device: $DeviceId${RESET}`n"
    }

    $adb = Get-AdbCommand

    # Clear logcat
    Write-Host "${CYAN}▶ Clearing logcat...${RESET}"
    & adb -s $DeviceId logcat -c

    # Create output files
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $logFile = Join-Path $OutputDir "h264_test_${timestamp}.log"
    $metricsFile = Join-Path $OutputDir "h264_metrics_${timestamp}.txt"
    $frameDataFile = Join-Path $OutputDir "h264_frames_${timestamp}.csv"

    # Start logcat in background
    Write-Host "${CYAN}▶ Capturing logcat for $DurationSeconds seconds...${RESET}`n"
    $logcatProcess = Start-Process -FilePath "adb" -ArgumentList "-s $DeviceId logcat" `
        -RedirectStandardOutput $logFile -NoNewWindow -PassThru

    # Monitor in real-time
    $frameCount = 0
    $h264Count = 0
    $jpegCount = 0
    $keyframeCount = 0
    $totalBytes = 0
    $startTime = Get-Date

    Write-Host "${CYAN}═ REAL-TIME MONITORING ═${RESET}"
    Write-Host "Frame Type Distribution | Bandwidth | Keyframes | Quality`n"

    # Read logcat output in real-time
    $logReader = [System.IO.StreamReader]::new($logFile)
    Start-Sleep -Milliseconds 100 # Let logcat start

    $monitorJob = Start-Job -ScriptBlock {
        param($logFile, $duration)
        $startTime = Get-Date
        $lastCheck = $startTime
        $h264 = 0
        $jpeg = 0
        $keyframes = 0
        $bytes = 0

        while ((Get-Date) -lt $startTime.AddSeconds($duration)) {
            if (Test-Path $logFile) {
                $content = @(Get-Content $logFile -ErrorAction SilentlyContinue)
                foreach ($line in $content) {
                    if ($line -match "H264|format.*h264") { $h264++ }
                    if ($line -match "JPEG|format.*jpeg") { $jpeg++ }
                    if ($line -match "keyframe|is_keyframe.*true") { $keyframes++ }
                    if ($line -match "Frame size.*(\d+)") {
                        $bytes += [int]$matches[1]
                    }
                }
            }
            Start-Sleep -Milliseconds 500
        }

        return @{
            h264 = $h264
            jpeg = $jpeg
            keyframes = $keyframes
            bytes = $bytes
        }
    } -ArgumentList $logFile, $DurationSeconds

    # Wait for test duration
    Start-Sleep -Seconds $DurationSeconds
    Stop-Process -InputObject $logcatProcess -ErrorAction SilentlyContinue

    # Get results
    $result = Receive-Job -Job $monitorJob -Wait
    $elapsedSeconds = ((Get-Date) - $startTime).TotalSeconds

    # Parse logcat for actual metrics
    if (Test-Path $logFile) {
        $logContent = Get-Content $logFile -ErrorAction SilentlyContinue
        
        # Count frame types
        $h264Count = @($logContent | Select-String -Pattern "H264|format.*h264" -AllMatches).Count
        $jpegCount = @($logContent | Select-String -Pattern "JPEG|format.*jpeg" -AllMatches).Count
        $keyframeCount = @($logContent | Select-String -Pattern "is_keyframe.*true" -AllMatches).Count
        
        # Extract bandwidth info
        $bandwidthLines = $logContent | Select-String -Pattern "Bitrate|bandwidth|Frame size"
    }

    # Display Results
    Write-Host "`n${CYAN}═══════════════════════════════════════${RESET}"
    Write-Host "${CYAN}         TEST RESULTS (${elapsedSeconds}s)         ${RESET}"
    Write-Host "${CYAN}═══════════════════════════════════════${RESET}`n"

    # Frame Type Distribution
    $totalFrames = $h264Count + $jpegCount
    if ($totalFrames -gt 0) {
        $h264Percent = [math]::Round(($h264Count / $totalFrames) * 100, 1)
        $jpegPercent = [math]::Round(($jpegCount / $totalFrames) * 100, 1)
        
        Write-Host "${GREEN}Frame Type Distribution:${RESET}"
        Write-Host "  • H.264: ${GREEN}$h264Count frames ($h264Percent%)${RESET}"
        Write-Host "  • JPEG:  ${YELLOW}$jpegCount frames ($jpegPercent%)${RESET}"
    } else {
        Write-Host "${YELLOW}No frame data captured (check if app is streaming)${RESET}"
    }

    # Keyframe Analysis
    if ($h264Count -gt 0) {
        $keyframeInterval = if ($keyframeCount -gt 0) { 
            [math]::Round($elapsedSeconds / $keyframeCount, 2) 
        } else { 
            "N/A" 
        }
        Write-Host "`n${GREEN}Keyframe Analysis:${RESET}"
        Write-Host "  • Keyframes: ${GREEN}$keyframeCount${RESET}"
        Write-Host "  • Expected interval: 2 seconds"
        Write-Host "  • Actual interval: ${GREEN}$keyframeInterval seconds${RESET}"
    }

    # Bandwidth Estimation
    if ($h264Count -gt 0) {
        $fps = [math]::Round($totalFrames / $elapsedSeconds, 1)
        Write-Host "`n${GREEN}Encoding Performance:${RESET}"
        Write-Host "  • Frame Rate: ${GREEN}$fps FPS${RESET}"
        Write-Host "  • Total Frames: $totalFrames"
    }

    # Quality Adaptation
    $qualityLines = $logContent | Select-String -Pattern "Quality.*changed|Adaptive.*quality" -ErrorAction SilentlyContinue
    if ($qualityLines) {
        Write-Host "`n${GREEN}Quality Transitions:${RESET}"
        $qualityLines | ForEach-Object {
            Write-Host "  • $_"
        }
    }

    # Show sample logs
    Write-Host "`n${CYAN}═════════════════════════════════════════${RESET}"
    Write-Host "${CYAN}      SAMPLE LOGCAT OUTPUT (last 10 lines)       ${RESET}"
    Write-Host "${CYAN}═════════════════════════════════════════${RESET}`n"
    
    $logLines = @(Get-Content $logFile -ErrorAction SilentlyContinue)
    $logLines[-10..-1] | ForEach-Object {
        if ($_ -match "H264") {
            Write-Host "${GREEN}$_${RESET}"
        } elseif ($_ -match "ERROR|Error") {
            Write-Host "${RED}$_${RESET}"
        } else {
            Write-Host $_
        }
    }

    Write-Host "`n${GREEN}✅ Logs saved to: $logFile${RESET}"
    Write-Host "${GREEN}✅ Full test complete!${RESET}`n"

    return @{
        device = $DeviceId
        duration = $elapsedSeconds
        h264Frames = $h264Count
        jpegFrames = $jpegCount
        keyframes = $keyframeCount
        logFile = $logFile
    }
}

# Run test
$result = Test-H264Encoding
Write-Host "${CYAN}═══════════════════════════════════════${RESET}`n"
