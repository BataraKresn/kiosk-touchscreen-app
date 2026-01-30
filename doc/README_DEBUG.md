
# 🚨 APP INSTALL SUKSES TAPI TIDAK ADA AKTIVITAS? BACA INI!

## ⚡ SOLUSI CEPAT (Copy & Paste)

### Jalankan script ini SEKARANG:

```powershell
.\troubleshoot.ps1
```

Ini akan:
- ✅ Cek device connected
- ✅ Cek app installed
- ✅ Cek app running
- ✅ Launch app jika belum running
- ✅ Tampilkan errors jika ada

---

## 🎯 3 LANGKAH MUDAH

### LANGKAH 1: Cek Device & Launch App
```powershell
.\launch-app.ps1
```

### LANGKAH 2: Lihat Layar Device
**App harus muncul full-screen di device Anda!**

Jika TIDAK muncul, lanjut ke Langkah 3:

### LANGKAH 3: Debug dengan Logs
```powershell
.\debug-live.ps1
```
Ini akan tampilkan live logs berwarna. Lihat untuk:
- ❌ Error messages (warna merah)
- ✅ MainActivity onCreate (warna kuning)
- ✅ WebView loading (warna cyan)
- ✅ WebSocket connected (warna hijau)

---

## 🔍 MANUAL CHECK (Jika Scripts Tidak Jalan)

### 1. Cek Device Connected
```powershell
adb devices
```
**Harus muncul device Anda (contoh: RR8R309LDWL)**

❌ Jika tidak muncul:
```powershell
adb kill-server
adb start-server
adb devices
```

### 2. Cek App Installed
```powershell
adb shell pm list packages | Select-String "cosmic"
```
**Harus muncul: package:com.kiosktouchscreendpr.cosmic**

❌ Jika tidak muncul, install dulu:
```powershell
.\install.ps1
```

### 3. Launch App Manual
```powershell
adb shell am force-stop com.kiosktouchscreendpr.cosmic
adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity
```
**Setelah ini, lihat layar device Anda!**

### 4. Cek App Running
```powershell
adb shell ps | Select-String "cosmic"
```
**Jika muncul = app running ✅**
**Jika tidak = app crashed ❌, lihat logs:**

```powershell
adb logcat -d AndroidRuntime:E *:S | Select-Object -Last 50
```

### 5. Lihat Live Logs
```powershell
adb logcat | Select-String "cosmic|MainActivity|WebView|WebSocket"
```
Press Ctrl+C untuk stop.

---

## 📱 APA YANG HARUS TERLIHAT DI DEVICE?

### ✅ NORMAL (Berhasil):
- Full-screen tanpa navigation bar
- Dashboard dari https://kiosk.mugshot.dev
- Loading spinner kemudian content muncul
- Tidak bisa tekan home button (kiosk mode)

### ❌ MASALAH:
1. **Layar Blank/Putih**
   - Cek internet device
   - Cek WEBVIEW_BASEURL di env.properties
   - Run: `adb logcat chromium:* WebView:*`

2. **App Langsung Crash**
   - Cek env.properties values
   - Run: `.\check-errors.ps1`
   - Rebuild: `.\gradlew.bat clean assembleDebug`
   - Reinstall: `.\install.ps1`

3. **App Tidak Muncul**
   - Launch manual: `.\launch-app.ps1`
   - Atau: `adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity`

---

## 🎬 VIDEO RECORDING DEVICE SCREEN

Jika ingin record apa yang terjadi di device:

```powershell
# Start recording
adb shell screenrecord /sdcard/demo.mp4

# Tunggu beberapa detik, tekan Ctrl+C untuk stop

# Download video ke PC
adb pull /sdcard/demo.mp4 demo.mp4

# Buka demo.mp4 untuk lihat apa yang terjadi
```

---

## 📸 SCREENSHOT DEVICE

```powershell
# Ambil screenshot
adb shell screencap /sdcard/screen.png
adb pull /sdcard/screen.png screenshot.png

# Buka screenshot.png
```

---

## 🛠️ COMPLETE TROUBLESHOOTING WORKFLOW

### Workflow Lengkap dari Awal:

```powershell
# 1. Build APK
.\gradlew.bat assembleDebug

# 2. Check device
adb devices

# 3. Install APK  
.\install.ps1

# 4. Launch & debug
.\debug-live.ps1
```

**Dalam 10 detik, Anda harus lihat:**
- Di PowerShell: Logs MainActivity onCreate, WebView loading
- Di Device: App full-screen dengan dashboard

---

## 📋 SCRIPTS CHEAT SHEET

| Script | Fungsi |
|--------|--------|
| `.\troubleshoot.ps1` | Quick diagnostic |
| `.\launch-app.ps1` | Launch app cepat |
| `.\debug-live.ps1` | Launch + live logs berwarna |
| `.\check-errors.ps1` | Lihat semua errors |
| `.\install.ps1` | Install/reinstall app |
| `.\view-logs.ps1` | Live logs only |

---

## 🆘 MASIH TIDAK JALAN?

### Collect Info untuk Debug:

```powershell
# 1. Device info
adb devices > debug_info.txt
adb shell getprop >> debug_info.txt

# 2. App status
adb shell pm list packages | Select-String "cosmic" >> debug_info.txt
adb shell ps | Select-String "cosmic" >> debug_info.txt

# 3. Logs
adb logcat -d *:E >> debug_info.txt

# 4. Screenshot
adb shell screencap /sdcard/screen.png
adb pull /sdcard/screen.png screenshot.png

# Kirim debug_info.txt dan screenshot.png untuk analisa
```

---

## ✅ SUCCESS CHECKLIST

App berhasil jika:
- [ ] `adb devices` menampilkan device
- [ ] `.\launch-app.ps1` sukses launch
- [ ] Device screen tampil full-screen dashboard
- [ ] Logs: "MainActivity: onCreate"
- [ ] Logs: "WebView: Loaded"
- [ ] Logs: "WebSocket: Connected"
- [ ] Tidak ada error di logs

---

## 🔥 QUICK FIX COMMANDS

```powershell
# Device disconnect? Reconnect:
adb kill-server; adb start-server; adb devices

# App not launching? Force launch:
adb shell am force-stop com.kiosktouchscreendpr.cosmic; adb shell am start -n com.kiosktouchscreendpr.cosmic/.MainActivity

# App crashed? Check why:
adb logcat -d AndroidRuntime:E *:S | Select-Object -Last 50

# WebView blank? Check errors:
adb logcat -d chromium:E WebView:E *:S | Select-Object -Last 30

# Full reinstall:
adb uninstall com.kiosktouchscreendpr.cosmic; .\install.ps1

# Clean rebuild:
.\gradlew.bat clean assembleDebug; .\install.ps1
```

---

## 📚 BACA DOKUMENTASI LENGKAP

- **DEBUG_GUIDE.md** ⭐ - Panduan lengkap debugging (BACA INI DULU!)
- **ADB_FIXED.md** - Setup ADB
- **INSTALL_GUIDE.md** - Cara install yang benar
- **BUILD_SUCCESS.md** - Build instructions

---

## 🎯 MULAI SEKARANG

Copy paste command ini:

```powershell
.\troubleshoot.ps1
```

Kemudian:
1. Lihat output di PowerShell
2. Lihat layar device Anda
3. Jika masih ada masalah, run: `.\debug-live.ps1`

**App HARUS muncul di device screen dalam 5-10 detik!**

---

*Last updated: 2026-01-29*
*All scripts tested and working ✅*
