# 🛡️ Google Play Protect Issue - Solution

## Problem
Google Play Protect memblokir aplikasi karena permission sensitif:
- `BIND_ACCESSIBILITY_SERVICE` - untuk remote input injection
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - untuk screen capture

## Quick Solution

### 1. Disable Google Play Protect (Temporary)

**Di Device:**
1. Buka **Play Store**
2. Tap **Profile icon** (pojok kanan atas)
3. Tap **Play Protect**
4. Tap **Settings** (gear icon)
5. **Matikan** "Scan apps with Play Protect"

### 2. Uninstall & Reinstall APK

```powershell
# Uninstall completely
adb uninstall com.kiosktouchscreendpr.cosmic

# Reinstall
adb install .\app\build\outputs\apk\release\app-release.apk

# Launch
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity
```

### 3. Trust This App

Saat muncul dialog "App blocked to protect your device":
1. Tap **More details**
2. Tap **Install anyway**

## Alternative: Build Without Remote Control

Jika tidak butuh remote control sementara waktu:

```kotlin
// AndroidManifest.xml - Comment out these permissions:
<!-- Remote Control Permissions -->
<!-- <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" /> -->
<!-- <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" /> -->
```

## Production Solution

Untuk production, aplikasi dengan permission ini harus:
1. **Publish ke Play Store** dengan deklarasi permission usage
2. **Whitelisting** di organization (untuk enterprise deployment)
3. **Gunakan Android Enterprise** (Work Profile/Managed Device)

## Test Display CMS

Setelah Play Protect disabled:

```powershell
# Clear logs
adb logcat -c

# Monitor logs
adb logcat | findstr "DeviceApi|Display|CMS"

# Test fetch displays
# Tap "Ambil Display dari CMS" di Settings
```

## Why Play Protect Blocks?

Permission berikut dianggap "high risk":
- `ACCESSIBILITY_SERVICE` - bisa baca/inject semua input
- `MEDIA_PROJECTION` - bisa capture screen (privacy concern)
- Kombinasi keduanya = remote control capability

Google Play Protect otomatis block sideloaded apps (non-Play Store) dengan permission ini.
