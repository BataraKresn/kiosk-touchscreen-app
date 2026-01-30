# Quick Start: Running Cosmic Kiosk on Android Devices

## ⚠️ IMPORTANT: OneDrive Compatibility Fix

**Your project is in OneDrive.** This causes Gradle cache corruption with error:
```
java.io.IOException: The cloud operation is invalid
```

### ✅ SOLUTION ALREADY APPLIED

We've added this to `gradle.properties`:
```properties
org.gradle.caching=false
```

**This fix is already done!** You can now build normally.

---

## 🚀 TL;DR - Get Started in 5 Minutes

### Step 1: Configure Environment
```powershell
Copy-Item env.example.properties env.properties
# Edit env.properties with your server details
notepad env.properties
```

### Step 2: Build APK
```powershell
# Build debug APK for testing
.\gradlew.bat assembleDebug

# Build release APK for deployment
.\gradlew.bat assembleRelease
```

The build will create:
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

### Step 3: Connect Device & Run
**Physical Device:**
```powershell
# Enable USB Debug in: Settings → Developer Options → USB Debugging
# Connect device via USB, then install:
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

**Android Emulator:**
```powershell
emulator -avd Pixel_5_API_33 &
# Wait 2-3 minutes for boot, then install:
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Step 4: Verify Installation
```powershell
# Check if app is installed
adb shell pm list packages | findstr "kiosktouchscreendpr"

# View live logs
adb logcat | findstr "cosmic"
```

App should launch in **full-screen kiosk mode** automatically!

---

## 📋 Environment Variables (env.properties)

Example for **Production (Mugshot):**
```properties
APP_PASSWORD=cosmic123
WS_URL=wss://kiosk.mugshot.dev/remote-control-ws
WEBVIEW_BASEURL=https://kiosk.mugshot.dev
```

Example for **Local Testing:**
```properties
APP_PASSWORD=test123
WS_URL=ws://192.168.1.100:8080
WEBVIEW_BASEURL=http://192.168.1.100:8080
```

---

## 🔧 Common Commands

```powershell
# Build with no cache
.\gradlew.bat assembleDebug --no-build-cache

# Install on device
adb install -r app\build\outputs\apk\debug\app-debug.apk

# View app logs
adb logcat | findstr "cosmic"

# List devices
adb devices

# Check device info
adb shell getprop ro.build.version.release
```

---

## 🎮 After Installation

1. **App launches in full-screen kiosk mode**
2. **WebView loads from WEBVIEW_BASEURL**
3. **WebSocket connects to WS_URL for remote commands**
4. **Idle timer returns to home after inactivity**

---

## ❌ Troubleshooting

| Problem | Solution |
|---------|----------|
| "The cloud operation is invalid" | Use `--no-build-cache` flag or move project out of OneDrive |
| WebView blank | Check WEBVIEW_BASEURL is correct and reachable |
| WebSocket won't connect | Verify WS_URL, check firewall/ports, verify certificate |
| Device not found | Enable USB Debug, check `adb devices` |

---

## 📞 Need Help?

See the full **DEMO_GUIDE.md** for complete documentation.
