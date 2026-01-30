# 🚀 Cosmic Kiosk - Quick Command Reference

## Essential Commands (Copy & Paste Ready)

### View Live Logs
```powershell
.\view-logs.ps1
```

### Reinstall App
```powershell
.\install-apk.ps1
```

### Check Connected Devices
```powershell
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
```

### Rebuild and Install
```powershell
.\gradlew.bat assembleDebug; .\install-apk.ps1
```

### Stop App on Device
```powershell
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.kiosktouchscreendpr.cosmic
```

### Start App on Device
```powershell
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity
```

### Uninstall App
```powershell
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" uninstall com.kiosktouchscreendpr.cosmic
```

### View Only Errors
```powershell
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat *:E | Select-String "cosmic"
```

### Clear Logs
```powershell
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -c
```

### Device Info
```powershell
# Android version
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell getprop ro.build.version.release

# Screen resolution
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell wm size

# Battery status
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell dumpsys battery
```

---

## After Restarting PowerShell

You can use `adb` directly (PATH is configured):

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat | Select-String "cosmic"
adb uninstall com.kiosktouchscreendpr.cosmic
```

---

## Workflow Shortcuts

### Full Development Cycle
```powershell
# Clean → Build → Install → View Logs
.\gradlew.bat clean assembleDebug; .\install-apk.ps1; .\view-logs.ps1
```

### Quick Update
```powershell
# Build → Install
.\gradlew.bat assembleDebug; .\install-apk.ps1
```

### Restart App
```powershell
# Force stop then start
$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb shell am force-stop com.kiosktouchscreendpr.cosmic
& $adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity
```

---

## Troubleshooting One-Liners

### Reconnect Device
```powershell
$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"; & $adb kill-server; & $adb start-server; & $adb devices
```

### Full Reinstall
```powershell
$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"; & $adb uninstall com.kiosktouchscreendpr.cosmic; .\install-apk.ps1
```

### Check If App Is Installed
```powershell
& "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell pm list packages | Select-String "cosmic"
```

---

## File Locations

**Scripts:**
- `.\setup-adb.ps1` - Setup ADB PATH
- `.\install-apk.ps1` - Install APK with verification
- `.\view-logs.ps1` - Live log viewer

**Build Output:**
- `app\build\outputs\apk\debug\app-debug.apk` - Debug APK
- `app\build\outputs\apk\release\app-release.apk` - Release APK

**Documentation:**
- `ADB_FIXED.md` - Complete ADB guide ⭐
- `BUILD_SUCCESS.md` - Build instructions
- `QUICK_START.md` - Quick start guide
- `DEMO_GUIDE.md` - Full reference

---

## PowerShell Aliases (Optional)

Add to your PowerShell profile for even faster commands:

```powershell
# Open profile
notepad $PROFILE

# Add these lines:
$adb = "C:\Users\IT\AppData\Local\Android\Sdk\platform-tools\adb.exe"
function Install-CosmicKiosk { cd C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app; .\install-apk.ps1 }
function Watch-CosmicLogs { cd C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app; .\view-logs.ps1 }
function Build-Cosmic { cd C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app; .\gradlew.bat assembleDebug }

# Then use:
# Install-CosmicKiosk
# Watch-CosmicLogs
# Build-Cosmic
```

---

**Quick Help:** See `ADB_FIXED.md` for detailed explanations and troubleshooting.
