# 🔌 Backend Integration - Solusi Token Not Found

## ✅ STATUS: IMPLEMENTED

**Backend:** Auto-register + API register sudah diterapkan  
**Android App:** API call ke `/api/displays/register` sudah diimplementasikan  
**Date:** January 29, 2026

---

## ⚠️ Masalah

Token yang di-generate di app (`DISPLAY-001`) tidak ada di database CMS:
```
https://kiosk.mugshot.dev/display/DISPLAY-001 → 404 Not Found
```

## ✅ Solusi (3 Pilihan)

### **Solusi 1: Backend Auto-Register (Paling Mudah)** ⭐ RECOMMENDED

Di backend Laravel, buat controller untuk auto-register token yang belum ada:

```php
// File: app/Http/Controllers/DisplayController.php

public function display($token)
{
    // Cari device berdasarkan token
    $device = Device::where('token', $token)->first();
    
    // Jika device tidak ditemukan, buat otomatis
    if (!$device) {
        $device = Device::create([
            'token' => $token,
            'name' => 'Device ' . $token,
            'location' => 'Unknown',
            'status' => 'pending', // atau 'active'
            'created_at' => now()
        ]);
        
        Log::info("Auto-registered new device: $token");
    }
    
    // Load content untuk device ini (atau default content)
    $content = Content::where('device_id', $device->id)
                      ->where('is_active', true)
                      ->latest()
                      ->first();
    
    // Jika belum ada content, gunakan default
    if (!$content) {
        $content = Content::where('is_default', true)->first();
    }
    
    return view('kiosk.display', [
        'device' => $device,
        'content' => $content,
        'isNewDevice' => $device->wasRecentlyCreated
    ]);
}
```

**Database Migration:**

```php
// database/migrations/xxxx_create_devices_table.php
Schema::create('devices', function (Blueprint $table) {
    $table->id();
    $table->string('token')->unique();
    $table->string('name');
    $table->string('location')->nullable();
    $table->enum('status', ['active', 'inactive', 'pending'])->default('active');
    $table->timestamps();
});

Schema::create('contents', function (Blueprint $table) {
    $table->id();
    $table->foreignId('device_id')->nullable()->constrained()->onDelete('cascade');
    $table->text('html_content');
    $table->boolean('is_active')->default(true);
    $table->boolean('is_default')->default(false); // Default content untuk device baru
    $table->timestamps();
});
```

**Buat Default Content:**

```php
// database/seeders/ContentSeeder.php
Content::create([
    'device_id' => null,
    'html_content' => '
        <div style="text-align: center; padding: 50px; font-family: Arial;">
            <h1>🎉 Welcome to Cosmic Kiosk</h1>
            <p>Device belum ada content, silakan setup di CMS</p>
            <p>Token: <strong>{{ $device->token }}</strong></p>
        </div>
    ',
    'is_active' => true,
    'is_default' => true
]);
```

**✅ Keuntungan:**
- Device otomatis terdaftar saat pertama kali akses
- Tidak ada 404 error
- Admin bisa lihat list device yang terdaftar di CMS
- Admin tinggal assign content ke device tersebut

---

### **Solusi 2: App Register Token via API**

Di Android app, setelah save token, kirim request ke backend:

**Backend API:**

```php
// routes/api.php
Route::post('/devices/register', [DeviceController::class, 'register']);

// app/Http/Controllers/DeviceController.php
public function register(Request $request)
{
    $validated = $request->validate([
        'token' => 'required|string|unique:devices,token',
        'device_name' => 'nullable|string',
        'android_version' => 'nullable|string'
    ]);
    
    $device = Device::create([
        'token' => $validated['token'],
        'name' => $validated['device_name'] ?? 'Device ' . $validated['token'],
        'location' => 'Unknown',
        'android_version' => $validated['android_version'],
        'status' => 'active'
    ]);
    
    return response()->json([
        'success' => true,
        'device' => $device,
        'message' => 'Device registered successfully'
    ]);
}
```

**Android Code:**

```kotlin
// File: app/src/main/java/com/kiosktouchscreendpr/cosmic/data/api/DeviceApi.kt
interface DeviceApi {
    @POST("/api/devices/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<RegisterDeviceResponse>
}

data class RegisterDeviceRequest(
    val token: String,
    val device_name: String,
    val android_version: String
)

data class RegisterDeviceResponse(
    val success: Boolean,
    val message: String,
    val device: Device?
)
```

```kotlin
// Update SettingsViewModel.kt
private fun onSubmit() = viewModelScope.launch {
    // Save to SharedPreferences
    preferences.edit().apply {
        putString(AppConstant.TOKEN, _state.value.token)
        putString(AppConstant.TIMEOUT, _state.value.timeout)
        putString(AppConstant.POWER_OFF, formatTime(_state.value.powerOffTime))
        putString(AppConstant.POWER_ON, formatTime(_state.value.powerOnTime))
        apply()
    }

    // Register token ke backend
    try {
        val response = deviceApi.registerDevice(
            RegisterDeviceRequest(
                token = _state.value.token,
                device_name = android.os.Build.MODEL,
                android_version = android.os.Build.VERSION.RELEASE
            )
        )
        
        if (response.isSuccessful) {
            Log.d("Settings", "Token registered successfully")
        }
    } catch (e: Exception) {
        Log.w("Settings", "Failed to register token: ${e.message}")
        // Tidak perlu error, karena optional
    }

    // Schedule alarm & navigate
    _state.value.powerOffTime?.let { powerOff ->
        _state.value.powerOnTime?.let { powerOn ->
            scheduleAlarmInternal(powerOff, powerOn)
        }
    }
    _state.update { it.copy(isSuccess = true, errorMessage = null) }
}
```

---

### **Solusi 3: Token dari CMS (QR Code)**

Paling secure dan proper, tapi butuh implementasi QR Scanner di app.

**Flow:**
1. Admin di CMS create device → Generate token → Generate QR Code
2. User di app scan QR Code
3. Token otomatis terisi dan **pasti** sudah terdaftar di backend

**Backend CMS:**

```php
// Generate QR Code untuk token
use SimpleSoftwareIO\QrCode\Facades\QrCode;

public function generateQRCode($deviceId)
{
    $device = Device::findOrFail($deviceId);
    
    // Generate QR Code berisi token
    $qrCode = QrCode::size(300)->generate($device->token);
    
    return view('devices.qrcode', [
        'device' => $device,
        'qrCode' => $qrCode
    ]);
}
```

**Android App:**

```kotlin
// Butuh library CameraX + ML Kit Barcode
dependencies {
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
}
```

---

## 🎯 Rekomendasi

### **Untuk Development/Testing:**

**Gunakan Solusi 1** (Backend Auto-Register) - paling cepat dan simple!

```php
// Backend: app/Http/Controllers/DisplayController.php
public function display($token)
{
    $device = Device::firstOrCreate(
        ['token' => $token],
        [
            'name' => 'Device ' . $token,
            'location' => 'Unknown',
            'status' => 'active'
        ]
    );
    
    $content = Content::where('device_id', $device->id)
                      ->orWhere('is_default', true)
                      ->latest()
                      ->first();
    
    return view('kiosk.display', compact('device', 'content'));
}
```

### **Untuk Production:**

**Kombinasi Solusi 1 + 2:**
1. Backend auto-register (agar tidak 404)
2. App juga register via API (untuk kirim device info lengkap)
3. Admin di CMS bisa manage devices yang auto-registered

---

## 🎯 Implementasi Final (Production Ready)

### ✅ Yang Sudah Diterapkan:

#### **1. Backend Laravel (Auto-Register + API)**

**File:** `app/Http/Controllers/DisplayController.php`

```php
public function show($token)
{
    // Auto-register display baru jika belum ada
    $display = Display::firstOrCreate(
        ['token' => $token],
        ['name' => 'Display ' . $token]
    );
    
    // Cari schedule/screen yang aktif
    // Jika tidak ada, tampilkan halaman "unassigned"
}
```

**File:** `app/Http/Controllers/Api/DisplayRegistrationController.php`

```php
// Endpoint: POST /api/displays/register
public function register(Request $request)
{
    $validated = $request->validate([
        'token' => 'required|string',
        'name' => 'required|string',
        'device_info' => 'required|array'
    ]);
    
    $display = Display::updateOrCreate(
        ['token' => $validated['token']],
        [
            'name' => $validated['name'],
            'device_info' => $validated['device_info']
        ]
    );
    
    return response()->json([
        'message' => 'Display registered successfully',
        'display' => $display
    ]);
}
```

#### **2. Android App Integration**

**File:** `DeviceApi.kt`

```kotlin
class DeviceApi @Inject constructor(
    private val client: HttpClient
) {
    suspend fun registerDevice(
        baseUrl: String,
        request: RegisterDeviceRequest
    ): RegisterDeviceResponse? {
        return try {
            client.post("$baseUrl/api/displays/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<RegisterDeviceResponse>()
        } catch (e: Exception) {
            Log.w("DeviceApi", "Failed to register: ${e.message}")
            null // Backend sudah ada auto-register fallback
        }
    }
}
```

**File:** `SettingsViewModel.kt`

```kotlin
private fun onSubmit() = viewModelScope.launch {
    // 1. Save to SharedPreferences
    preferences.edit().apply {
        putString(AppConstant.TOKEN, _state.value.token)
        // ... other settings
        apply()
    }

    // 2. Register device to backend (non-blocking, optional)
    registerDeviceToBackend(_state.value.token)

    // 3. Continue with normal flow
    scheduleAlarms()
    _state.update { it.copy(isSuccess = true) }
}

private fun registerDeviceToBackend(token: String) = viewModelScope.launch {
    val request = RegisterDeviceRequest(
        token = token,
        name = "${Build.MANUFACTURER} ${Build.MODEL}",
        deviceInfo = DeviceMetadata(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            appVersion = BuildConfig.VERSION_NAME
        )
    )
    
    deviceApi.registerDevice(BuildConfig.WEBVIEW_BASEURL, request)
}
```

**Payload yang dikirim:**

```json
POST https://kiosk.mugshot.dev/api/displays/register
{
  "token": "DISPLAY-042",
  "name": "Samsung Galaxy Tab A8",
  "device_info": {
    "manufacturer": "Samsung",
    "model": "SM-X200",
    "android_version": "13",
    "app_version": "1.0"
  }
}
```

**Response dari backend:**

```json
{
  "message": "Display registered successfully",
  "display": {
    "id": 15,
    "token": "DISPLAY-042",
    "name": "Samsung Galaxy Tab A8",
    "device_info": {
      "manufacturer": "Samsung",
      "model": "SM-X200",
      "android_version": "13",
      "app_version": "1.0"
    },
    "created_at": "2026-01-29T10:30:00Z",
    "updated_at": "2026-01-29T10:30:00Z"
  }
}
```

### 🔄 Flow Lengkap:

1. **User di App:**
   - Buka Settings
   - Token auto-generate (misal: `DISPLAY-042`) atau edit manual
   - Klik Submit

2. **App melakukan:**
   - Save token ke SharedPreferences ✅
   - Call API `POST /api/displays/register` dengan device info ✅
   - Continue ke HomeView ✅

3. **Backend menerima:**
   - Request API register → Buat/update display di database ✅
   - WebView akses `/display/DISPLAY-042` → Auto-register jika belum ada ✅
   - Return konten atau halaman "unassigned" ✅

### ✅ Keuntungan Implementasi Ini:

1. **No 404 Errors**: Backend auto-register saat pertama akses
2. **Rich Device Info**: Backend dapat info lengkap dari API call
3. **Non-Blocking**: App tidak error jika API gagal
4. **Admin Friendly**: Admin bisa manage devices di CMS
5. **Flexible**: Token bisa dari app (auto-generate) atau dari CMS

---

## 🚀 Quick Fix (Sekarang)

**Di Backend Laravel, edit file:**

```php
// app/Http/Controllers/DisplayController.php atau routes/web.php

Route::get('/display/{token}', function ($token) {
    $device = \App\Models\Device::firstOrCreate(
        ['token' => $token],
        ['name' => 'Device ' . $token]
    );
    
    $content = \App\Models\Content::where('device_id', $device->id)
                                   ->orWhere('is_default', true)
                                   ->first();
    
    if (!$content) {
        return view('kiosk.welcome', ['token' => $token, 'device' => $device]);
    }
    
    return view('kiosk.display', compact('device', 'content'));
});
```

**Buat view default:**

```blade
{{-- resources/views/kiosk/welcome.blade.php --}}
<!DOCTYPE html>
<html>
<head>
    <title>Cosmic Kiosk</title>
</head>
<body style="text-align: center; padding: 100px; font-family: Arial;">
    <h1>🎉 Device Terdaftar!</h1>
    <p>Token: <strong>{{ $token }}</strong></p>
    <p>Silakan setup content di CMS untuk device ini</p>
    <hr>
    <small>Device Name: {{ $device->name }}</small>
</body>
</html>
```

**✅ Selesai!** Sekarang token apapun yang di-generate app akan otomatis terdaftar di backend.

---

## 📝 Catatan

- Token tetap harus unik per device
- Admin bisa lihat semua registered devices di CMS
- Admin bisa assign content spesifik per device
- Default content akan tampil untuk device yang belum ada content-nya

