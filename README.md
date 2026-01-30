# Cosmic Kiosk Android Application

<p align="center">
  <strong>Enterprise-grade Android Kiosk application for Digital Signage & Content Display</strong>
</p>

## 🎉 Quick Start

✅ **App Status:** Installed & Running  
✅ **Device:** RR8R309LDWL  
✅ **APK Size:** 23.2 MB  
✅ **Password:** `260224`

### 📱 Cara Pakai Sekarang

1. Masukkan password: **260224**
2. Tekan OK
3. Dashboard akan muncul dari https://kiosk.mugshot.dev

### 🔐 Ganti Password

1. Edit `env.properties`
2. Ubah `APP_PASSWORD=260224` ke password baru
3. Rebuild & reinstall

---

## 📋 Overview

Cosmic Kiosk adalah aplikasi Android native yang didesain khusus untuk digital signage dan kiosk display. Aplikasi ini menampilkan konten web dari Cosmic Media Streaming Platform dengan fitur kiosk mode lengkap, auto-refresh, dan remote control melalui WebSocket.

### Key Features

- ✅ **Kiosk Mode** - Full-screen lockdown dengan home screen replacement
- ✅ **WebView Integration** - Auto-play media, JavaScript enabled, DOM storage
- ✅ **Power Schedule** - Automated on/off scheduling untuk hemat energi
- ✅ **Auto-Reset Mechanism** - Kembali ke halaman utama setelah idle
- ✅ **Real-time Refresh** - WebSocket-based remote refresh dari server
- ✅ **Heartbeat Monitoring** - Device status tracking (online/offline)
- ✅ **Network Resilience** - Auto-retry dan reconnection pada network change
- ✅ **Battery Optimization** - Disabled untuk mencegah app termination
- ✅ **Boot Auto-start** - Automatically start on device boot
- ✅ **Foreground Service** - Keep alive dengan persistent notification

## 🏗️ Architecture

### Tech Stack

#### Core Framework
- **Kotlin 2.1.20** - Modern, type-safe programming language
- **Jetpack Compose** - Declarative UI framework
- **Compose BOM 2025.03.01** - Latest stable Compose libraries
- **Min SDK: 26** (Android 8.0 Oreo)
- **Target SDK: 35** (Android 15)

#### Dependency Injection
- **Hilt 2.55** - Compile-time DI framework by Google
- **Dagger Compiler** - Annotation processor
- **Hilt Navigation Compose** - Integration dengan Compose Navigation

#### Networking & Communication
- **Ktor Client 3.0.1** - Multiplatform HTTP client
  - `ktor-client-core` - Core functionality
  - `ktor-client-cio` - Coroutine-based I/O engine
  - `ktor-client-okhttp` - OkHttp engine
  - `ktor-client-websockets` - WebSocket support
  - `ktor-client-content-negotiation` - Content negotiation
  - `ktor-client-logging` - Network logging
  - `ktor-serialization-kotlinx-json` - JSON serialization

#### Asynchronous Programming
- **Kotlin Coroutines 1.9.0** - Structured concurrency
- **Flow** - Reactive streams
- **StateFlow** - State management
- **SharedFlow** - Event broadcasting

#### Serialization
- **Kotlinx Serialization 1.7.3** - Type-safe JSON parsing

#### Navigation
- **Compose Navigation 2.8.0-alpha10** - Type-safe navigation

#### UI Components
- **Material 3** - Modern Material Design components
- **Material Icons Extended** - Full icon set
- **AndroidView** - WebView integration dengan Compose

### Architecture Pattern

```
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  ┌──────────────────────────────────────────┐  │
│  │  Composables (UI)                        │  │
│  │  - HomeView.kt                           │  │
│  │  - AuthView.kt                           │  │
│  │  - SettingsView.kt                       │  │
│  └──────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────┐  │
│  │  ViewModels                              │  │
│  │  - HomeViewModel                         │  │
│  │  - AppViewModel                          │  │
│  │  - AuthViewModel                         │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                      │
                      │ Use Cases
                      ▼
┌─────────────────────────────────────────────────┐
│               Domain Layer                       │
│  ┌──────────────────────────────────────────┐  │
│  │  Use Cases                               │  │
│  │  - RemoteRefreshUseCase                  │  │
│  │  - HeartbeatUseCase                      │  │
│  │  - AuthUseCase                           │  │
│  └──────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────┐  │
│  │  Models & Entities                       │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                      │
                      │ Repository Interface
                      ▼
┌─────────────────────────────────────────────────┐
│                Data Layer                        │
│  ┌──────────────────────────────────────────┐  │
│  │  Repositories                            │  │
│  │  - RemoteRefreshRepositoryImpl           │  │
│  │  - HeartbeatRepositoryImpl               │  │
│  └──────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────┐  │
│  │  Data Sources                            │  │
│  │  - RemoteRefreshDataSourceImpl (Ktor)   │  │
│  │  - WebSocketDataSourceImpl (Ktor WS)    │  │
│  │  - Preference (SharedPreferences)        │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                      │
                      │ Network/Storage
                      ▼
┌─────────────────────────────────────────────────┐
│            Core Infrastructure                   │
│  ┌──────────────────────────────────────────┐  │
│  │  Utils                                   │  │
│  │  - ConnectivityObserver                  │  │
│  │  - AlarmScheduler                        │  │
│  │  - WakeService                           │  │
│  └──────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────┐  │
│  │  Receivers                               │  │
│  │  - BootReceiver                          │  │
│  │  - AlarmReceiver                         │  │
│  │  - CosmicAdminReceiver                   │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### Design Principles

1. **MVVM (Model-View-ViewModel)** - Clear separation of concerns
2. **Clean Architecture** - Domain/Data/Presentation layers
3. **Unidirectional Data Flow** - State flows down, events flow up
4. **Repository Pattern** - Abstract data sources
5. **Dependency Injection** - Loose coupling, testability
6. **Single Source of Truth** - StateFlow for state management

## 🚀 Configuration

### Environment Variables

Configuration file: `env.properties`

```properties
# Authentication password untuk login
APP_PASSWORD=your_password_here

# WebSocket URL untuk refresh mechanism
WS_URL=wss://kiosk.mugshot.dev

# Base URL untuk WebView
WEBVIEW_BASEURL=https://kiosk.mugshot.dev
```

**⚠️ Important:** File `env.properties` tidak masuk git. Copy dari `env.example.properties` terlebih dahulu.

### Build Configuration

Defined in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.kiosktouchscreendpr.cosmic"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    
    buildConfigField("String", "APP_PASSWORD", "\"${envProperties["APP_PASSWORD"]}\"")
    buildConfigField("String", "WS_URL", "\"${envProperties["WS_URL"]}\"")
    buildConfigField("String", "WEBVIEW_BASEURL", "\"${envProperties["WEBVIEW_BASEURL"]}\"")
}
```

## 🎯 Quick Commands

```powershell
# Build APK (OneDrive-safe method)
.\ps1_clis_power_shell\build.ps1

# Install APK to device
.\ps1_clis_power_shell\install.ps1

# Launch app on device
.\ps1_clis_power_shell\launch-app.ps1

# View live logs
.\ps1_clis_power_shell\debug-live.ps1

# Force build (if issues)
.\ps1_clis_power_shell\force-build.ps1

# Check errors
.\ps1_clis_power_shell\check-errors.ps1
```

---

## 📚 Documentation

📁 **Semua dokumentasi lengkap ada di folder [doc/](doc/)** - Lihat [PROJECT_STRUCTURE.md](doc/PROJECT_STRUCTURE.md) untuk struktur lengkap.

### 🚀 Getting Started
| File | Purpose |
|------|---------|
| [QUICK_START.md](doc/QUICK_START.md) | Panduan cepat mulai |
| [SETUP_COMPLETE.md](doc/SETUP_COMPLETE.md) | Setup completion checklist |

### 🔧 Build & Deploy
| File | Purpose |
|------|---------|
| [BUILD_SUCCESS.md](doc/BUILD_SUCCESS.md) | Panduan build APK |
| [INSTALL_GUIDE.md](doc/INSTALL_GUIDE.md) | Panduan instalasi |
| [FIX_BUILD_ERROR.md](doc/FIX_BUILD_ERROR.md) | Fix OneDrive build errors |

### 🐛 Debug & Troubleshoot
| File | Purpose |
|------|---------|
| [DEBUG_GUIDE.md](doc/DEBUG_GUIDE.md) | Panduan debugging lengkap |
| [README_DEBUG.md](doc/README_DEBUG.md) | Debug quick reference |
| [COMMANDS.md](doc/COMMANDS.md) | Useful commands |

### 🔌 Backend Integration
| File | Purpose |
|------|---------|
| [BACKEND_INTEGRATION.md](doc/BACKEND_INTEGRATION.md) | Integrasi dengan backend |
| [BACKEND_API_REQUIRED.md](doc/BACKEND_API_REQUIRED.md) | Backend API requirements |
| [TOKEN_GUIDE.md](doc/TOKEN_GUIDE.md) | Token management |
| [SOLUTION_TOKEN_404.md](doc/SOLUTION_TOKEN_404.md) | Token 404 solutions |

### 🛠️ Tools & Setup
| File | Purpose |
|------|---------|
| [ADB_FIXED.md](doc/ADB_FIXED.md) | Setup ADB |
| [PASSWORD_FIXED.md](doc/PASSWORD_FIXED.md) | Password troubleshooting |
| [PLAY_PROTECT_FIX.md](doc/PLAY_PROTECT_FIX.md) | Google Play Protect issues |

---

## 🔧 Build & Deploy

### Prerequisites

- **Android Studio** Hedgehog atau lebih baru
- **JDK 11** atau lebih baru
- **Android SDK 35** (Android 15)
- **Gradle 8.11.1**

### Build APK

```powershell
# Debug Build
.\ps1_clis_power_shell\build.ps1

# Release Build
.\gradlew.bat assembleRelease

# Force Build (fix OneDrive issues)
.\ps1_clis_power_shell\force-build.ps1
```

### Install to Device

```powershell
# Via PowerShell script
.\ps1_clis_power_shell\install-apk.ps1

# Manual ADB
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 🐛 Troubleshooting

### Video Loading Issue (Spinning Loading)

**Root Cause:** Backend JavaScript `setInterval(() => displayScreen(data), 60000)` reload semua DOM termasuk video setiap 60 detik.

**Solution (Backend):**
```javascript
// ❌ BAD: Always reload
setInterval(() => {
    displayScreen(data);
}, 60000);

// ✅ GOOD: Only reload if schedule changed
let currentScheduleId = data.schedule_id;
setInterval(() => {
    fetch('/api/current-schedule?display_id=XXX')
        .then(r => r.json())
        .then(newData => {
            if (newData.schedule_id !== currentScheduleId) {
                currentScheduleId = newData.schedule_id;
                displayScreen(newData);
            }
        });
}, 60000);
```

**Logs menunjukkan:**
- MediaCodec cycling setiap 5-12 detik
- Video restart loop karena DOM reload
- APK code sudah oke, tidak perlu fix

### Build Errors (OneDrive)

```powershell
# Option 1: Skip clean
.\gradlew.bat assembleDebug --no-daemon --no-build-cache

# Option 2: Force build
.\ps1_clis_power_shell\force-build.ps1

# Option 3: Move to local disk
robocopy . C:\dev\kiosk-touchscreen-app /E
cd C:\dev\kiosk-touchscreen-app
```

### Device Not Detected

```powershell
.\ps1_clis_power_shell\setup-adb.ps1
```

---

## 📱 Key Components

### 1. WebView Configuration

**File:** `HomeView.kt`

```kotlin
WebView(context).apply {
    settings.apply {
        javaScriptEnabled = true              // Enable JS execution
        domStorageEnabled = true              // Enable localStorage
        allowFileAccess = true                // File access
        allowContentAccess = true             // Content provider access
        mediaPlaybackRequiresUserGesture = false  // ⭐ Auto-play media
        useWideViewPort = true                // Viewport meta tag
        loadWithOverviewMode = true           // Zoom to fit
        userAgentString = AppConstant.USER_AGENT  // Desktop Chrome UA
    }
}
```

**Key Features:**
- ✅ Auto-play video/audio tanpa user interaction
- ✅ Full JavaScript support untuk web apps
- ✅ Local storage untuk persistence
- ✅ Custom User Agent (Desktop Chrome)
- ✅ Error handling dengan custom error page

### 2. WebSocket Integration

**File:** `RemoteRefreshDataSourceImpl.kt`

```kotlin
// WebSocket connection untuk remote refresh
client.webSocket(urlString = wsUrl) {
    for (frame in incoming) {
        when (frame) {
            is Frame.Text -> {
                val message = Json.decodeFromString<RefreshMessage>(frame.readText())
                if (message.token == deviceToken) {
                    emit(RefreshRes.Triggered(message.token))
                }
            }
        }
    }
}
```

**Features:**
- Real-time refresh dari server
- Token-based targeting
- Automatic reconnection
- Error recovery

### 3. Network Monitoring

**File:** `ConnectivityObserver.kt`

```kotlin
// Monitor network state changes
connectivityManager.registerDefaultNetworkCallback(object : NetworkCallback() {
    override fun onAvailable(network: Network) {
        _status.value = ConnectivityObserver.Status.Available
    }
    
    override fun onLost(network: Network) {
        _status.value = ConnectivityObserver.Status.Lost
    }
})
```

**Features:**
- Real-time network state monitoring
- Auto-retry pada network change
- Graceful degradation

### 4. Power Scheduling

**File:** `AlarmScheduler.kt`

```kotlin
// Schedule device power on/off
fun schedulePowerOn(hour: Int, minute: Int)
fun schedulePowerOff(hour: Int, minute: Int)
```

**Features:**
- Exact alarm scheduling
- Wake device dari sleep
- Persistent across reboots

### 5. Idle Timer

**File:** `HomeViewModel.kt`

```kotlin
// Auto-reset to home after idle timeout
private fun startInactivityTimer() {
    inactivityTimerJob?.cancel()
    inactivityTimerJob = viewModelScope.launch {
        delay(timeout * 60_000L)  // Convert minutes to ms
        _resetEvent.emit(Unit)    // Trigger reset
    }
}
```

**Features:**
- Configurable timeout
- Reset on user interaction
- Return to home URL

## 🔒 Permissions & Security

### Required Permissions

```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Scheduling -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- Foreground Service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Power Management -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- System Events -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Audio Control -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### Security Features

- ✅ **ProGuard Enabled** - Code obfuscation untuk release build
- ✅ **No Sensitive Data in Code** - All configs dari `env.properties`
- ✅ **Device Admin** - Full device control untuk kiosk mode
- ✅ **Minimal Permissions** - Only necessary permissions requested

## 🔧 Build & Deploy

### Prerequisites

- **Android Studio** Hedgehog atau lebih baru
- **JDK 11** atau lebih baru
- **Android SDK 35** (Android 15)
- **Gradle 8.11.1**

### Setup Development Environment

```bash
# 1. Clone repository
git clone <repository-url>
cd kiosk-touchscreen-dpr-app

# 2. Copy environment configuration
cp env.example.properties env.properties

# 3. Edit env.properties dengan domain yang benar
nano env.properties

# Update values:
# APP_PASSWORD=your_password
# WS_URL=wss://kiosk.mugshot.dev
# WEBVIEW_BASEURL=https://kiosk.mugshot.dev

# 4. Sync project dengan Gradle
./gradlew sync
```

### Build APK

#### Debug Build (Development)

```bash
# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build (Production)

```bash
# Clean previous builds
./gradlew clean

# Build release APK dengan ProGuard
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

**Note:** Release build akan menggunakan ProGuard untuk optimasi dan obfuscation.

### Install to Device

```bash
# Via ADB
adb install app/build/outputs/apk/release/app-release.apk

# Force reinstall (overwrite existing)
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## 📊 Performance & Optimization

### Build Optimizations

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true           // Code shrinking
        isDebuggable = false             // Disable debug
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### WebView Optimizations

- **Hardware Acceleration** - Enabled by default
- **Cache Strategy** - DOM storage + app cache
- **Progressive Loading** - Load images on-demand
- **Zoom Controls** - Disabled untuk cleaner UI

### Network Optimizations

- **Connection Pooling** - Ktor default (HTTP/2)
- **Keep-Alive** - Persistent connections
- **Compression** - Automatic GZIP
- **Retry Logic** - Exponential backoff

## 🐛 Known Issues & Solutions

### ✅ Fixed Issues

- [x] **ANR on network disconnect** - Fixed dengan proper coroutine scope
- [x] **WebSocket not reconnecting** - Added reconnection logic
- [x] **Reload after network change** - Implemented retry mechanism

### 📝 TODO List

- [ ] **VNC Server Integration** - Remote screen sharing untuk troubleshooting
- [ ] **Analytics Integration** - Track device usage dan performance
- [ ] **OTA Updates** - Automatic app updates
- [ ] **Multi-language Support** - Internationalization

## 📚 API Integration

### Backend Requirements

Aplikasi ini berkomunikasi dengan **Cosmic Media Streaming Platform** (Laravel):

**Required Endpoints:**

1. **Authentication**
   - `POST /api/auth/token` - Login dengan device token

2. **Display Content**
   - `GET /display/{token}` - WebView content endpoint

3. **WebSocket**
   - `wss://domain.com` - Real-time refresh mechanism

### WebSocket Message Format

```json
{
  "action": "refresh",
  "token": "device-token-here"
}
```

### Environment URLs

**Production:**
- WebView: `https://kiosk.mugshot.dev`
- WebSocket: `wss://kiosk.mugshot.dev`

**Development:**
- WebView: `http://localhost:8080`
- WebSocket: `ws://localhost:8080`

## 🔍 Additional Resources

### PowerShell Scripts (ps1_clis_power_shell/)
- `build.ps1` - Build APK dengan OneDrive-safe method
- `install-apk.ps1` - Install APK ke device
- `launch-app.ps1` - Launch app on device
- `debug-live.ps1` - Live debugging dengan filter
- `force-build.ps1` - Aggressive build fix untuk OneDrive issues
- `check-errors.ps1` - Quick error check
- `view-logs.ps1` - View logs dengan filtering
- `troubleshoot.ps1` - Quick diagnosis tool
- `setup-adb.ps1` - Setup ADB environment

### Documentation (doc/)
Complete documentation untuk semua aspek development, troubleshooting, dan deployment.

---

**Developed with ❤️ for Cosmic Media Streaming Platform**

**Last Updated:** January 31, 2026