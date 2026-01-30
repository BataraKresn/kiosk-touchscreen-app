# 🚀 Demo Guide: Running Cosmic Kiosk on Android Devices

This guide covers how to run the Cosmic Kiosk Android app on physical devices or emulators **before building the APK**.

---

## 📋 Prerequisites

### Required Software
- ✅ **Android Studio** Hedgehog (2023.1.1) or newer
- ✅ **JDK 11** or newer (Project uses JDK 17)
- ✅ **Android SDK 26** (minimum) to SDK 35 (target)
- ✅ **Gradle 8.11.1** (included in project via wrapper)

### Hardware Options
- **Physical Device**: Android 8.0 (API 26) or higher
- **Android Emulator**: Android 8.0 or higher virtual device

### Network Requirements
- ✅ Device must connect to WiFi/Network
- ✅ Must have internet access for WebSocket connection
- ✅ Backend server must be running (Cosmic Media Streaming Platform)

---

## 🔧 Initial Setup (One-Time Configuration)

### Step 1: Clone & Open Project

```powershell
# Clone the repository
git clone <repository-url>
cd kiosk-touchscreen-app

# Open in Android Studio
# File → Open → Select this folder
```

### Step 2: Configure Environment Variables

```powershell
# Copy example configuration
Copy-Item env.example.properties env.properties

# Edit env.properties with your values
# You can use Notepad or any text editor
notepad env.properties
```

**File: `env.properties`** - Edit these values:

```properties
# Kiosk unlock password
APP_PASSWORD=your_password_here

# WebSocket URL for real-time refresh commands
WS_URL=wss://your-server.com/remote-control-ws

# Base URL for WebView content/dashboard
WEBVIEW_BASEURL=https://your-server.com
```

**Example for Production (Mugshot):**
```properties
APP_PASSWORD=cosmic123
WS_URL=wss://kiosk.mugshot.dev/remote-control-ws
WEBVIEW_BASEURL=https://kiosk.mugshot.dev
```

**Example for Local Testing:**
```properties
APP_PASSWORD=test123
WS_URL=ws://192.168.1.100:8080
WEBVIEW_BASEURL=http://192.168.1.100:8080
```

### Step 3: Build and Sync Dependencies

In Android Studio:
1. Click: **File** → **Sync Now**

Or use terminal to compile and download dependencies:
```powershell
# This will take 2-5 minutes on first run
.\gradlew.bat build
```

This downloads all dependencies and compiles the project.

---

## 📱 Option 1: Run on Physical Android Device

### Step 1: Connect Device via USB

1. **Enable Developer Mode on Device:**
   - Settings → About Phone
   - Tap "Build Number" 7 times rapidly
   - Back to Settings → System → Developer Options

2. **Enable USB Debugging:**
   - Settings → Developer Options → USB Debugging (toggle ON)

3. **Connect via USB Cable to Computer**
   - Trust the computer when prompted on device
   - Device should appear in Android Studio

### Step 2: Verify Device Connection

**In Android Studio:**
- View → Tool Windows → Device Manager
- Your device should appear with status "Connected"

**Or via Terminal:**
```powershell
# List all connected Android devices
adb devices

# Expected output: device123456 device
```

### Step 3: Run App from Android Studio

**Method 1: Use Green Run Button (Easiest)**
1. Select your device from dropdown (top toolbar)
2. Click the green ▶️ **Run** button
3. Wait for app to install and launch (30-60 seconds)

**Method 2: Terminal Command**
```powershell
# Install debug APK on connected device and run
.\gradlew.bat installDebug

# Or use adb directly
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Step 4: Monitor Live Logs

```powershell
# View real-time logs from the device
adb logcat | findstr "cosmic"

# Or in Android Studio: View → Tool Windows → Logcat
```

---

## 🖥️ Option 2: Run on Android Emulator

### Step 1: Create Virtual Device

**In Android Studio:**
1. Right-click in project → **Device Manager**
2. Click **Create Device**
3. Select: "Pixel 5" or "Pixel 6" (recommended)
4. Select: **API 33** (Android 13) or higher
5. Name: "Cosmic_Demo"
6. Click: **Finish**

### Step 2: Launch Emulator

```powershell
# List available emulators
emulator -list-avds

# Start emulator (replace Pixel_5_API_33 with your device name)
emulator -avd Pixel_5_API_33 &

# Wait for emulator to fully boot (2-3 minutes)
# You'll see the Android home screen when ready
```

**Or from Android Studio:**
1. Device Manager → Your device → ▶️ (play button)
2. Wait for emulator to fully boot

### Step 3: Run App on Emulator

**In Android Studio:**
1. Wait for emulator to fully boot (home screen visible)
2. Click ▶️ **Run** button
3. Select your emulator from device list
4. App will install and launch automatically

---

## 🎮 Testing the App (Interactive Demo)

### Initial Launch Behavior

```
✅ App starts in full-screen kiosk mode
✅ Home button disabled (can't exit easily)
✅ WebView loads content from WEBVIEW_BASEURL
✅ WebSocket connects to WS_URL for remote commands
```

### Test WebSocket Real-Time Refresh

```powershell
# From your backend server, send refresh command
# Message format (WebSocket):
{
  "action": "refresh",
  "timestamp": "2026-01-29T10:30:00Z"
}

# Device receives message and refreshes webpage in real-time
```

### Test Auto-Reset (Idle Timer)

```
1. Configure idle timeout (check MainActivity settings)
2. Wait for configured time without touching screen
3. App should return to home URL automatically
4. Check logs: adb logcat | findstr "idle"
```

### Test Network Resilience

```
1. Turn WiFi off on device
2. App should show "Offline" state or cached content
3. Turn WiFi back on
4. App should auto-reconnect within 5-10 seconds
```

### View Detailed Logs in Real-Time

```powershell
# Watch all cosmic app logs
adb logcat | findstr "cosmic"

# Watch specific component (MainActivity)
adb logcat -s "MainActivity:*"

# Watch WebView issues
adb logcat -s "WebView:*"

# Watch WebSocket connection
adb logcat -s "WebSocket:*"
```

---

## ⚙️ Common Debug Tasks

### Debug WebView Content Loading

```powershell
# Enable WebView verbose logging
adb shell setprop log.tag.WebView V

# Check what URL is loading
adb logcat | findstr "WebView"

# View JavaScript console errors
# Via Chrome: chrome://inspect (requires WebView debugging enabled)
```

### Check WebSocket Connection Status

```powershell
# Monitor WebSocket activity
adb logcat | findstr "WebSocket"

# Check network port connectivity
adb shell netstat | findstr "8080"

# Or on Windows from host:
# Test connection to server
Test-NetConnection -ComputerName kiosk.mugshot.dev -Port 443
```

### View Stored Preferences & Settings

```powershell
# Export SharedPreferences
adb shell dumpsys preferences com.kiosktouchscreendpr.cosmic

# Or access via Android Studio:
# View → Tool Windows → App Inspection → Database Inspector
```

### Enable Advanced Debug Logging

```powershell
# Edit MainActivity.kt to add debug logging
# Around onCreate() method:
if (BuildConfig.DEBUG) {
    Log.d("CosmicApp", "Debug mode enabled - Logging initialized")
}

# Rebuild and run: Ctrl+Shift+F10
```

---

## 🔄 Hot Reload & Fast Development

### Use Fast Rebuild During Development

**While app is running on device:**

```powershell
# Recompile only changed code (very fast - 10-20 seconds)
Ctrl + Shift + F10   # Rerun app
Ctrl + F9            # Rebuild

# Or use: Run → Run 'app'
```

### Faster Gradle Builds

In Android Studio:
1. **Settings** → **Experimental** → **Gradle**
2. Enable "Gradle-aware make"
3. Next run will be significantly faster

### No Need to Build APK During Testing

- During demo/development, just use **Run** (creates debug APK)
- Debug APK is 3-5x faster to build than release APK
- Don't run `assembleRelease` until final deployment

---

## 🐛 Troubleshooting

### "Device Not Found"

```powershell
# Check ADB connection status
adb devices

# Troubleshoot:
adb kill-server
adb start-server
adb devices

# On Windows, you may need USB drivers:
# Download from device manufacturer website
# Or use Android SDK Manager: SDK Platform Tools
```

### "Failed to Connect to WebSocket"

**Check in this order:**
1. Verify `WS_URL` in `env.properties` is correct
2. Device must have WiFi enabled and working
3. Test from device: Open Chrome browser and try visiting `WEBVIEW_BASEURL`
4. Check backend server is running and accessible
5. Check firewall on server allows WebSocket (usually port 443 for WSS)
6. View logs: `adb logcat | findstr "WebSocket"`

### "WebView Won't Load Content"

```powershell
# Check WEBVIEW_BASEURL is correct and reachable
# Device must have internet access

# View WebView errors:
adb logcat -s "WebView:*"

# Common issues:
# - HTTPS certificate not valid (self-signed?)
# - URL is incorrect
# - Backend server not running
# - Firewall blocking connection
```

### "App Crashes on Startup"

```powershell
# Get detailed crash log
adb logcat -s "AndroidRuntime:*"

# Look for exception and stack trace
# Common causes:
# - env.properties missing required values
# - WS_URL or WEBVIEW_BASEURL invalid format
# - Server not responding

# Rebuild with: .\gradlew.bat clean build
```

### "Gradle Sync/Build Fails"

```powershell
# Option 1: Clean cache
.\gradlew.bat clean

# Option 2: Full rebuild
.\gradlew.bat clean build

# Option 3: Invalidate Android Studio cache
# Android Studio: File → Invalidate Caches → Invalidate and Restart

# Option 4: Check Java version
# Must be JDK 11 or newer
java -version
```

### "Emulator Won't Start"

```powershell
# Check if another emulator already running
adb devices

# Kill all emulators
adb emu kill

# Try starting again with hardware acceleration
emulator -avd Pixel_5_API_33 -gpu auto

# Or create new device with less RAM:
# Device Manager → Create Device → Advanced Options → RAM 2GB
```

### "WebView Shows Blank Page"

```
1. Check WEBVIEW_BASEURL is accessible from device WiFi
2. Verify no firewall blocking connection
3. Check server is returning valid HTML
4. Try accessing URL from Chrome on device

# Debug logs:
adb logcat -s "WebView:*"
```

---

## 📊 Performance Tips for Demo

### Optimize Emulator Performance

```powershell
# Use hardware acceleration
emulator -avd Pixel_5_API_33 -gpu auto

# Save state (faster boot next time)
emulator -avd Pixel_5_API_33 -snapshot default

# Allocate more RAM/CPU
# Device Manager → Edit Device → Advanced → RAM: 4GB, CPU Cores: 4
```

### Quick Demo Setup

```
1. Don't clean build - just Run the app
2. Use physical device if possible (faster than emulator)
3. Pre-load the WebView content in browser before demo
4. Test WiFi connection before showing stakeholders
5. Have fallback demo video ready (just in case)
```

---

## ✅ Demo Checklist

**Before Showing to Stakeholders:**

- [ ] Device connected and app running latest code
- [ ] Tested offline behavior (WiFi turned off briefly)
- [ ] Manually tested WebSocket refresh (send command from server)
- [ ] Tested idle timer (let it sit 5 minutes)
- [ ] Verified environment variables correct (no placeholder values)
- [ ] Check logcat for any warnings/errors
- [ ] Power button works (device goes to sleep/wakes)
- [ ] Device has good WiFi signal
- [ ] Tested on landscape orientation (if applicable)
- [ ] Have backend server ready to demonstrate remote commands

---

## 📝 Quick Commands Reference

```powershell
# ============ PROJECT SETUP ============
.\gradlew.bat clean                     # Clean previous build
.\gradlew.bat build                     # Build and sync dependencies
.\gradlew.bat --version                 # Check Gradle version

# ============ INSTALL & RUN ============
.\gradlew.bat installDebug              # Install debug APK on device
adb install -r app\build\outputs\apk\debug\app-debug.apk

# In Android Studio:
# Ctrl + R   - Run app
# Ctrl + Shift + F10 - Rerun app
# F9         - Rebuild

# ============ DEVICE MANAGEMENT ============
adb devices                             # List connected devices
adb logcat                              # View all system logs
adb logcat | findstr "cosmic"           # Filter app logs
adb logcat -s "WebView:*"               # View WebView logs only

# Device input simulation:
adb shell input keyevent 4              # Press Back button
adb shell input keyevent 3              # Press Home button
adb shell input keyevent 82             # Press Menu button

# ============ DEBUGGING ============
adb shell getprop ro.build.version.release    # Android version
adb shell dumpsys display | findstr "mScreenState"  # Screen status
adb shell dumpsys battery                     # Battery status

# ============ CLEANUP ============
adb uninstall com.kiosktouchscreendpr.cosmic  # Remove app
adb emu kill                            # Stop all emulators
```

---

## 🎯 Next Steps After Successful Demo

### If Demo Goes Well:

1. **Build Release APK for Deployment:**
   ```powershell
   .\gradlew.bat assembleRelease
   # Output: app/build/outputs/apk/release/app-release.apk
   ```

2. **Install on Multiple Devices:**
   ```powershell
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

3. **Configure Kiosk Mode Lockdown:**
   - Enable Device Admin in Settings
   - Block home button access
   - Set as default launcher
   - Lock screen navigation

4. **Setup Power Scheduling (if needed):**
   - Configure on/off times
   - Test wake/sleep behavior
   - Verify auto-start on boot

### For Production Deployment:

1. Sign APK with production keystore
2. Test on target hardware (your specific display/tablet)
3. Configure network for corporate WiFi (enterprise setup if needed)
4. Setup remote monitoring and management
5. Create deployment guide for field technicians
6. Document troubleshooting procedures

---

## 📞 Support & Additional Resources

### Collect Debug Information for Support

```powershell
# Save full logs for analysis
adb logcat > logs_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt

# Device system info
adb shell dumpsys build > device_info.txt
adb shell dumpsys display > display_info.txt
```

### Quick Troubleshooting Table

| Problem | Check First | Solution |
|---------|-------------|----------|
| WebView blank | `WEBVIEW_BASEURL` correct | Verify server running, check firewall |
| WebSocket timeout | `WS_URL` correct | Check server port open, verify certificate |
| App crashes | `env.properties` filled | Check all values not empty, no quotes |
| Device offline | WiFi enabled | Check SSID, password, signal strength |
| Performance lag | Using emulator | Try physical device, clear app cache |
| Logcat not updating | Device selected | Check `adb devices`, reconnect USB |

### Related Documentation

- Android Studio: https://developer.android.com/studio/intro
- Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Android WebView: https://developer.android.com/guide/webapps/webview

---

**Happy Demoing! 🎉**

For APK signing, release configuration, and production deployment guides, see the main README.md.
