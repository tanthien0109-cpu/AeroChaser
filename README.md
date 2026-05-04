# AeroChaser ✈️

An aviation photo management application for Android, designed to organize plane photos using EXIF metadata.

## Features

### Working (v1.0.0)
- **EXIF-Based Timeline**: Automatically sorts photos by capture date, camera, and lens data extracted from EXIF metadata
- **Local Import**: Import photo folders via Android's Storage Access Framework with background processing (WorkManager)
- **Immersive Viewer**: Full-screen photo detail view with pinch-to-zoom (up to 5x) and animated EXIF overlay
- **Bottom Navigation**: Three-tab navigation between Timeline, Import, and Cloud screens
- **Material 3 Dynamic Colors**: Adaptive theming on Android 12+ devices

### Coming Soon
- **Cloud Import**: Google Drive & Google Photos integration (interface defined, UI stubbed)
- **Map View**: Photo capture location on Google Maps (GPS coordinates displayed when available)
- **AI Tagging**: ML Kit aircraft and airline recognition

## Architecture

```
Domain Layer (interfaces — "seeds" for porting)
  ├── FileIO, ExifParser, PhotoRepository, CloudPhotoSource
  └── GetPhotosUseCase, ScanDirectoryUseCase

Data Layer (Android implementations — PLATFORM-SPECIFIC tagged)
  ├── AndroidFileIO (SAF), AndroidExifParser (ExifInterface)
  ├── Room Database (PhotoEntity, ExifDataEntity)
  └── ImportWorker (WorkManager)

Presentation Layer (Jetpack Compose)
  ├── TimelineScreen, ImportScreen, CloudImportScreen
  ├── PhotoDetailScreen (gestures, EXIF overlay)
  └── AppNavGraph (Compose Navigation with bottom bar)
```

Platform-specific code is explicitly tagged with `// PLATFORM-SPECIFIC:` comments for teams porting to iOS (Swift) or Windows (C#).

## Building

### Prerequisites

- **Android Studio Hedgehog (2023.1.1) or later** — the IDE will generate the Gradle wrapper automatically
- JDK 17 (bundled with Android Studio)

### Steps

1. Clone the repository
2. Open the project folder in Android Studio
3. Android Studio will prompt to generate the Gradle wrapper — click **OK**
4. Wait for Gradle sync to complete (dependencies will download automatically)
5. Run on a device or emulator (API 26+, Android 8.0 Oreo minimum)

> **Note**: The Gradle wrapper JAR is intentionally not committed to the repository (it's a binary). Android Studio generates it on first open. The `gradle/wrapper/gradle-wrapper.properties` file specifies Gradle 8.5.

## Testing

Unit tests are located in `app/src/test/`. They test domain use cases using fakes (no Android dependencies required):

```
./gradlew test
```

## Version History

- **v1.0.0**: Initial release — local import, EXIF parsing, timeline, photo viewer, navigation

## License

Copyright 2026. All rights reserved.
