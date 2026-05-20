# AeroChaser ✈️

**The Cloud-Connected Aviation Photo Hub for Android**

AeroChaser turns your plane-spotting photos into a rich, location-aware, AI-enhanced collection. Import from local storage or Google Drive, view every shot on an interactive map, and let Gemini AI tell you about the gear that captured it — all from a single, beautifully crafted Compose interface.

---

## Features

### 📸 Smart Timeline
Automatically reads EXIF metadata (capture date, camera body, lens, GPS coordinates) and builds a reverse-chronological timeline of every sighting.

### 🗺️ Interactive Map Viewer
Each photo's GPS coordinates are plotted on a Google Map with reverse-geocoded city names — so you always know _where_ that 747 was parked. Map viewport updates dynamically as you swipe between photos.

### ☁️ Cloud Import (Drive & Photos)
Sign in with your Google account via Google Sign-In to browse Google Drive folders or Google Photos albums. Background imports are powered by WorkManager for resilience, with built-in **Cross-Provider Duplicate Detection** to ensure the same image isn't imported twice across different services.

### 🤖 AI Gear Overview (Gemini)
Expand the AI panel on any photo to get an on-demand summary of the camera and lens combination used. Powered by the Gemini generative AI SDK, with results cached locally in Room for instant recall.

### 🔍 Immersive Detail Viewer
Full-screen photo viewer with pinch-to-zoom (up to 5×), swipeable HorizontalPager navigation, and a tap-toggle EXIF overlay showing camera specs at a glance.

### 🎨 Material 3 Dynamic Theming
Fully supports Material You dynamic colors on Android 12+, with a polished dark/light mode experience.

---

## Architecture

AeroChaser follows **Clean Architecture** with a strict separation of concerns:

```
com.aerochaser/
├── domain/           ← Pure Kotlin — zero Android imports
│   ├── models/       ← PhotoMetadata, GearProfile, etc.
│   ├── repository/   ← Repository interfaces (PhotoRepository, AiSummaryRepository)
│   ├── usecase/      ← ScanDirectoryUseCase, GetPhotosUseCase, HardwareClassifier
│   ├── exif/         ← ExifParser interface
│   ├── io/           ← FileIO interface
│   └── cloud/        ← CloudPhotoSource interface
│
├── data/             ← Android-specific implementations
│   ├── local/        ← Room DB, DAOs, EXIF parsing, WorkManager workers
│   ├── cloud/        ← Drive (SDK) and Photos (REST) sources
│   ├── ai/           ← GeminiAiSummaryRepository
│   └── repository/   ← Repository implementations
│
├── presentation/     ← Jetpack Compose UI
│   ├── timeline/     ← Main photo grid + timeline
│   ├── detail/       ← Full-screen viewer, map, AI panel
│   ├── cloud/        ← Drive import screen + Google Sign-In
│   ├── importing/    ← Local folder import flow
│   └── navigation/   ← NavHost + bottom nav
│
├── di/               ← Koin dependency injection modules
└── ui/               ← Theme, colors, typography
```

> **Porters:** The `domain/` layer has zero Android dependencies. All platform contracts (`FileIO`, `ExifParser`, `CloudPhotoSource`) are defined as interfaces — swap the `data/` implementations to target iOS, Desktop, or any other Kotlin target.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **UI** | Jetpack Compose · Material 3 · Navigation Compose · Coil |
| **DI** | Koin |
| **Database** | Room (SQLite) |
| **Background** | WorkManager |
| **Cloud** | Google Drive v3 · Google Photos REST API |
| **AI** | Google Gemini Generative AI SDK |
| **Maps** | Google Maps SDK · Maps Compose |
| **Auth** | Google Identity Services · Play Services Auth |
| **Observability** | Firebase Analytics · Crashlytics · Performance Monitoring |
| **Config** | Firebase Remote Config · App Check (Play Integrity) |
| **Build** | Kotlin 1.9 · KSP · Gradle (Kotlin DSL) |

---

## Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17**
- A physical device or emulator running **Android 8.0+ (API 26)**

### 1. Clone

```bash
git clone https://github.com/tanthien0109-cpu/AeroChaser.git
cd AeroChaser
```

### 2. Configure API Keys

Create or edit `local.properties` in the project root:

```properties
# Google Maps — restricted to Maps SDK + Android app
MAPS_API_KEY=your_maps_api_key

# Google OAuth — Web Client ID from GCP Console (Credentials tab)
OAUTH_CLIENT_ID="your_web_client_id"

# Gemini AI — API key from Google AI Studio
GEMINI_API_KEY="your_gemini_api_key"
```

> **Security:** `local.properties` is git-ignored and **never** committed. All keys must be restricted by application (package name + SHA-1) and API scope in the GCP Console.

### 3. Firebase Setup

The project includes a committed `google-services.json` for the `com.aerochaser` package. If you're building under a different package name, replace it with your own from the [Firebase Console](https://console.firebase.google.com/).

### 4. Build & Run

```bash
./gradlew assembleDebug
```

Or simply open the project in Android Studio and hit **Run**.

> **Note about Gradle Wrapper:** If `gradlew` isn't present, Android Studio will prompt you to generate it on first open. Accept the prompt.

---

## Running Tests

```bash
# Unit tests (no device required)
./gradlew test

# Instrumented tests (requires connected device / emulator)
./gradlew connectedAndroidTest
```

---

## Project Configuration

| File | Purpose |
|---|---|
| `local.properties` | API keys (git-ignored) |
| `app/google-services.json` | Firebase configuration |
| `app/build.gradle.kts` | Dependencies, SDK versions, build config |
| `gradle.properties` | JVM args, AndroidX opt-in flags |
| `settings.gradle.kts` | Plugin repositories, project name |

---

## Security Policy

- **Zero secrets in source control.** All API keys live in `local.properties` (git-ignored).
- **Key restriction.** Every GCP key is restricted by Android app (package + SHA-1) and API scope.
- **App Check.** Firebase App Check with Play Integrity is configured for backend resource protection.
- **Privacy.** No PII is logged or sent to Analytics. GPS coordinates are accessed only with explicit user permission.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
