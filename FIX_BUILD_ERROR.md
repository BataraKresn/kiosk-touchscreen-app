# 🔧 FIX: AccessDeniedException - OneDrive Build Error

## ❌ Error Yang Terjadi

```
java.nio.file.AccessDeniedException: 
C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app\app\build\...
```

**Root Cause:** OneDrive sedang sync files dan mengunci (lock) build directories, sehingga Gradle tidak bisa write files.

---

## ✅ SOLUSI CEPAT (Gunakan Build Script)

### Method 1: Gunakan Build Script (RECOMMENDED)

```powershell
.\build.ps1
```

Script ini otomatis:
- ✅ Stop all Gradle daemons
- ✅ Kill Java processes
- ✅ Clean build directories
- ✅ Build dengan OneDrive-safe flags

---

## 🛠️ SOLUSI MANUAL

### Option A: Pause OneDrive Sementara

1. **Klik icon OneDrive di system tray** (kanan bawah)
2. **Klik Settings** (⚙️)
3. **Klik "Pause syncing"** → Pilih **"2 hours"**
4. **Run build:**
   ```powershell
   .\gradlew.bat clean assembleDebug
   ```

### Option B: Build Dengan Flags Khusus

```powershell
# Stop daemon dulu
.\gradlew.bat --stop

# Clean manual
Remove-Item -Recurse -Force app\build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue

# Build tanpa daemon & cache
.\gradlew.bat clean assembleDebug --no-daemon --no-build-cache
```

### Option C: Close Android Studio

Jika Android Studio terbuka, close dulu karena bisa lock files:

1. Close Android Studio
2. Stop Gradle:
   ```powershell
   .\gradlew.bat --stop
   ```
3. Build:
   ```powershell
   .\build.ps1
   ```

---

## 🔄 Gradle Settings Sudah Di-Update

File `gradle.properties` sudah di-update dengan OneDrive-safe settings:

```properties
org.gradle.caching=false              # Disable cache
org.gradle.daemon=false               # Disable daemon
org.gradle.configureondemand=false    # Disable on-demand config
org.gradle.workers.max=2              # Limit workers
```

---

## 🚀 Quick Fix Commands

```powershell
# === METHOD 1: Use build script ===
.\build.ps1

# === METHOD 2: Manual commands ===
# Stop everything
.\gradlew.bat --stop
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force

# Clean
Remove-Item -Recurse -Force app\build -ErrorAction SilentlyContinue

# Build
.\gradlew.bat clean assembleDebug --no-daemon --no-build-cache

# === METHOD 3: Full clean rebuild ===
.\gradlew.bat clean
Remove-Item -Recurse -Force .gradle
.\gradlew.bat assembleDebug --no-daemon
```

---

## ⚡ Mengapa Ini Terjadi?

**OneDrive File Locking Issues:**

1. **OneDrive Sync Active**
   - OneDrive terus sync files di background
   - Lock files saat sedang upload
   - Gradle tidak bisa write ke locked files

2. **Gradle Daemon**
   - Gradle daemon keep files open
   - OneDrive try to sync → conflict
   - Result: AccessDeniedException

3. **Build Cache**
   - Cache files di `.gradle/` directory
   - OneDrive lock cache files
   - Gradle can't update cache

---

## 💡 Best Practices Untuk OneDrive

### Permanent Solution: Exclude Build Directories

Tambahkan ke OneDrive exclusions:

1. Buka OneDrive Settings
2. Pilih "Advanced"
3. Exclude folders:
   - `app\build`
   - `.gradle`
   - `build`

### Alternative: Move Project to Local Disk

Jika masalah terus terjadi, pindahkan project ke local disk (non-OneDrive):

```powershell
# Copy project ke C:\dev atau D:\projects
robocopy C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app C:\dev\kiosk-touchscreen-app /E

# Build dari lokasi baru
cd C:\dev\kiosk-touchscreen-app
.\gradlew.bat assembleDebug
```

---

## 📊 Troubleshooting Checklist

Jika build masih gagal, cek:

- [ ] OneDrive sync paused?
- [ ] Android Studio closed?
- [ ] Gradle daemon stopped? (`.\gradlew.bat --stop`)
- [ ] Java processes killed? (`Get-Process java | Stop-Process -Force`)
- [ ] Build directories cleaned?
- [ ] Using `--no-daemon --no-build-cache` flags?
- [ ] Run as Administrator? (right-click PowerShell → Run as Administrator)

---

## 🔍 Check Build Status

```powershell
# Check if APK exists
Test-Path "app\build\outputs\apk\debug\app-debug.apk"

# Check APK size
Get-Item "app\build\outputs\apk\debug\app-debug.apk" | Select-Object Length

# List build output
Get-ChildItem "app\build\outputs\apk" -Recurse
```

---

## ⚠️ Common Errors & Fixes

### Error: "Access Denied" saat clean
```powershell
# Close all programs accessing files
Get-Process | Where-Object {$_.Path -like "*kiosk*"} | Stop-Process -Force

# Try clean again
.\gradlew.bat clean --no-daemon
```

### Error: "Process cannot access the file"
```powershell
# Find what's locking the file
# Install: choco install handle (if needed)
# handle app\build

# Or just kill Java processes
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force
```

### Error: Build takes too long
```powershell
# Use build script with progress
.\build.ps1

# Or check progress manually
Get-Process -Name java | Select-Object CPU, WorkingSet
```

---

## 📱 After Successful Build

```powershell
# Install on device
.\install.ps1

# Or manual
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Enter password di device: 260224
```

---

## 🆘 Still Not Working?

### Last Resort Options:

1. **Restart Computer**
   - Closes all file locks
   - Fresh start for OneDrive

2. **Run as Administrator**
   ```powershell
   # Right-click PowerShell → Run as Administrator
   cd C:\Users\IT\OneDrive\Documents\KIOSK\kiosk-touchscreen-app
   .\build.ps1
   ```

3. **Temporarily Disable OneDrive**
   - Right-click OneDrive icon → Settings
   - Account → Unlink this PC
   - Build
   - Re-link PC

4. **Use Android Studio Instead**
   - Open project in Android Studio
   - Build → Make Project
   - Build → Build APK(s)

---

## ✅ Success Indicators

Build berhasil jika:
```
BUILD SUCCESSFUL in Xs
```

Dan APK created di:
```
app\build\outputs\apk\debug\app-debug.apk
```

---

## 📚 Related Documentation

- **BUILD_SUCCESS.md** - Build instructions
- **ADB_FIXED.md** - Device connection
- **DEBUG_GUIDE.md** - Debugging
- **README_DEBUG.md** - Troubleshooting

---

**TL;DR: Run `.\build.ps1` untuk build dengan OneDrive fixes!**
