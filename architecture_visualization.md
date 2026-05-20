# AeroChaser Architecture & Workflow Visualization

This document provides a visual guide to the application's structure and how data flows through the system.

## 1. High-Level Architecture (Clean Architecture)
AeroChaser follows a strict Layered Architecture to ensure that business logic is independent of the UI and data sources.

```mermaid
graph TD
    subgraph Presentation_Layer["Presentation Layer (Jetpack Compose)"]
        UI[UI Screens / Composables]
        VM[ViewModels (StateFlow)]
    end

    subgraph Domain_Layer["Domain Layer (Pure Kotlin)"]
        UC[Use Cases]
        Model[Domain Models (PhotoMetadata, GearProfile)]
        RepoInt[Repository Interfaces]
    end

    subgraph Data_Layer["Data Layer (Android/Cloud)"]
        RepoImpl[Repository Implementations]
        Room[Room Database (Local Cache)]
        LocalSrc[Local File System (SAF)]
        CloudSrc[Google Drive & Photos API]
    end

    UI --> VM
    VM --> UC
    UC --> RepoInt
    RepoImpl -- implements --> RepoInt
    RepoImpl --> Room
    RepoImpl --> LocalSrc
    RepoImpl --> CloudSrc
```

---

## 2. Photo Import Workflow
This diagram shows how a photo is processed from a directory (Local or Cloud) into the app's timeline.

```mermaid
sequenceDiagram
    participant User
    participant UI as CloudImportScreen / ImportScreen
    participant WM as WorkManager (ImportWorker)
    participant EP as AndroidExifParser (Thread-Safe)
    participant DB as Room Database (PhotoRepository)
    participant Glide as Image Loader (Coil)

    User->>UI: Selects Folder / Album / Drive Folder
    alt Local Folder Import
        UI->>WM: Enqueue Import Task (Folder URI)
        WM->>WM: Iterate Files
        WM->>EP: Parse EXIF (Thread-Safe Date/GPS/Camera)
        EP-->>WM: PhotoMetadata
        WM->>DB: Save metadata if not duplicate
    else Cloud Import (Drive / Photos)
        UI->>DB: Check duplicate via getPhotoIdByUri / photoExistsByUri
        UI->>DB: Save metadata if not duplicate
    end
    DB-->>UI: Update Timeline (Flow)
    UI->>Glide: Load Image for Display
```

---

## 3. Google Services Integration Map
How the Google Services connect to the application components.

```mermaid
graph LR
    subgraph Google_Cloud
        Firebase[Firebase Analytics/Crashlytics/App Check]
        Maps[Google Maps SDK & Geocoder]
        Drive[Google Drive API]
        Photos[Google Photos Library REST API]
        Auth[Google Sign-In / Play Services Auth]
    end

    subgraph App_Components
        App[AeroChaserApp]
        DetailVM[PhotoDetailViewModel]
        DetailUI[PhotoDetailScreen]
        Import[ImportWorker]
        CloudVM[CloudImportViewModel]
    end

    App -->|Init| Firebase
    DetailVM -->|Geocoding| Maps
    DetailUI -->|Render Map| Maps
    Import -->|Fetch| Drive
    Auth -->|Authorize| Drive
    Auth -->|Authorize| Photos
    CloudVM -->|Query| Drive
    CloudVM -->|Query| Photos
```

---

## 4. Component Directory Map
| Component | Package Path | Responsibility |
|---|---|---|
| **Entry Point** | `com.aerochaser.AeroChaserApp` | Firebase Init, Koin Setup |
| **Main Timeline** | `presentation.timeline` | Scrolling Grid of Photos with auto-refresh on display |
| **Detail View** | `presentation.detail` | Zoomable Photo + **Google Maps** dynamically updating camera viewport |
| **Cloud Logic** | `data.cloud` | **GoogleDrivePhotoSource** & **GooglePhotosSource** (volatile auth) |
| **Metadata Engine** | `data.local.exif` | Thread-safe **AndroidExifParser** using ThreadLocal SimpleDateFormat |
| **Sync Engine** | `data.local.worker` | **ImportWorker** (Background Local folder processing) |

---
**Tip**: Use these diagrams to explain the system to new developers during the onboarding phase.
