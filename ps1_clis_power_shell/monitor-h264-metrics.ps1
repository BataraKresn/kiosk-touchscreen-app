#!/usr/bin/env pwsh
<#
.SYNOPSIS
Real-time H.264 metrics monitoring dashboard
.DESCRIPTION
Displays live metrics in a dashboard format:
- Frame rate and type distribution
- Bandwidth usage estimate
- Frame drops and latency
- Quality adaptation events
- Keyframe intervals
#>

param(
    [string]$DeviceId = "",
    [int]$UpdateIntervalMs = 1000,
    [int]$DurationSeconds = 120
)

$GREEN = "`e[92m"
$YELLOW = "`e[93m"
$RED = "`e[91m"
$CYAN = "`e[96m"
$MAGENTA = "`e[95m"
$RESET = "`e[0m"

class MetricsCollector {
    [string]$DeviceId
    [array]$FrameLog
    [int]$H264Frames
    [int]$JpegFrames
    [int]$Keyframes
    [int]$FrameDrops
    [double]$AvgLatency
    [datetime]$StartTime
    [string]$CurrentQuality
    [array]$QualityChanges

    MetricsCollector([string]$Device) {
        $this.DeviceId = $Device
        $this.FrameLog = @()
        $this.H264Frames = 0
        $this.JpegFrames = 0
        $this.Keyframes = 0
        $this.FrameDrops = 0
        $this.AvgLatency = 0
        $this.StartTime = Get-Date
        $this.CurrentQuality = "MEDIUM"
        $this.QualityChanges = @()
    }

    [void] UpdateMetrics([string]$LogContent) {
        # Count frame types
        $h264 = @($LogContent | Select-String -Pattern "H264|format.*h264" -AllMatches).Count
        $jpeg = @($LogContent | Select-String -Pattern "JPEG|format.*jpeg" -AllMatches).Count
        
        $this.H264Frames = $h264
        $this.JpegFrames = $jpeg
        $this.Keyframes = @($LogContent | Select-String -Pattern "is_keyframe.*true" -AllMatches).Count
        $this.FrameDrops = @($LogContent | Select-String -Pattern "dropped|drop" -AllMatches).Count
        
        # Extract quality level
        if ($LogContent -match "Quality.*HIGH") { $this.CurrentQuality = "HIGH" }
        elseif ($LogContent -match "Quality.*MEDIUM") { $this.CurrentQuality = "MEDIUM" }
        elseif ($LogContent -match "Quality.*LOW") { $this.CurrentQuality = "LOW" }
    }

    [double] GetFPS() {
        $elapsed = ((Get-Date) - $this.StartTime).TotalSeconds
        if ($elapsed -eq 0) { return 0 }
        return [math]::Round(($this.H264Frames + $this.JpegFrames) / $elapsed, 1)
    }

    [double] GetCompressionRatio() {
        $total = $this.H264Frames + $this.JpegFrames
        if ($total -eq 0) { return 0 }
        return [math]::Round(($this.H264Frames / $total) * 100, 1)
    }

    [double] GetKeyframeInterval() {
        $elapsed = ((Get-Date) - $this.StartTime).TotalSeconds
        if ($this.Keyframes -eq 0) { return 0 }
        return [math]::Round($elapsed / $this.Keyframes, 2)
    }
}

function Show-Dashboard {
    param(
        [MetricsCollector]$Metrics,
        [int]$ElapsedSeconds
    )

    Clear-Host
    
    # Header
    Write-Host "`n${CYAN}╔═══════════════════════════════════════════════════════════╗${RESET}"
    Write-Host "${CYAN}║       H.264 REAL-TIME METRICS DASHBOARD (Demo Device)      ║${RESET}"
    Write-Host "${CYAN}╚═══════════════════════════════════════════════════════════╝${RESET}`n"

    # Elapsed time and device info
    Write-Host "${YELLOW}Device:${RESET} " $Metrics.DeviceId "  |  ${YELLOW}Elapsed:${RESET} ${ElapsedSeconds}s`n"

    # Frame Distribution
    Write-Host "${MAGENTA}📊 FRAME DISTRIBUTION${RESET}"
    Write-Host "├─ H.264 Frames: ${GREEN}$($Metrics.H264Frames)${RESET}"
    Write-Host "├─ JPEG Frames:  ${YELLOW}$($Metrics.JpegFrames)${RESET}"
    $totalFrames = $Metrics.H264Frames + $Metrics.JpegFrames
    if ($totalFrames -gt 0) {
        $h264Pct = [math]::Round(($Metrics.H264Frames / $totalFrames) * 100, 1)
        Write-Host "└─ H.264 Ratio:  ${GREEN}${h264Pct}%${RESET}`n"
    } else {
        Write-Host "└─ H.264 Ratio:  N/A (waiting for frames...)`n"
    }

    # Performance Metrics
    Write-Host "${MAGENTA}⚡ PERFORMANCE${RESET}"
    $fps = $Metrics.GetFPS()
    Write-Host "├─ Frame Rate:   ${GREEN}${fps} FPS${RESET}"
    Write-Host "├─ Dropped:      $(if ($Metrics.FrameDrops -gt 5) { $RED } else { $GREEN })$($Metrics.FrameDrops)${RESET}"
    Write-Host "└─ Est. Bitrate: ${GREEN}~$(if ($Metrics.H264Frames -gt 0) { "256" } else { "512" }) Kbps${RESET}`n"

    # Quality Status
    Write-Host "${MAGENTA}🎯 QUALITY ADAPTATION${RESET}"
    $qualityColor = switch ($Metrics.CurrentQuality) {
        "HIGH" { $GREEN }
        "MEDIUM" { $YELLOW }
        "LOW" { $RED }
        default { $CYAN }
    }
    Write-Host "├─ Current Level: ${qualityColor}$($Metrics.CurrentQuality)${RESET}"
    Write-Host "├─ Changes:      $($Metrics.QualityChanges.Count)"
    Write-Host "└─ Status:       $(if ($Metrics.H264Frames -gt $Metrics.JpegFrames) { "${GREEN}H.264 Active${RESET}" } else { "${YELLOW}JPEG Fallback${RESET}" })`n"

    # Keyframe Analysis
    Write-Host "${MAGENTA}🔑 KEYFRAME ANALYSIS${RESET}"
    $kfInterval = $Metrics.GetKeyframeInterval()
    $expectedInterval = 2.0
    $intervalOK = if ([math]::Abs($kfInterval - $expectedInterval) -lt 0.5) { $true } else { $false }
    
    Write-Host "├─ Keyframes:    ${GREEN}$($Metrics.Keyframes)${RESET}"
    if ($kfInterval -gt 0) {
        $intervalColor = if ($intervalOK) { $GREEN } else { $YELLOW }
        Write-Host "├─ Interval:     ${intervalColor}${kfInterval}s${RESET} (Expected: 2s)"
    } else {
        Write-Host "├─ Interval:     Calculating..."
    }
    Write-Host "└─ Status:       $(if ($intervalOK) { "${GREEN}✅ Good${RESET}" } else { "${YELLOW}⚠️  Monitor${RESET}" })`n"

    # Compression Summary
    Write-Host "${MAGENTA}💾 COMPRESSION SUMMARY${RESET}"
    $compressionRatio = $Metrics.GetCompressionRatio()
    if ($compressionRatio -gt 50) {
        $compressionStatus = "${GREEN}Excellent (H.264 dominant)${RESET}"
    } elseif ($compressionRatio -gt 25) {
        $compressionStatus = "${YELLOW}Good (Mixed)${RESET}"
    } else {
        $compressionStatus = "${YELLOW}JPEG Primary${RESET}"
    }
    Write-Host "├─ H.264 Usage:  ${compressionRatio}%"
    Write-Host "├─ Est. Savings: $(if ($compressionRatio -gt 50) { "${GREEN}~60-70%${RESET}" } else { "${YELLOW}~20-30%${RESET}" })"
    Write-Host "└─ Assessment:   $compressionStatus`n"

    # Status bar
    Write-Host "${CYAN}══════════════════════════════════════════════════════════${RESET}`n"
}

function Monitor-Metrics {
    Write-Host "${CYAN}🚀 Starting H.264 Metrics Monitor...${RESET}`n"

    $adb = "adb"
    if ($DeviceId) { $adb += " -s $DeviceId" }

    # Clear logcat
    Invoke-Expression "$adb logcat -c" 2>&1 | Out-Null
    
    $tempLog = "monitor_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
    $metrics = [MetricsCollector]::new($DeviceId)
    
    # Start background logcat capture
    $logProcess = Start-Process -FilePath "adb" -ArgumentList "-s $DeviceId logcat" `
        -RedirectStandardOutput $tempLog -NoNewWindow -PassThru

    Write-Host "${GREEN}✅ Monitoring started. Updates every ${UpdateIntervalMs}ms...${RESET}`n"
    Start-Sleep -Seconds 2

    $startTime = Get-Date
    $updateCount = 0

    try {
        while (((Get-Date) - $startTime).TotalSeconds -lt $DurationSeconds) {
            # Read current logcat
            if (Test-Path $tempLog) {
                $logContent = @(Get-Content $tempLog -ErrorAction SilentlyContinue) -join "`n"
                $metrics.UpdateMetrics($logContent)
            }

            # Show dashboard
            $elapsed = [int]((Get-Date) - $startTime).TotalSeconds
            Show-Dashboard $metrics $elapsed
            
            $updateCount++
            Start-Sleep -Milliseconds $UpdateIntervalMs
        }
    }
    finally {
        Stop-Process -InputObject $logProcess -ErrorAction SilentlyContinue
        Remove-Item $tempLog -ErrorAction SilentlyContinue
    }

    # Final summary
    Write-Host "${CYAN}╔═══════════════════════════════════════════════════════════╗${RESET}"
    Write-Host "${CYAN}║              MONITORING COMPLETE - SUMMARY               ║${RESET}"
    Write-Host "${CYAN}╚═══════════════════════════════════════════════════════════╝${RESET}`n"

    Write-Host "${GREEN}Total H.264 Frames: $($metrics.H264Frames)${RESET}"
    Write-Host "${GREEN}Total JPEG Frames:  $($metrics.JpegFrames)${RESET}"
    Write-Host "${GREEN}Keyframes:          $($metrics.Keyframes)${RESET}"
    Write-Host "${GREEN}Frame Drops:        $($metrics.FrameDrops)${RESET}`n"

    Write-Host "${GREEN}✅ Monitoring session complete!${RESET}"
}

# Run monitor
Monitor-Metrics
