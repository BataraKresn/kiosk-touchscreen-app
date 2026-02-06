# Deployment & Testing Guide - Backend Integration

**Date:** February 6, 2026  
**Target Device:** Device 74 (samsung SM-A525F - feabbdceecf754b6)  
**Objective:** Verify health metrics and WebSocket remote control integration

---

## Quick Start

### 1. Deploy APK (5 minutes)

```powershell
# From project root
cd c:\Users\Public\kiosk-apps\kiosk-touchscreen-app

# Copy APK to device
adb push app\build\outputs\apk\release\app-release.apk /sdcard/Download/

# Install on device
adb install -r app\build\outputs\apk\release\app-release.apk

# Launch app
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity
```

### 2. Enable Remote Control (1 minute)

**On Device:**
1. Open app
2. Tap Settings → Remote Control
3. Verify baseURL: `https://kiosk.mugshot.dev`
4. Tap "Start Remote Control"

### 3. Monitor Backend (Real-time)

**Terminal 1 - Relay Logs:**
```bash
docker logs -f remote-relay-prod
```

**Terminal 2 - Laravel Logs:**
```bash
docker logs -f cosmic-app-1-prod
# OR
docker exec cosmic-app-1-prod tail -f storage/logs/laravel.log
```

**Terminal 3 - Device Logs:**
```powershell
adb logcat | Select-String "MetricsReporter|RemoteControlWS|HealthMonitor"
```

---

## Expected Output

### 1. Health Metrics Reporter (Every 5 seconds)

**Device Logs:**
```
D/MetricsReporter: Metrics reporter initialized with URL: https://kiosk.mugshot.dev
D/MetricsReporter: ✅ Health report sent successfully
V/MetricsReporter: ✅ Health report sent successfully (repeats every 5s)
```

**Laravel Logs:**
```
[2026-02-06 23:45:10] INFO: Health report received
[2026-02-06 23:45:10] Device: feabbdceecf754b6
[2026-02-06 23:45:10] Quality: high, Latency: 45ms, FPS: 30
[2026-02-06 23:45:15] INFO: Health report received (next report)
```

**Database Verification:**
```bash
# Check metrics count
docker exec cosmic-app-1-prod php artisan tinker --execute="
\$device = \App\Models\MonitoringDevice::where('device_identifier', 'feabbdceecf754b6')->first();
echo 'Metrics Count: ' . \$device->healthMetrics()->count() . PHP_EOL;
echo 'Last Metric: ' . \$device->healthMetrics()->latest()->first() . PHP_EOL;
"
```

**Dashboard Verification:**
1. Open: https://kiosk.mugshot.dev/back-office
2. Navigate to: Monitoring → Devices
3. Find: samsung SM-A525F
4. Verify: Health status changes from "UNKNOWN" to "EXCELLENT/GOOD/POOR"
5. Check: "Last Report" timestamp updates every 5 seconds

---

### 2. WebSocket Connection

**Device Logs:**
```
D/RemoteControlWS: 🌐🌐🌐 Connecting to relay server: wss://kiosk.mugshot.dev/remote-control-ws
D/RemoteControlWS: 📱 DeviceID: 74, Token: 8yvL3wk7y6...
D/RemoteControlWS: WebSocket connected
D/RemoteControlWS: Sending auth - deviceId: 74, token: 8yvL3wk7y6...
D/RemoteControlWS: Authentication message sent
D/RemoteControlWS: ✅ Authentication successful
D/HealthMonitor: Started health monitoring
```

**Relay Logs:**
```
[INFO] New WebSocket connection from 17.17.17.17
[INFO] Device authentication: deviceId=74, role=device
[INFO] ✅ Device authenticated successfully: 74
[INFO] 📱 Device added to room: 74
[DEBUG] Room 74 now has 1 device(s) and 0 viewer(s)
```

**Success Criteria:**
- ✅ No "Authentication timeout" warning
- ✅ No "Invalid device token" error
- ✅ Device stays in room (no disconnections)

---

### 3. Frame Streaming

**Device Logs:**
```
D/ScreenCapture: Frame captured: 1920x1080
D/RemoteControlWS: 📹 Frame sent: 12500 bytes
D/RemoteControlWS: 📹 Frame sent: 12100 bytes (repeating at 30 FPS)
```

**Relay Logs:**
```
[DEBUG] 📹 Broadcasting frame to 0 viewer(s) in room 74
[DEBUG] Frame received from device 74: 12500 bytes
[DEBUG] 📹 Broadcasting frame to 1 viewer(s) in room 74 (when viewer connects)
```

**Browser Test:**
1. Open: https://kiosk.mugshot.dev/remotes/74
2. Click: "Start Remote Control"
3. Expected: Video appears within 2 seconds
4. Verify: No "Frame timeout detected" error
5. Check: Frame rate indicator shows ~30 FPS
6. Confirm: Video is smooth and responsive

---

### 4. Input Commands (Touch/Keyboard)

**Browser Test:**
1. Click/tap anywhere on video stream
2. Type text in browser
3. Use control buttons (Home, Back, etc.)

**Device Logs:**
```
D/RemoteControlWS: Received message: {"type":"touch","action":"down","x":540,"y":960}
D/InputInjection: Processing touch event: down at (540, 960)
D/InputInjection: Touch event injected successfully
```

**Device Behavior:**
- ✅ Taps in browser → executed on physical device
- ✅ Keyboard input → types on device
- ✅ No delay > 100ms

---

## Verification Checklist

### Health Metrics ✅
- [ ] Device logs show "Metrics reporter initialized"
- [ ] Health reports sent every 5 seconds
- [ ] Laravel logs receive POST /api/metrics/health-report
- [ ] Database: `device_health_metrics` count increases
- [ ] Dashboard: Health status updates (not "UNKNOWN")
- [ ] Dashboard: Last report timestamp within 5 seconds

### WebSocket Connection ✅
- [ ] Device connects to wss://kiosk.mugshot.dev/remote-control-ws
- [ ] Authentication successful (no timeout)
- [ ] Relay logs show "Device added to room: 74"
- [ ] No disconnection warnings
- [ ] Heartbeat mechanism working (ping/pong every 15s)

### Frame Streaming ✅
- [ ] Frames captured at target FPS (30 or 60)
- [ ] Frames sent to relay server
- [ ] Browser receives frames within 2 seconds
- [ ] Video quality acceptable
- [ ] Frame drop rate < 5%

### Input Commands ✅
- [ ] Touch events received from browser
- [ ] Touch events injected on device
- [ ] Keyboard events work
- [ ] Control commands work (quality change, etc.)
- [ ] Latency < 100ms

### Session Management ✅
- [ ] Session start recorded in database
- [ ] Session end recorded with duration
- [ ] Frame statistics tracked (sent/dropped)
- [ ] Dashboard shows session history

---

## Troubleshooting

### Problem 1: Health Metrics Not Appearing

**Symptoms:**
- Dashboard still shows "Health: UNKNOWN"
- No logs in Laravel about health reports
- Metrics count stays at 0

**Debug Steps:**
```powershell
# Check device logs
adb logcat | Select-String "MetricsReporter"

# Check if baseURL is correct
adb logcat | Select-String "initialized with URL"

# Test API manually
curl -X POST https://kiosk.mugshot.dev/api/metrics/health-report `
  -H "Content-Type: application/json" `
  -d '{
    "device_id": "feabbdceecf754b6",
    "quality": "high",
    "latency_ms": 45,
    "fps": 30,
    "resolution": "1920x1080",
    "drop_rate": 0.5
  }'
```

**Common Causes:**
1. `backendApiUrl` parameter not passed to `startRemoteControl()`
2. Network connectivity issue
3. Backend API endpoint not accessible
4. Invalid device_id

**Solution:**
```kotlin
// Ensure this is called in SettingsView or RemoteControlScreen
remoteControlViewModel.startRemoteControl(
    context = context,
    deviceId = deviceId,
    authToken = token,
    relayServerUrl = "wss://kiosk.mugshot.dev/remote-control-ws",
    backendApiUrl = "https://kiosk.mugshot.dev"  // ← MUST BE PROVIDED
)
```

---

### Problem 2: WebSocket Authentication Failed

**Symptoms:**
```
[WARN] Authentication timeout, closing connection
[ERROR] ❌ Authentication failed: Invalid device token
```

**Debug Steps:**
```bash
# Verify device token in database
docker exec cosmic-app-1-prod php artisan tinker --execute="
\$remote = \App\Models\Remote::where('id', 74)->first();
echo 'Device Token: ' . \$remote->device_token . PHP_EOL;
echo 'Device Identifier: ' . \$remote->device_identifier . PHP_EOL;
"

# Check device logs
adb logcat | Select-String "DeviceID.*Token"
```

**Common Causes:**
1. Token mismatch between database and APK
2. Device ID incorrect
3. Token expired or regenerated

**Solution:**
1. Get correct token from database
2. Update token in device settings
3. Restart remote control session

---

### Problem 3: Frame Timeout in Browser

**Symptoms:**
- Browser shows "Frame timeout detected"
- No video appears
- Relay logs: "Device disconnected but X viewer(s) still in room"

**Debug Steps:**
```powershell
# Check if frames are being captured
adb logcat | Select-String "ScreenCapture|Frame captured"

# Check if frames are being sent
adb logcat | Select-String "Frame sent"

# Check relay server
docker logs remote-relay-prod | Select-String "Broadcasting frame"
```

**Common Causes:**
1. Screen capture permission not granted
2. MediaProjection service not started
3. Frame encoding failed
4. WebSocket not connected

**Solution:**
1. Grant screen recording permission
2. Start ScreenCaptureService
3. Check encoding logs for errors
4. Verify WebSocket connected and authenticated

---

## Performance Metrics

### Target Metrics
- **Latency:** < 100ms
- **Frame Rate:** 30 FPS minimum
- **Frame Drop Rate:** < 5%
- **Health Report Frequency:** Exactly 5 seconds
- **Authentication Time:** < 1 second
- **Frame Timeout:** Never (continuous streaming)

### Monitoring Commands

```powershell
# Real-time latency monitoring
adb logcat | Select-String "lastLatency"

# Frame rate monitoring
adb logcat | Select-String "Frame sent" | Measure-Object -Line

# Health report timing
adb logcat | Select-String "Health report sent" | Select-String -Pattern "(\d{2}:\d{2}:\d{2})"
```

---

## Success Criteria

### Minimal Success (MVP)
- ✅ Health metrics appear in dashboard (no "UNKNOWN")
- ✅ WebSocket connects and authenticates
- ✅ Video frames appear in browser
- ✅ Touch events work

### Full Success
- ✅ All MVP criteria met
- ✅ Health reports every 5 seconds (consistent)
- ✅ Frame rate ≥ 30 FPS
- ✅ Frame drop rate < 5%
- ✅ Latency < 100ms
- ✅ No disconnections for 5+ minutes
- ✅ Session tracking working
- ✅ Dashboard real-time updates

### Production Ready
- ✅ All Full Success criteria met
- ✅ Stress tested with multiple viewers
- ✅ H.264 encoding working (bandwidth < 500 KB/s)
- ✅ Adaptive quality working
- ✅ Input injection < 50ms latency
- ✅ No memory leaks (24h+ uptime)
- ✅ Battery impact < 15%

---

## Next Phase: H.264 Testing

Once basic integration is verified:

```powershell
# Test H.264 encoding (60 seconds)
.\ps1_clis_power_shell\test-h264-encoding.ps1

# Monitor H.264 metrics (real-time dashboard)
.\ps1_clis_power_shell\monitor-h264-metrics.ps1

# Network stress testing
.\ps1_clis_power_shell\test-network-stress.ps1 -StressLevel high
```

**Expected Results:**
- H.264 ratio > 90%
- Bandwidth reduction > 70%
- Keyframe interval ~2 seconds
- Encoding latency < 50ms

---

**Document Version:** 1.0  
**Last Updated:** February 6, 2026 23:50 UTC  
**Status:** Ready for deployment testing
