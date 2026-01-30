# 🚀 Cosmic Kiosk - Installation Quick Reference

## ✅ The Correct Command

**From project root directory:**

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

❌ **WRONG:** `adb install -r app-debug.apk` (file not in current directory)
✅ **RIGHT:** `adb install -r app\build\outputs\apk\debug\app-debug.apk`

---

## 🎯 Easy Methods to Install

### Method 1: Use Helper Script (Easiest)
```powershell
.\install.ps1
```
This automatically finds the APK and installs it.

### Method 2: Full Command
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Method 3: With Verification
```powershell
.\install-apk.ps1
```
This checks everything and gives detailed output.

---

## 📍 APK Location

The APK is built to:
```
app\build\outputs\apk\debug\app-debug.apk
```

**Full path:**
```
C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app\app\build\outputs\apk\debug\app-debug.apk
```

---

## 🔄 Complete Workflow

### First Time Setup
```powershell
# 1. Build the APK
.\gradlew.bat assembleDebug

# 2. Install on device
.\install.ps1

# 3. View logs
.\view-logs.ps1
```

### After Code Changes
```powershell
# Rebuild and install
.\gradlew.bat assembleDebug
.\install.ps1
```

### Quick Reinstall (APK already built)
```powershell
.\install.ps1
```

---

## ❌ Common Errors & Solutions

### Error: "failed to stat app-debug.apk: No such file or directory"
**Cause:** APK filename/path is wrong

**Solution:**
```powershell
# Use correct path:
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Or use script:
.\install.ps1
```

### Error: "APK not found"
**Cause:** Haven't built the APK yet

**Solution:**
```powershell
.\gradlew.bat assembleDebug
```

### Error: "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
**Cause:** Old version needs to be removed first

**Solution:**
```powershell
adb uninstall com.kiosktouchscreendpr.cosmic
.\install.ps1
```

### Error: "no devices/emulators found"
**Cause:** No Android device connected

**Solution:**
```powershell
# Check devices
adb devices

# Reconnect if needed
adb kill-server
adb start-server
adb devices
```

---

## 📋 All Available Scripts

**Simple & Quick:**
- `.\install.ps1` - Quick install with status
- `.\view-logs.ps1` - View live logs

**Detailed:**
- `.\install-apk.ps1` - Install with full verification
- `.\setup-adb.ps1` - Setup ADB PATH

**Build:**
- `.\gradlew.bat assembleDebug` - Build debug APK
- `.\gradlew.bat assembleRelease` - Build release APK

---

## 🎮 Device Management Commands

```powershell
# List connected devices
adb devices

# Install APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Uninstall app
adb uninstall com.kiosktouchscreendpr.cosmic

# View logs
adb logcat | Select-String "cosmic"

# Stop app
adb shell am force-stop com.kiosktouchscreendpr.cosmic

# Start app
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity

# Check if installed
adb shell pm list packages | Select-String "cosmic"
```

---

## 💡 Pro Tips

### Create PowerShell Alias
Add to your PowerShell profile for faster access:

```powershell
# Open profile
notepad $PROFILE

# Add these lines:
Set-Alias cosmic-install "C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app\install.ps1"
Set-Alias cosmic-logs "C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app\view-logs.ps1"

# Save and restart PowerShell

# Then use:
cosmic-install
cosmic-logs
```

### Build and Install in One Line
```powershell
.\gradlew.bat assembleDebug; if ($?) { .\install.ps1 }
```

### Full Dev Cycle
```powershell
.\gradlew.bat clean assembleDebug; .\install.ps1; .\view-logs.ps1
```

---

## 📊 File Structure

```
kiosk-touchscreen-app/
├── app/
│   └── build/
│       └── outputs/
│           └── apk/
│               ├── debug/
│               │   └── app-debug.apk          ← Debug APK here
│               └── release/
│                   └── app-release.apk        ← Release APK here
├── install.ps1                                 ← Quick install
├── install-apk.ps1                            ← Detailed install
├── view-logs.ps1                              ← Log viewer
└── gradlew.bat                                ← Build tool
```

---

## ✨ Remember

**Always use the correct path:**
```powershell
✅ adb install -r app\build\outputs\apk\debug\app-debug.apk
❌ adb install -r app-debug.apk
```

**Or just use the helper script:**
```powershell
.\install.ps1
```

---

**Need more help?** See:
- `ADB_FIXED.md` - Complete ADB guide
- `COMMANDS.md` - All commands reference
- `BUILD_SUCCESS.md` - Build instructions
