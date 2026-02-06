# H.264 Testing Guide - Phase 2 Demo

## Overview
Testing plan untuk memverifikasi H.264 hardware encoding, bandwidth reduction, dan adaptive quality di 1 demo device.

---

## 🎯 Testing Objectives

1. **Verify H.264 Encoding Active** - Frames transmitted as H.264, bukan JPEG
2. **Measure Bandwidth Reduction** - Target: 75% savings vs JPEG
3. **Monitor Adaptive Quality** - Verify system adapts quality under stress
4. **Test Keyframe Generation** - Every 2 seconds untuk error recovery
5. **Network Stress Test** - Verify H.264 stability saat poor network

---

## 📋 Testing Steps

### Step 1: Verify Device Connection
```powershell
adb devices
# Output should show: <device-id>  device
```

### Step 2: Enable H.264 Logging (Optional Enhancement)

Add more verbose logging ke ScreenCaptureService.kt:
```kotlin
Log.d(TAG, "✅ H.264 Frame: ${frameBytes.size} bytes, keyframe=$isKeyframe")
Log.d(TAG, "📊 Bitrate: $currentBitrate Kbps, Quality: $currentQuality")
Log.d(TAG, "⏱️  Latency: ${healthMonitor.currentMetrics.latency}ms")
```

### Step 3: Run H.264 Encoding Test (60 seconds)
```powershell
cd c:\Users\Public\kiosk-apps\kiosk-touchscreen-app
.\ps1_clis_power_shell\test-h264-encoding.ps1 -DeviceId <device-id> -DurationSeconds 60
```

**Expected Output:**
- ✅ H.264 Frames: > 1000 (minimal JPEG fallback)
- ✅ Keyframe Interval: ~2 seconds
- ✅ Frame Rate: 15-30 FPS
- ✅ No critical errors in logs

### Step 4: Real-time Metrics Monitoring (120 seconds)
```powershell
.\ps1_clis_power_shell\monitor-h264-metrics.ps1 -DeviceId <device-id> -DurationSeconds 120
```

**Dashboard Shows:**
```
📊 FRAME DISTRIBUTION
├─ H.264 Frames: 2150
├─ JPEG Frames:  45
└─ H.264 Ratio:  97.9%

⚡ PERFORMANCE
├─ Frame Rate:   21.3 FPS
├─ Dropped:      2
└─ Est. Bitrate: ~256 Kbps

🎯 QUALITY ADAPTATION
├─ Current Level: MEDIUM
├─ Changes:      1
└─ Status:       H.264 Active

🔑 KEYFRAME ANALYSIS
├─ Keyframes:    60
├─ Interval:     2.1s (Expected: 2s)
└─ Status:       ✅ Good

💾 COMPRESSION SUMMARY
├─ H.264 Usage:  97.9%
├─ Est. Savings: ~60-70%
└─ Assessment:   Excellent (H.264 dominant)
```

### Step 5: Network Stress Test
```powershell
.\ps1_clis_power_shell\test-network-stress.ps1 -DeviceId <device-id> -ConditionLevel "high" -DurationSeconds 60
```

**Condition Levels:**
- `low`: 20ms latency, 1% loss, 5 Mbps (baseline)
- `medium`: 100ms latency, 5% loss, 2 Mbps (typical mobile)
- `high`: 250ms latency, 10% loss, 1 Mbps (poor)
- `critical`: 500ms latency, 20% loss, 500 Kbps (very poor)

**Expected Behavior:**
- Frame drops increase but within acceptable range (<50%)
- Quality automatically downgrades to LOW/MEDIUM
- H.264 remains stable (better than JPEG)
- Keyframes still generated regularly

---

## 📊 Bandwidth Calculation

### JPEG (Before H.264)
```
Assumptions:
- Frame size: ~50-100 KB per frame
- FPS: 20
- Bitrate: (75 KB × 20 × 8) = 12 Mbps (worst case)

Typical: ~4-8 Mbps for quality JPEG
```

### H.264 (After Implementation)
```
Expected:
- Frame size: ~2-5 KB per frame (average)
- FPS: 20
- Bitrate: (4 KB × 20 × 8) = 640 Kbps

Target: 256-512 Kbps (adaptive)
```

### Bandwidth Reduction
```
Reduction = (JPEG Bitrate - H.264 Bitrate) / JPEG Bitrate × 100

Example:
(6 Mbps - 0.5 Mbps) / 6 Mbps × 100 = 91.7% reduction ✅

Conservative Estimate (75%):
- JPEG: 2 Mbps
- H.264: 0.5 Mbps
- Reduction: 75% ✅
```

---

## 🧪 Test Execution Plan

### Day 1: Baseline Testing (30 min)
```powershell
# 1. H.264 encoding verification
.\test-h264-encoding.ps1 -DeviceId <device-id> -DurationSeconds 60

# 2. Real-time metrics (let it run while using remote control)
.\monitor-h264-metrics.ps1 -DeviceId <device-id> -DurationSeconds 120

# 3. Document baseline metrics
# Save output to: test_results_baseline.txt
```

### Day 2: Network Stress Testing (45 min)
```powershell
# Medium condition (100ms latency)
.\test-network-stress.ps1 -DeviceId <device-id> -ConditionLevel "medium"

# High condition (250ms latency)
.\test-network-stress.ps1 -DeviceId <device-id> -ConditionLevel "high"

# Critical condition (500ms latency)
.\test-network-stress.ps1 -DeviceId <device-id> -ConditionLevel "critical"
```

### Day 3: Comparison & Validation (30 min)
```powershell
# Compare with previous metrics
# Verify bandwidth savings > 70%
# Confirm quality adaptation working
# Check error logs for exceptions
```

---

## 📈 Success Criteria

✅ **H.264 Encoding:**
- [ ] H.264 frame ratio > 90%
- [ ] Keyframe interval ≤ 2.5 seconds (target: 2s ± 0.5s)
- [ ] No encoding errors in logs

✅ **Bandwidth Reduction:**
- [ ] Estimated reduction > 70% (target: 75%)
- [ ] Frame size < 5 KB average
- [ ] Total bitrate < 1 Mbps (good quality)

✅ **Quality Adaptation:**
- [ ] Quality changes detected under network stress
- [ ] Fallback to JPEG works when H.264 fails
- [ ] Recovery to H.264 after network improves

✅ **Stability:**
- [ ] Frame drop rate < 5% normal conditions
- [ ] Frame drop rate < 15% under stress
- [ ] No crashes or hangs in 10+ minute sessions
- [ ] CPU usage < 30% (hardware accelerated)

✅ **Keyframe Integrity:**
- [ ] Keyframes detectable (NAL unit type 5)
- [ ] Regular interval maintained
- [ ] Relay server receives keyframe indicator

---

## 🔍 Troubleshooting

### Issue: No H.264 frames detected
**Causes:**
- App not streaming (check remote control active)
- Fallback to JPEG (check device logs for errors)
- Encoding not initialized

**Solution:**
```powershell
# Check device logs for initialization
adb logcat -s "H264EncoderHelper|ScreenCaptureService" | head -50

# Verify encoding thread running
adb logcat | grep -i "encodeframe\|h264"
```

### Issue: Keyframe interval wrong
**Causes:**
- H264EncoderHelper.KEYFRAME_INTERVAL not respected
- Device doesn't support MediaCodec parameter updates

**Solution:**
- Check MediaCodec.setParameters() result
- Verify device has hardware H.264 encoder
- Consider fallback interval

### Issue: Bandwidth not reducing
**Causes:**
- JPEG quality already very low
- H.264 adaptive bitrate not working
- Wrong assumption about baseline bandwidth

**Solution:**
```powershell
# Compare actual file sizes
# JPEG frame sample: ~50-100 KB
# H.264 frame sample: ~2-5 KB

# Extract frames for analysis
adb logcat | grep -i "frame size"
```

### Issue: App crashes with H.264
**Causes:**
- NV21 format conversion error
- MediaCodec initialization failure
- Out of memory

**Solution:**
```powershell
# Enable verbose logging
adb logcat "*:V" | grep -i "crash\|exception\|error"

# Check memory usage
adb shell "dumpsys meminfo" | grep "cosmic"

# Force fallback to JPEG (temporary fix)
# Modify ScreenCaptureService: useH264 = false
```

---

## 📊 Metrics Collection

### Manual Log Extraction
```powershell
# Extract all H.264-related logs
adb logcat > full_logcat.txt
Select-String -Path full_logcat.txt -Pattern "H264|format.*h264" -AllMatches | Measure-Object

# Count frames by type
$h264 = (Select-String -Path full_logcat.txt -Pattern "H264" | Measure-Object).Count
$jpeg = (Select-String -Path full_logcat.txt -Pattern "JPEG" | Measure-Object).Count
Write-Host "H.264: $h264, JPEG: $jpeg, Ratio: $(($h264/($h264+$jpeg))*100)%"
```

### Export Results
```powershell
# Create summary report
$results = @{
    timestamp = Get-Date
    device = "<device-id>"
    h264_frames = 2150
    jpeg_frames = 45
    keyframes = 60
    bandwidth_reduction = "75%"
    avg_latency = "45ms"
    quality_level = "MEDIUM"
}

$results | ConvertTo-Json | Out-File "test_summary.json"
```

---

## 🚀 Next Steps

**After Phase 2 Validation:**
1. ✅ Confirm all success criteria met
2. ✅ Document bandwidth savings achieved
3. ⏳ Deploy to remaining 24 devices
4. ⏳ Monitor in production (3-7 days)
5. ⏳ Measure real-world bandwidth savings
6. ⏳ Compare energy consumption (CPU usage)

---

## 📞 Support

**Log Files Location:**
- Device logs: `adb logcat > device_logs.txt`
- Test output: `h264_test_YYYYMMDD_HHMMSS.log`
- Metrics: `h264_metrics_YYYYMMDD_HHMMSS.csv`

**Key Components:**
- H264EncoderHelper.kt - Hardware encoding
- ScreenCaptureService.kt - Frame capture & encoding selection
- RemoteControlWebSocketClient.kt - Frame transmission
- AdaptiveQualityController.kt - Quality adaptation

---

**Version:** Phase 2.0  
**Last Updated:** February 6, 2026  
**Status:** Ready for Testing
