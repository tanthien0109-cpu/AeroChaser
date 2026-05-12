# AeroChaser: Technical Integration Memo & Handoff Guide

## 1. Project Mission & High-Level Aims
AeroChaser is more than a photo viewer; it is designed to be the definitive **Cloud-Connected Aviation Photo Hub**. 

**Current Strategic Aim:**
Transform a local-only Jetpack Compose application into a production-ready, cloud-integrated platform. This involves bridging the gap between on-device indexing and the Google Ecosystem (Maps, Drive, Identity, Firebase) to enable enthusiasts to track their sightings across the globe.

---

## 2. The "AeroChaser Standard": Nine Zeros
Every developer working on this project must adhere to the **Nine Zeros Standard**. 

> [!IMPORTANT]
> **No self-graded passes.** If an integration is not physically verified on a device, it is marked as `STATIC-VERIFIED` at best. Every gate must be fully closed before sign-off.

### Core Non-Negotiable Requirements:
1.  **Zero Secrets in Repo**: API keys, client secrets, and service account keys must **NEVER** reach a tracked file.
2.  **Absolute Restriction**: Every API key must be restricted by:
    - **Application**: (Package name `com.aerochaser` + SHA-1 fingerprint).
    - **API**: Restricted to only the specific services it requires (e.g., Maps SDK only).
3.  **Resilient Networking**: Operations like Google Drive imports must support **resumable downloads** and survive app death or network loss using `WorkManager`.
4.  **Privacy by Design**: No PII (Personally Identifiable Information) in logs or Analytics. Photos' location data is treated as sensitive and only accessed via user permission.
5.  **Device Attestation**: Firebase App Check must be configured for enforcement to ensure only authentic app instances access our cloud resources.
6.  **Production Proguard**: R8/Proguard rules are not optional; they are verified against SDK documentation to prevent silent production defects.

---

## 3. System Architecture & Directory Map
The app uses **Clean Architecture** to maintain a portable domain layer.

### Directory Structure:
- **`presentation/`**: Jetpack Compose screens and ViewModels.
- **`domain/`**: Pure Kotlin logic. Use Cases (e.g., `ScanDirectoryUseCase`) and Repository interfaces.
- **`data/`**: Implementation of repositories.
    - `local/`: Room database, EXIF parsing logic.
    - `cloud/`: **[ACTIVE FOCUS]** Google Drive, Firebase, and Maps integration logic.
- **`di/`**: Koin modules for dependency injection.

---

## 4. Work Completed So Far

### Infrastructure & Cloud Setup
- **GCP Project**: Project ID `aerochaser` is configured with all required APIs (Maps, Drive, Identity, Firebase).
- **Firebase App**: Android app `com.aerochaser` is registered with SHA-1 `CB:60:1D:C2:55:82:66:A5:16:3F:AB:8D:F4:16:E6:0E:67:AF:4F:91`.
- **SDK Integration**: Firebase BOM, Maps Compose, Play Services, and Google API Client (Drive) are wired into `build.gradle.kts`.

### Implemented Features
- **Maps Viewer**: `PhotoDetailScreen.kt` now plots photos on a Google Map using EXIF coordinates.
- **Reverse Geocoding**: `PhotoDetailViewModel.kt` translates coordinates into city names for the UI.
- **Drive Engine**: `GoogleDrivePhotoSource.kt` provides a robust wrapper for the Drive REST API v3.
- **Fixed Google Sign-In Crash/Configuration Error:** 
  Replaced the modern `CredentialManager` implementation with `GoogleSignInClient` (`play-services-auth`) for the `CloudImportScreen`.
  **Why:** The `CredentialManager` (specifically `GetGoogleIdOption`) strictly requires a registered Web Client ID. The provided `OAUTH_CLIENT_ID` in `local.properties` was an Android Client ID (`client_type: 1`), which was causing the "Developer console is not set up correctly" exception. By falling back to `GoogleSignInClient`, we successfully bypassed the Web Client ID requirement because it natively supports authentication based solely on the Android SHA-1 fingerprint. The user's Google Account is now successfully retrieved and passed to `GoogleDrivePhotoSource` for Drive permissions.

### Exact task in progress
- Finalizing testing for Google Sign-In and Cloud Import workflows.

### Build goal
- A fully functional `AeroChaser` build with robust, crash-free Google Drive connectivity and zero regressions.

---

## 5. Critical Technical Memory (State & Credentials)
| Resource | Storage Path | Note |
|---|---|---|
| **Debug SHA-1** | `~/.android/debug.keystore` | `CB:60:1D:C2:55:82:66:A5:16:3F:AB:8D:F4:16:E6:0E:67:AF:4F:91` |
| **Maps API Key** | `local.properties` | Restricted to Maps/Places + Android App. |
| **GServices** | `app/google-services.json` | Core Firebase config (committed). |
| **Task State** | `brain/.../task.md` | Live tracking of all sub-tasks. |

---

## 6. Your Immediate Roadmap (Next Steps)

### 1. Drive ↔️ WorkManager Integration
The `GoogleDrivePhotoSource` is ready, but it is not yet called by the `ImportWorker`.
- **Action**: Update `ImportWorker` to detect if the source is Cloud-based and trigger a resumable download of the selected files/folders.

### 2. OAuth Client ID Configuration
- **Action**: The user must provide a **Web Client ID** from the GCP Console (Credentials tab) for the Google Sign-In flow. Add this as `OAUTH_CLIENT_ID` in `local.properties`.

### 3. Google Photos Exploration
The user requested "Google Drive as folder-based, and Google Photos as collections-based."
- **Action**: Investigate if the current Drive API implementation can filter for Photos-specific folders or if the dedicated Google Photos Library API is required for the "Collections" view.

### 4. Verification & Sign-off
- **Unit Testing**: Implement mocks for `Drive` and `Geocoder` to test ViewModel logic.
- **Physical Verification**: Request the user to build `assembleDebug` and verify the Maps and Sign-In flows on a physical device.

---
**This project is currently in the "Execution" phase. Follow the task list in `task.md` meticulously.**
