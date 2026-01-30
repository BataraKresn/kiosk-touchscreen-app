# 🔌 Backend API Requirements - Display CMS Kosong

## ❌ Masalah Saat Ini

**Display CMS kosong** karena backend **belum ada endpoint** `/api/displays`

### Test Results:
```bash
❌ GET https://kiosk.mugshot.dev/api/displays
Response: 404 Not Found

✅ GET https://kiosk.mugshot.dev/api/health
Response: 200 OK (Backend hidup)
```

---

## ✅ API Endpoint yang Dibutuhkan

### 1. **GET /api/displays**

**Purpose:** Ambil daftar semua display/kiosk yang terdaftar di CMS

**Request:**
```http
GET /api/displays?search=DISPLAY&per_page=50
Accept: application/json
```

**Response Expected:**
```json
{
  "data": [
    {
      "id": 1,
      "name": "DISPLAY-001"
    },
    {
      "id": 2,
      "name": "DISPLAY-042"
    },
    {
      "id": 3,
      "name": "LOBBY-TV"
    }
  ],
  "meta": {
    "current_page": 1,
    "per_page": 50,
    "total": 3
  }
}
```

**Query Parameters:**
- `search` (optional): Filter display by name
- `per_page` (optional, default: 50): Jumlah items per page

---

### 2. **POST /api/displays/register** (sudah ada?)

**Purpose:** Register device baru ke CMS

**Request:**
```json
{
  "device_id": "xxxxx-xxxxx-xxxxx",
  "device_name": "Samsung Galaxy Tab A",
  "device_info": {
    "manufacturer": "samsung",
    "model": "SM-T510",
    "android_version": "11",
    "ip_address": "192.168.1.100"
  }
}
```

**Response:**
```json
{
  "message": "Device registered successfully",
  "display": {
    "id": 1,
    "token": "DISPLAY-001",
    "name": "DISPLAY-001",
    "device_info": {...},
    "created_at": "2026-01-30T12:00:00Z",
    "updated_at": "2026-01-30T12:00:00Z"
  }
}
```

---

## 📋 Implementation Guide (Laravel)

### Database Migration

```php
// database/migrations/xxxx_create_displays_table.php
Schema::create('displays', function (Blueprint $table) {
    $table->id();
    $table->string('token')->unique(); // DISPLAY-001, DISPLAY-042, etc
    $table->string('name')->nullable(); // Display Name (bisa sama dengan token)
    $table->string('device_id')->unique()->nullable();
    $table->json('device_info')->nullable();
    $table->timestamp('last_seen_at')->nullable();
    $table->boolean('is_active')->default(true);
    $table->timestamps();
});
```

### Routes

```php
// routes/api.php
Route::prefix('displays')->group(function () {
    Route::get('/', [DisplayController::class, 'index']); // List displays
    Route::post('/register', [DisplayController::class, 'register']); // Register device
    Route::get('/{token}', [DisplayController::class, 'show']); // Get single display
});
```

### Controller

```php
// app/Http/Controllers/DisplayController.php
namespace App\Http\Controllers;

use App\Models\Display;
use Illuminate\Http\Request;

class DisplayController extends Controller
{
    /**
     * GET /api/displays
     * List all displays with optional search
     */
    public function index(Request $request)
    {
        $query = Display::query();
        
        // Search by name or token
        if ($request->has('search')) {
            $search = $request->search;
            $query->where(function($q) use ($search) {
                $q->where('name', 'like', "%{$search}%")
                  ->orWhere('token', 'like', "%{$search}%");
            });
        }
        
        $perPage = $request->get('per_page', 50);
        $displays = $query->paginate($perPage);
        
        return response()->json([
            'data' => $displays->map(function($display) {
                return [
                    'id' => $display->id,
                    'name' => $display->name ?? $display->token, // fallback to token if name null
                ];
            }),
            'meta' => [
                'current_page' => $displays->currentPage(),
                'per_page' => $displays->perPage(),
                'total' => $displays->total(),
            ]
        ]);
    }
    
    /**
     * POST /api/displays/register
     * Register new device
     */
    public function register(Request $request)
    {
        $validated = $request->validate([
            'device_id' => 'required|string',
            'device_name' => 'nullable|string',
            'device_info' => 'nullable|array',
        ]);
        
        // Check if device already registered
        $display = Display::where('device_id', $validated['device_id'])->first();
        
        if (!$display) {
            // Generate new token
            $lastDisplay = Display::orderBy('id', 'desc')->first();
            $nextNumber = $lastDisplay ? (intval(substr($lastDisplay->token, -3)) + 1) : 1;
            $token = sprintf('DISPLAY-%03d', $nextNumber);
            
            $display = Display::create([
                'token' => $token,
                'name' => $token, // default name = token
                'device_id' => $validated['device_id'],
                'device_info' => $validated['device_info'] ?? null,
                'last_seen_at' => now(),
            ]);
        } else {
            // Update existing
            $display->update([
                'device_info' => $validated['device_info'] ?? $display->device_info,
                'last_seen_at' => now(),
            ]);
        }
        
        return response()->json([
            'message' => 'Device registered successfully',
            'display' => [
                'id' => $display->id,
                'token' => $display->token,
                'name' => $display->name,
                'device_info' => $display->device_info,
                'created_at' => $display->created_at,
                'updated_at' => $display->updated_at,
            ]
        ]);
    }
    
    /**
     * GET /api/displays/{token}
     * Get single display info
     */
    public function show($token)
    {
        $display = Display::where('token', $token)->firstOrFail();
        
        return response()->json([
            'data' => $display
        ]);
    }
}
```

### Model

```php
// app/Models/Display.php
namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Display extends Model
{
    protected $fillable = [
        'token',
        'name',
        'device_id',
        'device_info',
        'last_seen_at',
        'is_active'
    ];
    
    protected $casts = [
        'device_info' => 'array',
        'last_seen_at' => 'datetime',
        'is_active' => 'boolean',
    ];
}
```

---

## 🧪 Testing

### Create Dummy Data (Seeder)

```php
// database/seeders/DisplaySeeder.php
namespace Database\Seeders;

use App\Models\Display;
use Illuminate\Database\Seeder;

class DisplaySeeder extends Seeder
{
    public function run()
    {
        $displays = [
            ['token' => 'DISPLAY-001', 'name' => 'DISPLAY-001'],
            ['token' => 'DISPLAY-002', 'name' => 'DISPLAY-002'],
            ['token' => 'DISPLAY-042', 'name' => 'DISPLAY-042'],
            ['token' => 'LOBBY-TV', 'name' => 'LOBBY-TV'],
            ['token' => 'ENTRANCE-SCREEN', 'name' => 'ENTRANCE-SCREEN'],
        ];
        
        foreach ($displays as $display) {
            Display::create([
                'token' => $display['token'],
                'name' => $display['name'],
                'is_active' => true,
                'last_seen_at' => now(),
            ]);
        }
    }
}
```

Run seeder:
```bash
php artisan db:seed --class=DisplaySeeder
```

### Test API

```bash
# List all displays
curl -X GET "https://kiosk.mugshot.dev/api/displays" \
  -H "Accept: application/json"

# Search displays
curl -X GET "https://kiosk.mugshot.dev/api/displays?search=DISPLAY" \
  -H "Accept: application/json"

# Register device
curl -X POST "https://kiosk.mugshot.dev/api/displays/register" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "device_id": "test-device-123",
    "device_name": "Test Tablet",
    "device_info": {
      "manufacturer": "samsung",
      "model": "SM-T510"
    }
  }'
```

---

## ✅ Checklist Backend Developer

- [ ] Buat migration `displays` table
- [ ] Buat model `Display`
- [ ] Buat controller `DisplayController`
- [ ] Tambahkan routes di `api.php`
- [ ] Run migration: `php artisan migrate`
- [ ] Seed dummy data: `php artisan db:seed --class=DisplaySeeder`
- [ ] Test endpoint `/api/displays` → harus return data
- [ ] Test endpoint `/api/displays/register` → harus bisa register device
- [ ] Deploy ke production
- [ ] Test dari Android app

---

## 📱 After Backend Ready

Setelah backend ready, test dari Android:
```powershell
# Clear logs
adb logcat -c

# Monitor API calls
adb logcat | findstr "DeviceApi"

# Di app, buka Settings → tap "Ambil Display dari CMS"
# Harus muncul list: DISPLAY-001, DISPLAY-002, etc.
```

---

## 🚀 Expected Result

Setelah backend implement endpoint `/api/displays`, dropdown di Settings app akan terisi:

```
Display Name: [Pilih Display ▼]
  - DISPLAY-001
  - DISPLAY-002
  - DISPLAY-042
  - LOBBY-TV
  - ENTRANCE-SCREEN
```

User tinggal pilih salah satu → Save → WebView akan load `https://kiosk.mugshot.dev/display/DISPLAY-001`
