# 📁 Project Structure

Project structure yang terorganisir untuk Cosmic Kiosk Android Application.

## 🗂️ Root Directory

```
kiosk-touchscreen-app/
├── 📱 App Source Code
│   ├── app/                          # Main application module
│   │   ├── src/main/java/            # Kotlin source files
│   │   ├── src/main/res/             # Resources (layouts, drawables, etc)
│   │   ├── build.gradle.kts          # App-level build configuration
│   │   └── build/outputs/apk/        # Generated APK files
│   │
│   ├── gradle/                       # Gradle wrapper files
│   ├── build.gradle.kts              # Project-level build config
│   ├── settings.gradle.kts           # Project settings
│   ├── gradlew                       # Gradle wrapper (Unix)
│   └── gradlew.bat                   # Gradle wrapper (Windows)
│
├── 📝 Configuration
│   ├── env.properties                # Environment config (password, URLs)
│   ├── env.example.properties        # Example configuration template
│   ├── local.properties              # Local SDK path
│   └── gradle.properties             # Gradle settings
│
├── 📚 Documentation (doc/)
│   ├── QUICK_START.md                # Quick start guide
│   ├── BUILD_SUCCESS.md              # Build instructions
│   ├── INSTALL_GUIDE.md              # Installation guide
│   ├── DEBUG_GUIDE.md                # Debugging guide
│   ├── FIX_BUILD_ERROR.md            # OneDrive build error solutions
│   ├── BACKEND_INTEGRATION.md        # Backend API integration
│   ├── TOKEN_GUIDE.md                # Token management
│   ├── ADB_FIXED.md                  # ADB setup guide
│   ├── COMMANDS.md                   # Useful commands reference
│   ├── DEMO_GUIDE.md                 # Demo & presentation guide
│   ├── PASSWORD_FIXED.md             # Password troubleshooting
│   ├── PLAY_PROTECT_FIX.md           # Google Play Protect issues
│   ├── SOLUTION_TOKEN_404.md         # Token 404 error solutions
│   ├── SETUP_COMPLETE.md             # Setup completion checklist
│   ├── README_DEBUG.md               # Debug quick reference
│   ├── README_DOCUMENTATION.md       # Documentation index
│   ├── README_FINAL_ARCHIVE.md       # Archived final README
│   ├── BACKEND_API_REQUIRED.md       # Backend API requirements
│   └── PROJECT_STRUCTURE.md          # This file
│
├── 🛠️ PowerShell Scripts (ps1_clis_power_shell/)
│   ├── build.ps1                     # Build APK (OneDrive-safe)
│   ├── force-build.ps1               # Aggressive build fix
│   ├── install-apk.ps1               # Install APK to device
│   ├── install.ps1                   # Install with auto-detection
│   ├── launch-app.ps1                # Launch app on device
│   ├── debug-live.ps1                # Live debugging with filters
│   ├── view-logs.ps1                 # View filtered logs
│   ├── check-errors.ps1              # Quick error check
│   ├── check-password.ps1            # Verify password config
│   ├── troubleshoot.ps1              # Quick diagnosis tool
│   ├── setup-adb.ps1                 # Setup ADB environment
│   ├── build-and-sign.ps1            # Build & sign APK
│   ├── build-sign-install.ps1        # Build, sign & install
│   ├── install-apk-fixed.ps1         # Install with fixes
│   ├── monitor-cms-fetch.ps1         # Monitor CMS API calls
│   └── test-backend-api.ps1          # Test backend connectivity
│
├── 📊 Other Files
│   ├── README.md                     # Main project README
│   ├── .gitignore                    # Git ignore rules
│   ├── screen.png                    # Screenshot/logo
│   └── build-output.log              # Build log output
│
└── 🔒 Generated/Ignored
    ├── .gradle/                      # Gradle cache (ignored)
    ├── .idea/                        # Android Studio config (ignored)
    ├── .kotlin/                      # Kotlin compiler cache (ignored)
    └── build/                        # Build outputs (ignored)
```

---

## 📝 File Descriptions

### Configuration Files

| File | Purpose |
|------|---------|
| `env.properties` | Environment configuration (password, WebSocket URL, Base URL) |
| `env.example.properties` | Template for env.properties (safe to commit) |
| `local.properties` | Android SDK path (auto-generated) |
| `gradle.properties` | Gradle settings (JVM args, caching) |
| `build.gradle.kts` | Project-level Gradle build configuration |
| `app/build.gradle.kts` | App module Gradle build configuration |
| `settings.gradle.kts` | Project structure settings |

### Key Source Directories

| Directory | Content |
|-----------|---------|
| `app/src/main/java/` | Kotlin source code (ViewModels, UI, Repositories) |
| `app/src/main/res/` | Android resources (layouts, strings, drawables) |
| `app/src/main/AndroidManifest.xml` | App manifest (permissions, components) |
| `app/build/outputs/apk/` | Generated APK files (debug/release) |

### Documentation Categories

| Category | Files |
|----------|-------|
| **Getting Started** | QUICK_START.md, SETUP_COMPLETE.md |
| **Build & Deploy** | BUILD_SUCCESS.md, INSTALL_GUIDE.md, FIX_BUILD_ERROR.md |
| **Debugging** | DEBUG_GUIDE.md, README_DEBUG.md, COMMANDS.md |
| **Integration** | BACKEND_INTEGRATION.md, BACKEND_API_REQUIRED.md |
| **Troubleshooting** | PASSWORD_FIXED.md, SOLUTION_TOKEN_404.md, PLAY_PROTECT_FIX.md |
| **Reference** | TOKEN_GUIDE.md, ADB_FIXED.md, DEMO_GUIDE.md |

### PowerShell Script Categories

| Category | Scripts |
|----------|---------|
| **Building** | build.ps1, force-build.ps1, build-and-sign.ps1 |
| **Installing** | install-apk.ps1, install.ps1, build-sign-install.ps1 |
| **Debugging** | debug-live.ps1, view-logs.ps1, check-errors.ps1 |
| **Testing** | test-backend-api.ps1, monitor-cms-fetch.ps1, check-password.ps1 |
| **Utilities** | launch-app.ps1, setup-adb.ps1, troubleshoot.ps1 |

---

## 🎯 Quick Access

### Frequently Used Files

```powershell
# Configuration
.\env.properties

# Main README
.\README.md

# Quick Start
.\doc\QUICK_START.md

# Build APK
.\ps1_clis_power_shell\build.ps1

# Debug
.\ps1_clis_power_shell\debug-live.ps1
```

### APK Location

```
app\build\outputs\apk\debug\app-debug.apk          # Debug APK
app\build\outputs\apk\release\app-release.apk      # Release APK
```

---

## 🔄 Workflow

### 1. First Time Setup
```
1. Read: README.md
2. Setup: doc/QUICK_START.md
3. Configure: env.properties
4. Build: ps1_clis_power_shell/build.ps1
5. Install: ps1_clis_power_shell/install-apk.ps1
```

### 2. Development Cycle
```
1. Edit code in Android Studio
2. Build: ps1_clis_power_shell/build.ps1
3. Install: ps1_clis_power_shell/install-apk.ps1
4. Debug: ps1_clis_power_shell/debug-live.ps1
5. Fix issues: doc/DEBUG_GUIDE.md
```

### 3. Troubleshooting
```
1. Check: ps1_clis_power_shell/troubleshoot.ps1
2. View logs: ps1_clis_power_shell/view-logs.ps1
3. Read: doc/DEBUG_GUIDE.md
4. Fix build: doc/FIX_BUILD_ERROR.md
```

---

## 📊 Statistics

- **Total Documentation Files:** 18 files
- **Total PowerShell Scripts:** 16 scripts
- **Configuration Files:** 4 files
- **Main README:** 1 file (consolidated)

---

## ✅ Organization Benefits

### Before Reorganization
```
❌ Documentation files scattered in root
❌ PowerShell scripts mixed with config
❌ Duplicate README files
❌ Hard to find specific guides
```

### After Reorganization
```
✅ All documentation in doc/ folder
✅ All scripts in ps1_clis_power_shell/ folder
✅ Single comprehensive README.md
✅ Clear categorization and structure
✅ Easy to navigate and maintain
```

---

**Last Updated:** January 31, 2026  
**Version:** 2.0 (Reorganized Structure)
