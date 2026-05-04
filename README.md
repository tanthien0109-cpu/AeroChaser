# AeroChaser ✈️

A clean, fast Android app for aviation enthusiasts to organize and view plane photos using EXIF data.

## What's Working (v1.0.0)

We just hit our 1.0 release candidate! Here's what's packed inside:

*   **Smart Timeline:** The app reads your photos' EXIF metadata (capture date, camera, lens) and automatically builds a chronological timeline.
*   **Local Imports:** Grab whole folders of photos straight from your phone's storage. It runs smoothly in the background so you can keep using the app.
*   **Immersive Viewer:** Tap a photo to go full screen. You can pinch to zoom (up to 5x) to check the details, and tap to toggle a sleek overlay showing the camera specs.
*   **Modern Android UI:** Built completely with Jetpack Compose. It features a bottom navigation bar and fully supports Material 3 dynamic colors (so it matches your system theme on Android 12+).

## What's Next

*   **Cloud Sync:** Hooking up Google Drive and Google Photos so you aren't limited to local storage. (The UI is stubbed out, just waiting on the API integration).
*   **Map View:** We're going to plot your shots on a map using the GPS coordinates saved in the EXIF data.
*   **AI Spotting:** Integrating ML Kit to automatically recognize aircraft types and airlines from your photos.

## The Technical Stuff

Under the hood, AeroChaser is built using Clean Architecture with a focus on making it easy to port later.

*   **UI:** Jetpack Compose, Material 3, Navigation Compose
*   **DI:** Koin
*   **Database:** Room (SQLite)
*   **Images:** Coil
*   **Background Tasks:** WorkManager

**Note for Porters:** If you're looking to bring this to iOS or Windows, check out the `Domain Layer`. We've set up clear interfaces (like `FileIO` and `ExifParser`). All the Android-specific stuff is kept in the Data layer and clearly tagged with `// PLATFORM-SPECIFIC:`.

## How to Build It

Want to poke around the code or run it yourself?

1.  **Clone it:** `git clone https://github.com/tanthien0109-cpu/AeroChaser.git`
2.  **Open in Android Studio:** You'll need Hedgehog (2023.1.1) or newer.
3.  **The Gradle Wrapper Thing:** You might notice `gradlew` and the wrapper jar aren't in the repo. That's intentional! To keep the repo clean of binaries, we let Android Studio handle it. When you first open the project, Android Studio will prompt you to generate the wrapper. Just hit **OK**.
4.  **Sync & Run:** Let Gradle do its thing, then hit run. It works on any device or emulator running Android 8.0 (API 26) or higher.

## Running Tests

We've got unit tests set up for the core logic (no Android device needed). You can run them from Android Studio or the command line:

```bash
./gradlew test
```

## License

Copyright 2026. All rights reserved.
