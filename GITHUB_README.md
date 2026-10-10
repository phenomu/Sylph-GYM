<div align="center">

# 🏋️ Sylph GYM

**Android gym tracker with AI chat, calorie scan, workout statistics & Google Sign-In**

</div>

## ✨ Features

### 🏠 Home Dashboard
- Personalized greeting with daily fitness tip
- Quick action shortcuts to Gym, Chat, and Profile
- Cyberpunk-inspired dark UI

### 💪 Gym Workout Tracker (100% Offline)
- 7-day workout schedule with exercises, sets, reps, and weight tracking
- Add/edit/delete exercises per workout day
- **Import/Export** workout data as JSON (share via Intent)
- Toggle between Workout and Statistics sub-tabs

### 📊 Statistics
- **Body Weight & BMI** tracking with manual input
- **Cardio Log** — 11 presets (Walking (Incline), Running, Cycling, etc.)
  - Speed (km/h) + Incline (%) fields with auto-note
  - `ExposedDropdownMenuBox` for type selection
- **Weekly Volume** bar chart
- **Monthly Progress** overview
- **GitHub-style Activity Heatmap**
- **Total Energy Burned** summary
- **Workout Streak** counter

### 🤖 AI Chat (Multi-Session)
- OpenAI-compatible API (`ch.ate.my.id`)
- 32k max tokens
- Create/switch/delete chat sessions (persisted in SQLite)
- Markdown rendering with copy response

### 📸 Calorie Scan (AI Vision)
- Camera/gallery image picker → AI calorie estimation
- Uses `kimi-k2.5` model for food recognition
- Returns food name, calories, protein/carbs/fat per serving

### 👤 Profile & API Keys
- User info with avatar, verified badge
- Saldo & multiplier display from backend
- API key management: link, unlink, view
- Secure session with JWT (900s) + refresh token (180 days)

### 🔐 Authentication
- Google Sign-In with **account picker** (always shows picker)
- `CredentialManager` with Activity context
- `autoSelectEnabled(false)` + `filterByAuthorizedAccounts(false)`

## 🛠️ Tech Stack

| Category | Technology |
|-----------|------------|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01), Material3 |
| Architecture | MVVM + Repository pattern |
| Networking | OkHttp + kotlinx.serialization |
| Local Storage | SQLiteOpenHelper (manual, no Room/KSP) |
| Auth | Google Credentials API + JWT |
| Min SDK | 30 (Android 11) |
| Target SDK | 37 |

## 📦 Build & Run

1. Clone the repo
2. Create `local.properties` in project root:
   ```properties
   sdk.dir=YOUR_ANDROID_SDK_PATH
   GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   ```
3. Build:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install the APK on device/emulator

## 🌿 Branch Structure

| Branch | Description |
|--------|-------------|
| `main` | Project scaffolding, Gradle config, theme, manifest |
| `feature/data-layer` | API client, DTOs, SessionManager, repositories |
| `feature/auth` | GoogleSignInHelper, LoginScreen, LoginViewModel |
| `feature/home` | HomeScreen, TipsProvider |
| `feature/gym-tracker` | GymScreen, GymViewModel (workout CRUD) |
| `feature/statistics` | StatsScreen, StatsViewModel (charts, cardio) |
| `feature/ai-chat` | ChatScreen, ChatViewModel, ScanScreen (calorie AI) |
| `feature/profile` | ProfileScreen, DashboardScreen, ApiKeysScreen |
| `feature/navigation` | AppNav host, CyberBottomBar |
| `feature/resources` | App icon, Sylph bird logo, launcher assets |

## 📱 Screenshots

> Coming soon

## 📄 License

This project is for personal use.
