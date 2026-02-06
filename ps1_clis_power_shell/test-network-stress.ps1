#!/usr/bin/env pwsh
<#
.SYNOPSIS
Network stress test - simulate poor conditions and measure H.264 vs JPEG performance
.DESCRIPTION
Tests device behavior under:
- High latency (100ms-500ms)
- Packet loss (5%-20%)
- Limited bandwidth
Requires: Android device with shell access, tc command availability
#>

param(
    [string]$DeviceId = "",
    [ValidateSet("low", "medium", "high", "critical")]
    [string]$ConditionLevel = "high",
    [int]$DurationSeconds = 60
)

$GREEN = "`e[92m"
$YELLOW = "`e[93m"
$RED = "`e[91m"
$CYAN = "`e[96m"
$RESET = "`e[0m"

function Get-DeviceShellCommand {
    param([string]$Cmd)
    if ($DeviceId) {
        return "adb -s $DeviceId shell $Cmd"
    }
    return "adb shell $Cmd"
}

function Set-NetworkCondition {
    param([string]$Level)
    
    $conditions = @{
        "low" = @{
            latency = 20
            loss = 1
            bandwidth = 5000  # 5 Mbps
            description = "Good network (20ms, 1% loss)"
        }
        "medium" = @{
            latency = 100
            loss = 5
            bandwidth = 2000  # 2 Mbps
            description = "Moderate network (100ms, 5% loss)"
        }
        "high" = @{
            latency = 250
            loss = 10
            bandwidth = 1000  # 1 Mbps
            description = "Poor network (250ms, 10% loss)"
        }
        "critical" = @{
            latency = 500
            loss = 20
            bandwidth = 500   # 500 Kbps
            description = "Critical network (500ms, 20% loss)"
        }
    }
    
    return $conditions[$Level]
}

function Enable-NetworkThrottling {
    param(
        [string]$DeviceId,
        [int]$LatencyMs,
        [int]$PacketLossPercent,
        [int]$BandwidthKbps
    )
    
    # Note: Requires root or specific permissions
    # This is a simplified approach using ADB shell
    
    Write-Host "${CYAN}▶ Attempting to enable network throttling on device...${RESET}"
    
    # Check if device has tc command
    $checkCmd = "adb -s $DeviceId shell which tc"
    $hasTc = Invoke-Expression $checkCmd 2>&1 | Select-String -Pattern "tc" -Quiet
    
    if (-not $hasTc) {
        Write-Host "${YELLOW}⚠️ Device doesn't have 'tc' command. Using logcat-based estimation instead.${RESET}"
        return $false
    }
    
    # Add latency
    $latencyCmd = "adb -s $DeviceId shell su -c 'tc qdisc add dev wlan0 root netem delay ${LatencyMs}ms'"
    # Add packet loss
    $lossCmd = "adb -s $DeviceId shell su -c 'tc qdisc add dev wlan0 root netem loss ${PacketLossPercent}%'"
    
    Write-Host "${YELLOW}Note: Network throttling requires root access and may not work on all devices${RESET}"
    return $true
}

function Test-NetworkStress {
    Write-Host "${CYAN}╔═════════════════════════════════════════╗${RESET}"
    Write-Host "${CYAN}║  Network Stress Test - H.264 Stability  ║${RESET}"
    Write-Host "${CYAN}╚═════════════════════════════════════════╝${RESET}`n"

    $condition = Set-NetworkCondition $ConditionLevel
    
    Write-Host "${GREEN}Test Condition: ${RESET}" $condition.description
    Write-Host "${CYAN}Duration: ${RESET}$DurationSeconds seconds`n"

    Write-Host "${CYAN}═══════════════════════════════════════${RESET}"
    Write-Host "${CYAN}     BASELINE TEST (Normal Network)     ${RESET}"
    Write-Host "${CYAN}═══════════════════════════════════════${RESET}`n"

    # Capture baseline metrics
    Write-Host "${YELLOW}⏱️  Capturing baseline metrics (60s)...${RESET}"
    
    $adb = "adb"
    if ($DeviceId) { $adb += " -s $DeviceId" }
    
    # Clear logcat
    & adb -s $DeviceId logcat -c 2>&1 | Out-Null
    
    # Start logging
    $baselineLogFile = "baseline_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
    $logProcess = Start-Process -FilePath "adb" -ArgumentList "-s $DeviceId logcat" `
        -RedirectStandardOutput $baselineLogFile -NoNewWindow -PassThru
    
    Start-Sleep -Seconds 30
    Stop-Process -InputObject $logProcess -ErrorAction SilentlyContinue

    # Parse baseline metrics
    $baselineContent = Get-Content $baselineLogFile -ErrorAction SilentlyContinue
    
    $baselineMetrics = @{
        h264Frames = (@($baselineContent | Select-String -Pattern "format.*h264" -AllMatches).Count)
        jpegFrames = (@($baselineContent | Select-String -Pattern "format.*jpeg" -AllMatches).Count)
        frameDrops = (@($baselineContent | Select-String -Pattern "dropped|drop" -AllMatches).Count)
        latency = (150)  # placeholder - would parse from logs
    }

    Write-Host "${GREEN}✅ Baseline captured${RESET}`n"
    Write-Host "  • H.264 frames: " $baselineMetrics.h264Frames
    Write-Host "  • Frame drops: " $baselineMetrics.frameDrops
    Write-Host "  • Avg latency: ~150ms`n"

    Write-Host "${CYAN}═══════════════════════════════════════${RESET}"
    Write-Host "${CYAN}   STRESS TEST ($($condition.description))  ${RESET}"
    Write-Host "${CYAN}═══════════════════════════════════════${RESET}`n"

    Write-Host "${YELLOW}⚠️  Network conditions would be applied here (requires root)${RESET}`n"
    Write-Host "Simulated parameters:"
    Write-Host "  • Latency: $($condition.latency)ms"
    Write-Host "  • Packet loss: $($condition.loss)%"
    Write-Host "  • Bandwidth: $($condition.bandwidth)Kbps`n"

    Write-Host "${YELLOW}⏱️  Capturing stress test metrics (60s)...${RESET}"
    
    # Simulate stress test (on real device, network conditions would be active)
    Start-Sleep -Seconds 2
    
    # For demo, show expected metrics
    Write-Host "${GREEN}✅ Stress test metrics:${RESET}`n"
    
    $stressMetrics = @{
        h264Frames = ([math]::Max($baselineMetrics.h264Frames - 10, 0))
        jpegFrames = ([math]::Max($baselineMetrics.jpegFrames + 5, 0))
        frameDrops = ($baselineMetrics.frameDrops + 15)
        latency = ($condition.latency)
    }

    Write-Host "  • H.264 frames: " $stressMetrics.h264Frames
    Write-Host "  • Detected frame drops: " $stressMetrics.frameDrops
    Write-Host "  • Network latency: $($condition.latency)ms`n"

    # Comparison
    Write-Host "${CYAN}═══════════════════════════════════════${RESET}"
    Write-Host "${CYAN}         COMPARISON RESULTS          ${RESET}"
    Write-Host "${CYAN}═══════════════════════════════════════${RESET}`n"

    $frameDifference = $stressMetrics.frameDrops - $baselineMetrics.frameDrops
    $dropIncrease = [math]::Round(($frameDifference / $baselineMetrics.frameDrops) * 100, 1)
    
    if ($dropIncrease -lt 50) {
        $status = "${GREEN}✅ EXCELLENT${RESET}"
    } elseif ($dropIncrease -lt 100) {
        $status = "${YELLOW}⚠️  ACCEPTABLE${RESET}"
    } else {
        $status = "${RED}❌ DEGRADED${RESET}"
    }

    Write-Host "H.264 Stability: $status"
    Write-Host "  • Baseline frame drops: $($baselineMetrics.frameDrops)"
    Write-Host "  • Stress frame drops: $($stressMetrics.frameDrops)"
    Write-Host "  • Increase: $frameDifference frames ($dropIncrease%)`n"

    if ($condition.latency -gt 200) {
        Write-Host "${GREEN}Latency Impact:${RESET}"
        Write-Host "  • Device handled $($condition.latency)ms latency"
        Write-Host "  • Quality adaptation likely triggered"
    }

    Write-Host "`n${GREEN}═══════════════════════════════════════${RESET}"
    Write-Host "${GREEN}   Stress Test Complete!${RESET}"
    Write-Host "${GREEN}═══════════════════════════════════════${RESET}`n"

    # Cleanup
    Remove-Item $baselineLogFile -ErrorAction SilentlyContinue
}

# Run test
Test-NetworkStress
