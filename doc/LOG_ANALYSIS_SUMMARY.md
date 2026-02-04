# 📊 Cosmic Kiosk - Diagnostik Status & Checklist

**Date:** February 4, 2026  
**Project:** Kiosk Touchscreen App - WebSocket & Remote Control Integration

---

## 🎯 Analisis 4 Poin Kritis

### ✅ 1. APK Code Ada

**Status:** ✅ **CONFIRMED READY**

#### Evidence:
- **Build Configuration:** `app/build.gradle.kts` terstruktur dengan baik
- **Manifest:** `AndroidManifest.xml` mencakup semua permissions yang diperlukan
- **Komponent Utama Tersedia:**
  - WebSocket implementation: `WebSocketDataSourceImpl.kt`
  - Remote Control: `RemoteControlWebSocketClient.kt`
  - Settings UI: `SettingsView.kt` & `SettingsViewModel.kt`
  - Home & Dashboard UI: `MainActivity`, `HomeView`, `SettingsView`

#### Key Features Built-In:
```
✅ Full-screen Kiosk Mode
✅ WebSocket Client (Ktor-based)
✅ Settings UI untuk token management
✅ Remote Control functionality
✅ Heartbeat/Ping mechanism
✅ Auto-reconnect logic
✅ Offline caching (ResponseCache.kt)
✅ Permission handling (INTERNET, FOREGROUND_SERVICE, MEDIA_PROJECTION)
```

#### Build Configuration:
```kotlin
// env.properties configured correctly:
APP_PASSWORD=260224
WS_URL=wss://kiosk.mugshot.dev/remote-control-ws
WEBVIEW_BASEURL=https://kiosk.mugshot.dev
```

---

### ❓ 2. Device Token/ID Ada di SharedPreferences?

**Status:** ⚠️ **PARTIALLY IMPLEMENTED - REQUIRES USER ACTION**

#### Stored Keys:
```kotlin
// From AppConstant.kt:
DEVICE_ID = "device_id"
REMOTE_ID = "remote_id"
REMOTE_TOKEN = "remote_token"
TOKEN = "token"
```

#### Storage Locations:
```kotlin
// SharedPreferences names found:
"app_prefs"           // Device token & settings
"response_cache"      // Cached heartbeat responses
```

#### How It Works:
```
1. Settings Screen → User inputs token (e.g., "DISPLAY-001")
   ↓
2. saveToken() in SettingsViewModel
   ↓
3. preferences.edit().putString(TOKEN, value).apply()
   ↓
4. Stored in SharedPreferences["app_prefs"]["token"]
```

#### Remote Control Registration Flow:
```kotlin
// From SettingsViewModel.kt (line 170-180)
val response = deviceApi.registerRemoteDevice(
    baseUrl = baseUrl,
    deviceId = deviceId,
    deviceName = deviceName,
    appVersion = BuildConfig.VERSION_NAME
)

if (response?.success == true) {
    preferences.edit().apply {
        putString(AppConstant.DEVICE_ID, deviceId)
        putString(AppConstant.REMOTE_ID, response.data.remoteId.toString())
        putString(AppConstant.REMOTE_TOKEN, response.data.token)
        apply()
    }
}
```

#### Verification Command:
```powershell
# Check SharedPreferences on device
adb shell dumpsys preferences com.kiosktouchscreendpr.cosmic

# Or via Android Studio:
# View → Tool Windows → App Inspection → Database Inspector
```

#### ⚠️ Current Issue:
- **Token must be manually entered** in Settings screen
- **Or auto-registered via API** (if backend is ready)
- **If empty:** Remote control will fail with "Not registered" message

#### ✅ TODO: Verify Token Existence
```
Before attempting remote control:
1. Open Settings screen
2. Check "Device Information" section
3. Should show:
   - Device ID: [Android device ID]
   - Token: DISPLAY-XXX (or masked: DISPLAY-...XXXX)
   - Relay Server: wss://kiosk.mugshot.dev/remote-control-ws
```

---

### ❓ 3. Relay URL Correct?

**Status:** ✅ **CORRECT - VERIFIED IN CODE**

#### Configured URLs:
```properties
# From env.properties:
WS_URL=wss://kiosk.mugshot.dev/remote-control-ws
WEBVIEW_BASEURL=https://kiosk.mugshot.dev
```

#### How It's Built:
```kotlin
// From SettingsView.kt (line 56-67)
val baseUrl = BuildConfig.WEBVIEW_BASEURL.takeIf { it.isNotBlank() } 
    ?: "https://kiosk.mugshot.dev"

// Relay server URL derived from WEBVIEW_BASEURL:
val relayServerUrl = baseUrl
    .replace("https://", "wss://")
    .replace("http://", "ws://") 
    + "/remote-control-ws"

// Result: wss://kiosk.mugshot.dev/remote-control-ws ✅
```

#### WebSocket Connection Points:

**1. Main App WebSocket** (AppViewModel.kt):
```kotlin
private val websocketUrl = BuildConfig.WS_URL
private val wsUrl = "$websocketUrl/ws_status_device?url=$formatLink"
// URL: wss://kiosk.mugshot.dev/remote-control-ws/ws_status_device?url=...
```

**2. Remote Control WebSocket** (RemoteControlWebSocketClient.kt):
```kotlin
fun connect(wsUrl: String, token: String, devId: String) {
    // wsUrl: wss://kiosk.mugshot.dev/remote-control-ws
    connectInternal(wsUrl)
}
```

#### Verification:
```powershell
# Test connectivity to relay server
Test-NetConnection -ComputerName kiosk.mugshot.dev -Port 443

# Monitor WebSocket connections in logcat
adb logcat | Select-String "WebSocket|connected to"
```

#### ✅ Status:
- ✅ URL configured in env.properties
- ✅ BuildConfig properly reads from env.properties
- ✅ Both WebSocket endpoints use correct URL
- ✅ HTTPS/WSS (secure) is enforced
- ✅ Fallback to default if env var missing

---

### ❓ 4. WebSocket Connect Actually Being Called?

**Status:** ⚠️ **IMPLEMENTED BUT DEPENDS ON PRECONDITIONS**

#### WebSocket Connection Flow:

```
AppViewModel.registerOrResumeDevice()
    ↓
[Check if remoteToken exists]
    ├─ YES → connectWs() called
    └─ NO  → Log warning & skip
    ↓
ConnectionManager.connect(remoteToken)
    ↓
WebSocketDataSourceImpl.connect(url)
    ↓
client.webSocket(url) { ... }  ✅ Ktor WebSocket Client
```

#### Code References:

**1. Entry Point (AppViewModel.kt):**
```kotlin
private fun connectWs(): Job = viewModelScope.launch {
    val remoteToken = preference.get(AppConstant.REMOTE_TOKEN, null)
    if (!remoteToken.isNullOrBlank()) {
        connectionManager.connect(remoteToken)  // ← CALLED HERE
    } else {
        Log.w(TAG, "No remote token available, skipping connection")
    }
}
```

**2. Connection Manager (ConnectionManager.kt):**
```kotlin
suspend fun connect(token: String) {
    try {
        websocketDataSource.connect(wsUrl)  // ← CALLS WEBSOCKET
    } catch (e: Exception) {
        _connectionState.value = ConnectionState.ERROR
        handleReconnect()
    }
}
```

**3. WebSocket Implementation (WebSocketDataSourceImpl.kt):**
```kotlin
override suspend fun connect(url: String) {
    try {
        client.webSocket(url) {  // ← KTOR WEBSOCKET CLIENT
            sesh = this
            println("connected to $url")  // ← LOG OUTPUT
            startHeartbeat()
            
            while (isActive) {
                val frame = incoming.receive()
                when (frame) {
                    is Frame.Text -> { ... }
                    is Frame.Binary -> { ... }
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Connection failed", e)
    }
}
```

#### Log Indicators (What to Look For):

```
✅ Connected successfully:
   "connected to wss://kiosk.mugshot.dev/remote-control-ws"
   "WebSocket: Frame received"
   "Heartbeat: sent ping"

⚠️ Connection issues:
   "No remote token available, skipping connection"
   "Connection failed: ..."
   "Timeout waiting for heartbeat response"
   "WebSocket: closed by server"
```

#### Remote Control Connection:

**From SettingsView.kt (Screen Capture Flow):**
```kotlin
val deviceId = prefs.getString(AppConstant.REMOTE_ID, "")
val deviceToken = prefs.getString(AppConstant.REMOTE_TOKEN, "")

if (deviceId.isNotEmpty() && deviceToken.isNotEmpty()) {
    Log.d("SettingsView", "Starting remote control with: $relayServerUrl")
    remoteControlViewModel.startRemoteControl(
        context = context,
        deviceId = deviceId,
        authToken = deviceToken,
        relayServerUrl = relayServerUrl  // ← CONNECTION INITIATED
    )
}
```

**From RemoteControlViewModel.kt:**
```kotlin
fun startRemoteControl(
    context: Context,
    deviceId: String,
    authToken: String,
    relayServerUrl: String
) {
    _connectionStatus.value = ConnectionStatus.CONNECTING
    
    remoteControlClient.connect(relayServerUrl, authToken, deviceId)
    // ↓
    // RemoteControlWebSocketClient.connect() → connection established
}
```

#### Preconditions for WebSocket to Connect:

| Condition | Status | Impact |
|-----------|--------|--------|
| Token in SharedPreferences | ❓ Pending | **CRITICAL** - Must exist |
| Device ID in SharedPreferences | ❓ Pending | **CRITICAL** - Must exist |
| Internet/WiFi Connected | ❓ Device dependent | **CRITICAL** - No network = no connection |
| Relay Server Reachable | ✅ Configured | Should be OK (kiosk.mugshot.dev) |
| BuildConfig Variables Set | ✅ Confirmed | OK via env.properties |
| Permissions Granted | ✅ Declared | INTERNET permission present |

---

## 📋 Checklist - Action Items

### Before Testing WebSocket Connection:

```
□ Device connected via USB to computer
□ ADB debugging enabled on device
□ WiFi/Internet connected on device
□ APK built and installed: app-debug.apk
□ Token entered in Settings screen: "DISPLAY-XXX"
□ Relay server accessible (Test-NetConnection check)
```

### To Verify Each Component:

#### ✅ Test 1: APK Code
```powershell
# Build APK
./gradlew build

# Check if APK created
if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") { 
    Write-Host "✅ APK exists"
}
```

#### ❓ Test 2: SharedPreferences
```powershell
# Install APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch app
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity

# Check SharedPreferences
adb shell dumpsys preferences com.kiosktouchscreendpr.cosmic | Select-String "token|device_id"
```

#### ✅ Test 3: Relay URL
```powershell
# Test DNS resolution
nslookup kiosk.mugshot.dev

# Test connectivity
Test-NetConnection -ComputerName kiosk.mugshot.dev -Port 443 -WarningAction Ignore

# Expected: TcpTestSucceeded = True
```

#### ❓ Test 4: WebSocket Connect
```powershell
# Monitor logs (follow from app start)
adb logcat -c
adb logcat | Select-String "RemoteControl|WebSocket|connected|Heartbeat"

# Navigate to Settings → Remote Control
# Check logcat for:
#   "Starting remote control with: wss://kiosk.mugshot.dev/..."
#   "connected to wss://kiosk.mugshot.dev..."
#   OR error messages
```

---

## 🔍 Troubleshooting Guide

### Issue: "No remote token available, skipping connection"

**Root Cause:** `AppConstant.REMOTE_TOKEN` not in SharedPreferences

**Solution:**
1. Open Settings screen
2. Tap "Register Remote Control"
3. Or manually enter token in TextField
4. Check logcat: should see "Remote registered" message

---

### Issue: "Connection failed" or Timeout

**Root Cause:** 
- Relay server not reachable
- Wrong URL in env.properties
- Network connectivity issue

**Debug:**
```powershell
# Check URL in APK
adb shell dumpsys package com.kiosktouchscreendpr.cosmic | Select-String "WS_URL"

# Test connectivity
Test-NetConnection -ComputerName kiosk.mugshot.dev -Port 443

# Check device network
adb shell netstat | Select-String "kiosk.mugshot.dev"
```

---

### Issue: WebSocket connects but no frames received

**Root Cause:**
- Backend server not broadcasting
- Token mismatch
- Protocol version incompatible

**Debug:**
```powershell
# Detailed WebSocket logs
adb logcat -E "WebSocket|Frame|heartbeat" -v threadtime

# Test server response with curl/PowerShell
curl -i -N -H "Connection: Upgrade" https://kiosk.mugshot.dev/remote-control-ws
```

---

## 📚 Key Files Reference

| File | Purpose | Status |
|------|---------|--------|
| `env.properties` | Config (URLs, passwords) | ✅ Ready |
| `build.gradle.kts` | Build config | ✅ Ready |
| `AndroidManifest.xml` | Permissions & services | ✅ Ready |
| `WebSocketDataSourceImpl.kt` | WebSocket client | ✅ Implemented |
| `RemoteControlWebSocketClient.kt` | Remote control relay | ✅ Implemented |
| `SettingsViewModel.kt` | Token management | ✅ Implemented |
| `SettingsView.kt` | Settings UI | ✅ Implemented |
| `AppViewModel.kt` | Main app logic | ✅ Implemented |

---

## 📞 Summary

| Point | Status | Blockers |
|-------|--------|----------|
| **APK Code** | ✅ YES | None - ready to build |
| **Token in SharedPrefs** | ❓ CONDITIONAL | Must be entered via UI or API |
| **Relay URL** | ✅ YES | Correctly configured |
| **WebSocket Called** | ✅ YES | Only if token + network ready |

### Next Steps:
1. **Build APK** → `./gradlew build`
2. **Install APK** → `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. **Open Settings** → Tap "Remote Control" button
4. **Enter token** → Type or scan token (e.g., "DISPLAY-001")
5. **Monitor logs** → `adb logcat | grep -E "RemoteControl|WebSocket|connected"`
6. **Verify connection** → Should see "connected to wss://..." in logs

---

**Generated:** 2026-02-04  
**Status:** Ready for testing
