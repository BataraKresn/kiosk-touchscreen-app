# 🎫 Token Management Guide

## Apa itu Token?

**Token** adalah identifier unik untuk setiap device kiosk. Token ini digunakan untuk:

1. **WebView URL**: `https://kiosk.mugshot.dev/display/{TOKEN}`
   - Setiap device akan menampilkan konten berbeda berdasarkan token-nya
   
2. **WebSocket Refresh**: Menerima perintah refresh dari CMS khusus untuk device ini
   
3. **Remote Control**: Autentikasi device saat remote control dari CMS

## Format Token (Simple & Mudah)

Sekarang token menggunakan format **simple** yang mudah diingat:

```
DISPLAY-001
DISPLAY-042  
DISPLAY-999
```

Format: `DISPLAY-XXX` (3 digit angka)

## Cara Kerja

### 1. Auto-Generate Token

Saat pertama kali buka Settings, aplikasi akan **otomatis generate** token simple:

```kotlin
// Contoh: DISPLAY-123
fun generateSimpleToken(): String {
    val randomNumber = Random.nextInt(1, 1000)
    return "DISPLAY-${randomNumber.toString().padStart(3, '0')}"
}
```

### 2. Generate Token Baru

User bisa klik tombol **"🎲 Generate Token Baru"** untuk dapat token baru.

### 3. Edit Manual

User juga bisa edit token secara manual sesuai kebutuhan.

## Integrasi dengan Backend/CMS

### Di Backend (Laravel/CMS):

1. **Buat Database Table untuk Devices**

```sql
CREATE TABLE devices (
    id BIGINT PRIMARY KEY,
    token VARCHAR(50) UNIQUE,
    name VARCHAR(100),
    location VARCHAR(100),
    created_at TIMESTAMP
);
```

2. **Register Device dengan Token**

```php
// API: POST /api/devices/register
public function register(Request $request) {
    $device = Device::create([
        'token' => $request->token, // Contoh: DISPLAY-001
        'name' => $request->name,
        'location' => $request->location
    ]);
    
    return response()->json($device);
}
```

3. **Display Content Endpoint**

```php
// Route: GET /display/{token}
public function display($token) {
    $device = Device::where('token', $token)->firstOrFail();
    $content = Content::where('device_id', $device->id)->latest()->first();
    
    return view('kiosk.display', [
        'device' => $device,
        'content' => $content
    ]);
}
```

4. **WebSocket Refresh Command**

```php
// Kirim refresh command ke device tertentu
public function sendRefresh($token) {
    broadcast(new RefreshCommand($token))->toOthers();
}
```

```javascript
// WebSocket message format
{
    "action": "refresh",
    "token": "DISPLAY-001"
}
```

## Contoh Use Case

### Scenario 1: Simple Setup

```
Device 1: DISPLAY-001 → Menampilkan konten dari /display/DISPLAY-001
Device 2: DISPLAY-002 → Menampilkan konten dari /display/DISPLAY-002
Device 3: DISPLAY-003 → Menampilkan konten dari /display/DISPLAY-003
```

### Scenario 2: Location-based

```
Device Lobby:    DISPLAY-LOBBY → /display/DISPLAY-LOBBY
Device Room A:   DISPLAY-ROOMA → /display/DISPLAY-ROOMA
Device Entrance: DISPLAY-ENTRANCE → /display/DISPLAY-ENTRANCE
```

User bisa edit token manual untuk sesuaikan dengan lokasi.

### Scenario 3: Custom dari CMS

Di CMS, admin bisa generate token dan kasih QR Code:

1. Admin di CMS: Generate token `KIOSK-VIP-001`
2. Admin cetak QR Code berisi token tersebut
3. User scan QR Code di app (future feature)
4. Token otomatis terisi

## Migration dari Token Panjang

Jika sebelumnya pakai token panjang seperti UUID:

```
OLD: 7a8f3c2e-4b1d-9e5a-8c3f-2d1e4b5a6c7d
NEW: DISPLAY-123
```

**Backend tetap bisa support keduanya:**

```php
// Backend check apakah token lama atau baru
public function display($token) {
    // Cari dulu di database
    $device = Device::where('token', $token)->first();
    
    if (!$device) {
        // Token tidak ditemukan
        abort(404, "Device not found");
    }
    
    // Load konten untuk device ini
    $content = Content::where('device_id', $device->id)->latest()->first();
    
    return view('kiosk.display', [
        'device' => $device,
        'content' => $content
    ]);
}
```

## Tips

1. **Development**: Gunakan token simple seperti `DEV-001`, `DEV-002`
2. **Production**: Gunakan token dengan prefix lokasi `OFFICE1-001`, `STORE2-042`
3. **Testing**: Generate token baru untuk setiap test device
4. **Security**: Token bukan untuk security, hanya untuk identifier. Gunakan API authentication untuk protect endpoint.

## FAQ

### Q: Apakah token harus unik?
**A:** Ya, setiap device harus punya token unik. Backend harus validate uniqueness.

### Q: Bisa pakai token custom tidak?
**A:** Bisa! User bisa edit manual di TextField.

### Q: Token disimpan di mana?
**A:** Di SharedPreferences Android (`AppConstant.TOKEN`).

### Q: Token aman tidak?
**A:** Token bukan untuk security, hanya identifier. Jika perlu security, tambahkan API authentication di backend.

### Q: Bisa pakai UUID atau token panjang?
**A:** Bisa! User tetap bisa ketik/paste token apapun secara manual.

---

**Version:** 1.0  
**Last Updated:** January 29, 2026
