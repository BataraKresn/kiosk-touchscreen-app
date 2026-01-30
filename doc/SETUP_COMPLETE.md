# 📱 Running Cosmic Kiosk on Android Devices - Complete Guide

## ✅ What We've Done For You

1. **Fixed OneDrive Gradle Cache Issue**
   - Added `org.gradle.caching=false` to `gradle.properties`
   - This prevents the "The cloud operation is invalid" error
   - Now you can build successfully!

2. **Created Complete Documentation**
   - `DEMO_GUIDE.md` - Full reference guide for all scenarios
   - `QUICK_START.md` - Quick 5-minute startup guide
   - This file - Setup and deployment summary

3. **Environment Already Configured**
   - `env.properties` is ready with production Mugshot settings
   - Just edit the values if you need to use a different server

---

## 🚀 How to Run on Android Devices (Step by Step)

### Phase 1: Build the App (2-5 minutes)

```powershell
cd C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app

# First time - clean build (5-10 minutes)
.\gradlew.bat clean assembleDebug

# Subsequent builds (2-3 minutes)
.\gradlew.bat assembleDebug
```

**Output:** `app\build\outputs\apk\debug\app-debug.apk` (debug version for testing)

### Phase 2: Connect Android Device

**For Physical Device:**
```powershell
# 1. Enable USB Debugging on phone:
#    Settings → About Phone → Build Number (tap 7 times)
#    → Back to Settings → System → Developer Options → USB Debugging ON
# 
# 2. Connect via USB cable
# 
# 3. Verify connection:
adb devices
# Should show: device123456  device
```

**For Android Emulator:**
```powershell
# 1. Create virtual device (one-time):
#    Android Studio → Device Manager → Create Device
#    Select: Pixel 5, API 33+, name: Cosmic_Demo
# 
# 2. Start emulator:
emulator -avd Cosmic_Demo

# 3. Wait 2-3 minutes for Android to boot
#    (You'll see home screen when ready)
```

### Phase 3: Install App

```powershell
# Install on device/emulator
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Verify installation
adb shell pm list packages | findstr "kiosktouchscreendpr"
# Should show: com.kiosktouchscreendpr.cosmic
```

### Phase 4: Launch & Test

```powershell
# View live logs
adb logcat | findstr "cosmic"

# You should see logs like:
# - "MainActivity: Starting app"
# - "WebSocket: Connecting to wss://..."
# - "WebView: Loading https://..."
```

**App will automatically:**
1. Launch in full-screen kiosk mode
2. Load WebView from `WEBVIEW_BASEURL`
3. Connect WebSocket to `WS_URL`
4. Return to home on idle (after configured timeout)

---

## 📋 Configure Environment (env.properties)

**Already configured for Mugshot production:**
```properties
APP_PASSWORD=cosmic123
WS_URL=wss://kiosk.mugshot.dev/remote-control-ws
WEBVIEW_BASEURL=https://kiosk.mugshot.dev
```

**For local/custom server, edit:**
```powershell
notepad env.properties
```

**Local example (for testing):**
```properties
APP_PASSWORD=test123
WS_URL=ws://192.168.1.100:8080
WEBVIEW_BASEURL=http://192.168.1.100:8080
```

---

## 🎯 Demo Demonstration Checklist

Before showing to stakeholders, verify:

- [ ] **Device boots and loads app** - Appears in full-screen mode
- [ ] **WebView shows content** - Page loads from WEBVIEW_BASEURL
- [ ] **WebSocket connects** - Check logs: `"WebSocket: Connected"`
- [ ] **Home button disabled** - Can't exit app (kiosk mode)
- [ ] **Idle timer works** - Device returns to home after inactivity
- [ ] **Offline handling** - Turn WiFi off, content still visible (cached)
- [ ] **Auto-reconnect** - Turn WiFi back on, WebSocket reconnects
- [ ] **Remote commands work** - Backend can send refresh command

---

## 🔄 Development Workflow

### Quick Iteration (Change Code → Test)

```powershell
# 1. Edit code in Android Studio
# 2. Build and run:
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 3. View logs:
adb logcat | findstr "cosmic"
```

### Or Use Android Studio GUI

1. Open project in Android Studio
2. Connect device → Select from dropdown
3. Click green ▶️ **Run** button
4. App builds and launches automatically
5. View logs in Logcat tab

---

## 🔧 Useful Commands Reference

```powershell
# ===== BUILD =====
.\gradlew.bat clean                     # Clean build artifacts
.\gradlew.bat assembleDebug             # Build debug APK
.\gradlew.bat assembleRelease           # Build release APK (signed)

# ===== INSTALL & RUN =====
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb uninstall com.kiosktouchscreendpr.cosmic             # Remove app

# ===== DEVICE COMMANDS =====
adb devices                             # List connected devices
adb shell pm list packages              # List installed apps
adb logcat                              # View all logs
adb logcat | findstr "cosmic"           # Filter app logs
adb logcat -c                           # Clear logs

# ===== DEBUGGING =====
adb shell getprop ro.build.version.release   # Android version
adb shell dumpsys display | findstr "mScreenState"
adb shell input keyevent 4              # Simulate back button

# ===== EMULATOR =====
emulator -list-avds                     # List virtual devices
emulator -avd Cosmic_Demo &             # Start emulator
adb emu kill                            # Stop all emulators
```

---

## ⚠️ Troubleshooting

### Build Fails: "The cloud operation is invalid"
**Solution:** Already fixed by adding `org.gradle.caching=false`

If still occurs:
```powershell
# Clean and rebuild
.\gradlew.bat clean assembleDebug --no-build-cache
```

### WebView Shows Blank Page
```powershell
# 1. Check WEBVIEW_BASEURL in env.properties
# 2. Verify backend server is running
# 3. Check from device browser: curl http://<url>
# 4. View logs:
adb logcat -s "WebView:*"
```

### WebSocket Won't Connect
```powershell
# 1. Verify WS_URL is correct
# 2. Check firewall allows port (usually 443 for WSS)
# 3. Verify certificate is valid (for WSS)
# 4. View logs:
adb logcat | findstr "WebSocket"

# 5. Test from host:
Test-NetConnection -ComputerName kiosk.mugshot.dev -Port 443
```

### Device Not Found
```powershell
# 1. Check connection
adb devices

# 2. Reconnect:
adb kill-server
adb start-server
adb devices

# 3. Enable USB Debug on device:
#    Settings → About Phone → Build Number (tap 7x)
#    → System → Developer Options → USB Debugging ON
```

### Gradle Cache Issues Still Occurring
```powershell
# Option 1: Disable cache (already done, but try again)
# Edit gradle.properties and confirm:
# org.gradle.caching=false

# Option 2: Clean everything
.\gradlew.bat clean
Remove-Item -Recurse -Force .gradle
Remove-Item -Recurse -Force app\build

# Option 3: Move project out of OneDrive (best solution)
# Copy entire folder to C:\dev\kiosk-touchscreen-app
# Project works much better outside OneDrive
```

---

## 📦 Build Outputs

### Debug APK (for testing)
- **Location:** `app/build/outputs/apk/debug/app-debug.apk`
- **Size:** ~10-15 MB
- **Build Time:** 2-5 minutes
- **Use:** Testing on devices, demos

### Release APK (for production)
- **Location:** `app/build/outputs/apk/release/app-release.apk`
- **Size:** ~5-8 MB (optimized)
- **Build Time:** 5-10 minutes
- **Use:** Distribution, deployment to production devices
- **Note:** Requires signing with keystore

---

## 🚀 Deployment Steps

### For Single Device Testing
```powershell
# 1. Build
.\gradlew.bat assembleDebug

# 2. Install
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 3. Verify
adb logcat | findstr "cosmic"
```

### For Production Deployment

```powershell
# 1. Build release APK
.\gradlew.bat assembleRelease

# 2. Sign APK (see BUILD_GUIDE.md for details)

# 3. Distribute:
#    - Email APK to technicians
#    - Host on server for download
#    - Use MDM solution for enterprise deployment

# 4. Install on device:
adb install -r app\build\outputs\apk\release\app-release.apk

# 5. Configure:
#    - Enable Device Admin
#    - Set as default launcher
#    - Configure power schedule
#    - Test all features
```

---

## 📞 Support & Documentation

### Quick Reference Files
- **QUICK_START.md** - 5-minute quick start
- **DEMO_GUIDE.md** - Comprehensive reference guide
- **BUILD_GUIDE.md** - APK signing and release configuration

### Key Technologies
- **Kotlin** - Programming language
- **Jetpack Compose** - Modern UI framework
- **Hilt** - Dependency injection
- **Ktor Client** - HTTP/WebSocket networking
- **Android WebView** - Embedded browser
- **Coroutines** - Async programming

### Helpful Links
- Android Studio: https://developer.android.com/studio
- Android Developer Docs: https://developer.android.com
- Kotlin: https://kotlinlang.org

---

## ✨ Summary

You now have everything needed to:
1. ✅ **Build the app** - `.\gradlew.bat assembleDebug`
2. ✅ **Install on devices** - `adb install -r app-debug.apk`
3. ✅ **Test functionality** - WebView, WebSocket, Kiosk mode
4. ✅ **Debug issues** - View logs, test connectivity
5. ✅ **Deploy to production** - Build release APK and distribute

**The OneDrive cache issue is fixed!** You can now build normally without errors.

Happy coding! 🎉
