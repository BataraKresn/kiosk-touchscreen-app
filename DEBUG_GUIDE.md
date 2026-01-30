# 🔍 DEBUG LIVE - Panduan Lengkap Debugging di Device

## 🎯 Masalah: App Sukses Install Tapi Tidak Ada Aktivitas

Ini adalah panduan lengkap untuk debug kenapa app tidak terlihat/berjalan di device setelah install sukses.

---

## 📋 Quick Diagnosis Scripts

### 1️⃣ Launch App & View Logs (Lengkap)
```powershell
.\debug-live.ps1
```
**Ini akan:**
- ✅ Cek device terkoneksi
- ✅ Cek app terinstall
- ✅ Launch app di device
- ✅ Tampilkan live logs dengan warna
- ✅ Auto-detect errors

### 2️⃣ Manual Launch (Cepat)
```powershell
.\launch-app.ps1
```
**Ini akan:**
- ✅ Cek device & app
- ✅ Force stop app lama
- ✅ Launch app baru

### 3️⃣ Check Errors (Jika Crash)
```powershell
.\check-errors.ps1
```
**Ini akan:**
- ✅ Tampilkan semua error logs
- ✅ Cek app crashes
- ✅ Tampilkan WebView errors

---

## 🔧 Manual Debugging Commands

### Cek Device Terkoneksi
```powershell
adb devices
```
**Expected output:**
```
List of devices attached
RR8R309LDWL    device
```

❌ **Jika tidak muncul:**
```powershell
# Reconnect device
adb kill-server
adb start-server
adb devices

# Atau untuk WiFi device:
adb connect 192.168.x.x:5555
```

### Cek App Terinstall
```powershell
adb shell pm list packages | Select-String "cosmic"
```
**Expected output:**
```
package:com.kiosktouchscreendpr.cosmic
```

❌ **Jika tidak muncul:**
```powershell
# Install dulu
.\install.ps1
```

### Launch App Secara Manual
```powershell
# Stop app yang sedang berjalan
adb shell am force-stop com.kiosktouchscreendpr.cosmic

# Start app
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity
```

**Expected output:**
```
Starting: Intent { cmp=com.kiosktouchscreendpr.cosmic/.MainActivity }
```

### Cek App Berjalan atau Tidak
```powershell
adb shell ps | Select-String "cosmic"
```
**Jika muncul = app running ✅**
**Jika tidak = app crashed atau tidak running ❌**

---

## 📊 View Live Logs

### Method 1: Filtered Logs (Recommended)
```powershell
adb logcat -v time MainActivity:D HomeViewModel:D WebView:D WebSocket:* AndroidRuntime:E *:S
```

### Method 2: App Logs Only
```powershell
adb logcat | Select-String "cosmic|MainActivity|WebView|WebSocket" -CaseSensitive:$false
```

### Method 3: Errors Only
```powershell
adb logcat *:E *:F
```

### Method 4: Clear & Watch Fresh
```powershell
adb logcat -c                    # Clear old logs
adb logcat -v time              # Watch new logs with timestamp
```

---

## 🐛 Common Issues & Solutions

### Issue 1: App Tidak Muncul di Device Screen

**Symptoms:** Install sukses, tapi layar device kosong/blank

**Debug:**
```powershell
# 1. Cek app running
adb shell ps | Select-String "cosmic"

# 2. Launch manual
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity

# 3. Lihat logs
adb logcat MainActivity:* *:E
```

**Common Causes:**
- App crashed on startup → Check logs untuk error
- App minimize/background → Launch ulang
- WebView blank → Check WEBVIEW_BASEURL

### Issue 2: App Crash Saat Startup

**Symptoms:** App install tapi langsung crash

**Debug:**
```powershell
# Lihat crash logs
adb logcat -d AndroidRuntime:E *:S | Select-Object -Last 100
```

**Common Causes:**
- ❌ `env.properties` values salah
- ❌ Backend server tidak accessible
- ❌ Device tidak ada internet
- ❌ Missing permissions

**Solution:**
```powershell
# 1. Cek env.properties
notepad env.properties

# 2. Rebuild & reinstall
.\gradlew.bat clean assembleDebug
.\install.ps1

# 3. Launch & watch logs
.\debug-live.ps1
```

### Issue 3: WebView Blank/Putih

**Symptoms:** App berjalan tapi tampilan putih/blank

**Debug:**
```powershell
# Check WebView errors
adb logcat chromium:* WebView:* *:S
```

**Common Causes:**
- ❌ WEBVIEW_BASEURL salah atau tidak accessible
- ❌ SSL certificate invalid
- ❌ Device tidak ada internet
- ❌ Backend server down

**Solution:**
```powershell
# Test URL dari device
adb shell am start -a android.intent.action.VIEW -d "https://kiosk.mugshot.dev"

# Cek koneksi internet
adb shell ping -c 3 8.8.8.8

# Lihat WebView console errors
# Chrome desktop: chrome://inspect
```

### Issue 4: WebSocket Tidak Connect

**Symptoms:** App berjalan, WebView muncul, tapi WebSocket error di logs

**Debug:**
```powershell
adb logcat | Select-String "WebSocket|WS_URL"
```

**Common Causes:**
- ❌ WS_URL salah
- ❌ Backend WebSocket server tidak running
- ❌ Firewall block WebSocket port
- ❌ Certificate issue (untuk wss://)

**Solution:**
```powershell
# 1. Verify WS_URL di env.properties
notepad env.properties

# 2. Test dari PC
Test-NetConnection -ComputerName kiosk.mugshot.dev -Port 443

# 3. Rebuild & reinstall
.\gradlew.bat assembleDebug
.\install.ps1
```

### Issue 5: Device Disconnected

**Symptoms:** `adb devices` tidak tampilkan device

**Debug:**
```powershell
adb devices
```

**For WiFi Device:**
```powershell
# Reconnect
adb connect <device-ip>:5555

# Example:
adb connect 192.168.1.100:5555
```

**For USB Device:**
```powershell
# Restart ADB
adb kill-server
adb start-server

# Cek lagi
adb devices

# Jika masih tidak muncul:
# 1. Cabut & pasang ulang USB
# 2. Re-enable USB Debugging di device
# 3. Trust this computer lagi
```

---

## 📱 Check Device Info

### Screen Status
```powershell
adb shell dumpsys display | Select-String "mScreenState"
```

### Battery & Power
```powershell
adb shell dumpsys battery
```

### Device Specs
```powershell
# Android version
adb shell getprop ro.build.version.release

# Device model
adb shell getprop ro.product.model

# Screen resolution
adb shell wm size

# Screen density
adb shell wm density
```

### Storage Space
```powershell
adb shell df
```

### Running Apps
```powershell
adb shell ps
```

---

## 🎬 Simulate User Actions

### Press Buttons
```powershell
adb shell input keyevent 4      # Back button
adb shell input keyevent 3      # Home button
adb shell input keyevent 26     # Power button
adb shell input keyevent 82     # Menu button
```

### Tap Screen
```powershell
# Tap di koordinat x=500, y=800
adb shell input tap 500 800
```

### Swipe
```powershell
# Swipe dari (100,500) ke (100,100) dalam 300ms
adb shell input swipe 100 500 100 100 300
```

### Input Text
```powershell
adb shell input text "Hello"
```

---

## 🔄 Full Debug Workflow

### Step 1: Verify Setup
```powershell
# Cek device
adb devices

# Cek app installed
adb shell pm list packages | Select-String "cosmic"

# Cek env.properties
notepad env.properties
```

### Step 2: Launch App
```powershell
# Force stop old instance
adb shell am force-stop com.kiosktouchscreendpr.cosmic

# Launch fresh
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity
```

### Step 3: Monitor Logs
```powershell
# Clear old logs
adb logcat -c

# Watch live (with color coding)
.\debug-live.ps1
```

### Step 4: Check What You See

**✅ App tampil normal:**
```
- Full screen kiosk mode
- WebView loading dari WEBVIEW_BASEURL
- Logs: "WebSocket: Connected"
- Logs: "WebView: Loaded successfully"
```

**❌ App blank/crash:**
```powershell
# Check errors
.\check-errors.ps1

# Common fixes:
# 1. Fix env.properties
# 2. Rebuild & reinstall
# 3. Check backend server
```

---

## 📸 Take Screenshot from Device

```powershell
# Take screenshot
adb shell screencap /sdcard/screen.png

# Pull to PC
adb pull /sdcard/screen.png screenshot.png

# View screenshot.png untuk lihat apa yang di device
```

---

## 🎥 Record Screen

```powershell
# Start recording (max 180 seconds)
adb shell screenrecord /sdcard/demo.mp4

# Stop dengan Ctrl+C

# Pull video
adb pull /sdcard/demo.mp4 demo.mp4
```

---

## ⚡ Quick Commands Cheat Sheet

```powershell
# === DIAGNOSTIC ===
.\debug-live.ps1              # All-in-one debug & launch
.\launch-app.ps1              # Quick launch
.\check-errors.ps1            # Check for errors

# === DEVICE ===
adb devices                   # List devices
adb shell pm list packages    # List apps
adb shell ps                  # Running processes

# === APP CONTROL ===
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity  # Start
adb shell am force-stop com.kiosktouchscreendpr.cosmic              # Stop
adb uninstall com.kiosktouchscreendpr.cosmic                        # Uninstall

# === LOGS ===
adb logcat                    # All logs
adb logcat -c                 # Clear logs
adb logcat *:E                # Errors only
adb logcat | Select-String "cosmic"  # App logs

# === REINSTALL ===
.\gradlew.bat assembleDebug   # Build
.\install.ps1                 # Install
```

---

## 💡 Pro Tips

### Persistent Logs to File
```powershell
# Save logs to file
adb logcat > logs_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt
```

### Monitor Specific Activity
```powershell
# Watch MainActivity only
adb logcat MainActivity:D *:S
```

### Check Network Activity
```powershell
adb shell netstat
```

### Remote Debugging WebView
1. Pastikan WebView debugging enabled (sudah di code)
2. Buka Chrome di PC: `chrome://inspect`
3. Lihat device & open WebView
4. Debug seperti browser biasa

---

## 🆘 Still Not Working?

### Collect Full Debug Info
```powershell
# Save everything untuk analisa
$timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'

# Device info
adb shell dumpsys > "device_info_$timestamp.txt"

# App info
adb shell dumpsys package com.kiosktouchscreendpr.cosmic > "app_info_$timestamp.txt"

# Logs
adb logcat -d > "logs_$timestamp.txt"

# Screenshot
adb shell screencap /sdcard/screen.png
adb pull /sdcard/screen.png "screenshot_$timestamp.png"
```

### Check Backend Server
```powershell
# Test dari PC
Test-NetConnection -ComputerName kiosk.mugshot.dev -Port 443

# Test WebView URL
Start-Process "https://kiosk.mugshot.dev"

# Test WebSocket URL
# (need wscat atau browser console)
```

---

## ✅ Success Indicators

**App berjalan dengan baik jika:**

1. ✅ `adb shell ps | Select-String "cosmic"` → menampilkan process
2. ✅ Logs: `MainActivity: onCreate`
3. ✅ Logs: `WebView: Loading https://kiosk.mugshot.dev`
4. ✅ Logs: `WebSocket: Connected successfully`
5. ✅ Device screen: Full-screen dashboard
6. ✅ No crash logs di AndroidRuntime

**Test dengan:**
```powershell
# Launch & watch
.\debug-live.ps1

# Dalam 10 detik pertama harus ada:
# - MainActivity onCreate
# - WebView onPageStarted
# - WebSocket connecting
# - WebView onPageFinished
```

---

**Need help?** Run: `.\debug-live.ps1` and send output screenshots!
